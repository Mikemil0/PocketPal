package com.pocketpal.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Preferencias visuales locales (RNF-06). Los valores son **códigos neutros** (RN-11):
 * la UI los traduce.
 *
 * @property language `""` = Automático (idioma del sistema); si no, `es`/`en`/`pt`/`fr`.
 * @property theme `light` | `dark` | `system`.
 * @property weekStart 0–6, con 0 = domingo.
 * @property rememberMe D-13: "Recordarme" del login. `false` = la sesión no se
 * restaura al abrir la app de nuevo (se limpia en el splash).
 */
data class UserPreferences(
    val language: String = "",
    val theme: String = "system",
    val currency: String = "COP",
    val dateFormat: String = "DD/MM/YYYY",
    val weekStart: Int = 0,
    val rememberMe: Boolean = false,
)

private val Context.pocketpalDataStore by preferencesDataStore(name = "pocketpal_settings")

/**
 * Acceso a DataStore, la fuente local de preferencias (RNF-06: se aplican al instante
 * al abrir la app). Lo usa [SettingsRepository], que es la interfaz de las ViewModels;
 * la sincronización con la cuenta (`public.profiles`) llega con las preferencias (Fase 5).
 */
class UserPreferencesStore(private val context: Context) {

    val preferences: Flow<UserPreferences> = context.pocketpalDataStore.data.map { p ->
        UserPreferences(
            language = p[KEY_LANGUAGE] ?: "",
            theme = p[KEY_THEME] ?: "system",
            currency = p[KEY_CURRENCY] ?: "COP",
            dateFormat = p[KEY_DATE_FORMAT] ?: "DD/MM/YYYY",
            weekStart = p[KEY_WEEK_START] ?: 0,
            rememberMe = p[KEY_REMEMBER_ME] ?: false,
        )
    }

    /** Lectura puntual (sin flujo) para decidir el arranque en el splash. */
    suspend fun rememberMe(): Boolean = preferences.first().rememberMe

    suspend fun setLanguage(tag: String) {
        context.pocketpalDataStore.edit { it[KEY_LANGUAGE] = tag }
    }

    suspend fun setTheme(theme: String) {
        context.pocketpalDataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setCurrency(currency: String) {
        context.pocketpalDataStore.edit { it[KEY_CURRENCY] = currency }
    }

    suspend fun setDateFormat(format: String) {
        context.pocketpalDataStore.edit { it[KEY_DATE_FORMAT] = format }
    }

    suspend fun setWeekStart(day: Int) {
        context.pocketpalDataStore.edit { it[KEY_WEEK_START] = day }
    }

    suspend fun setRememberMe(remember: Boolean) {
        context.pocketpalDataStore.edit { it[KEY_REMEMBER_ME] = remember }
    }

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_DATE_FORMAT = stringPreferencesKey("date_format")
        val KEY_WEEK_START = intPreferencesKey("week_start")
        val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    }
}
