package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.profile.ProfileRepository
import com.pocketpal.app.data.settings.SettingsRepository
import com.pocketpal.app.ui.AppLocale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Preferencias generales (§7.2, RF-B8): idioma, moneda, formato de fecha, tema y
 * inicio de semana.
 *
 * RNF-06: cada cambio se aplica **al instante** (DataStore) y se sincroniza con la
 * cuenta (`profiles`). Al abrir la app **manda la cuenta** (decisión 3-A de la Fase 5),
 * con una excepción: el idioma **"Automático"** (`""`) es una preferencia del
 * dispositivo — sigue el idioma de ESTE equipo — y nunca lo arrastra la cuenta
 * (el CHECK de `profiles.language` solo admite `es`/`en`/`pt`/`fr`).
 */
class PreferencesViewModel(
    private val settings: SettingsRepository,
    private val profileRepository: ProfileRepository,
    /** Inyectable para poder probar la ViewModel sin Android (RNF-02). */
    private val applyLanguage: (String?) -> Unit = { AppLocale.setLanguage(it) },
) : ViewModel() {

    /** Códigos neutros (RN-11): la UI los traduce. [language] vacío = Automático. */
    data class UiState(
        val language: String = "",
        val theme: String = "system",
        val currency: String = "COP",
        val dateFormat: String = "DD/MM/YYYY",
        val weekStart: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val local = settings.settings.first()
            _state.value = UiState(
                language = local.language,
                theme = local.theme,
                currency = local.currency,
                dateFormat = local.dateFormat,
                weekStart = local.weekStart,
            )
            syncFromAccount()
        }
    }

    fun consumeError() { _error.value = null }

    /** "" = Automático (sigue el idioma del sistema de este dispositivo). */
    fun setLanguage(tag: String) = apply(language = tag)
    fun setTheme(theme: String) = apply(theme = theme)
    fun setCurrency(currency: String) = apply(currency = currency)
    fun setDateFormat(format: String) = apply(dateFormat = format)
    fun setWeekStart(day: Int) = apply(weekStart = day)

    /**
     * Cuenta manda (RNF-06): `profiles` actualiza el dispositivo al abrir la app.
     * Mejor esfuerzo: si falla la red no se avisa (la preferencia local ya aplica y
     * el error se muestra al guardar desde la pantalla, RNF-05).
     */
    private suspend fun syncFromAccount() {
        val profile = try {
            profileRepository.getMyProfile()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return
        val s = _state.value
        // El idioma solo se arrastra si el dispositivo lo eligió explícito.
        val language = if (s.language.isBlank()) s.language else profile.language
        val next = UiState(
            language = language,
            theme = profile.theme.ifBlank { s.theme },
            currency = profile.currency.ifBlank { s.currency },
            dateFormat = profile.dateFormat.ifBlank { s.dateFormat },
            weekStart = profile.weekStart,
        )
        if (next == s) return
        _state.value = next
        settings.setLanguage(next.language)
        settings.setTheme(next.theme)
        settings.setCurrency(next.currency)
        settings.setDateFormat(next.dateFormat)
        settings.setWeekStart(next.weekStart)
        if (s.language.isNotBlank() && next.language != s.language) {
            applyLanguage(next.language.ifBlank { null })
        }
    }

    /** Aplica al instante (DataStore) y sincroniza con la cuenta (mejor esfuerzo). */
    private fun apply(
        language: String? = null,
        theme: String? = null,
        currency: String? = null,
        dateFormat: String? = null,
        weekStart: Int? = null,
    ) {
        val next = _state.value.copy(
            language = language ?: _state.value.language,
            theme = theme ?: _state.value.theme,
            currency = currency ?: _state.value.currency,
            dateFormat = dateFormat ?: _state.value.dateFormat,
            weekStart = weekStart ?: _state.value.weekStart,
        )
        _state.value = next
        viewModelScope.launch {
            // Local primero: se aplica al instante (RNF-06).
            language?.let {
                settings.setLanguage(it)
                applyLanguage(it.ifBlank { null })
            }
            theme?.let { settings.setTheme(it) }
            currency?.let { settings.setCurrency(it) }
            dateFormat?.let { settings.setDateFormat(it) }
            weekStart?.let { settings.setWeekStart(it) }
            // Cuenta después (mejor esfuerzo).
            try {
                profileRepository.savePreferences(
                    language = next.language,
                    theme = next.theme,
                    currency = next.currency,
                    dateFormat = next.dateFormat,
                    weekStart = next.weekStart,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            }
        }
    }
}
