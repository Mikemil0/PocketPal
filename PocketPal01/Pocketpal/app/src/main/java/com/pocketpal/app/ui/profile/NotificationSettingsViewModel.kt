package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.notification.NotificationSettings
import com.pocketpal.app.data.notification.NotificationSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Preferencias de notificación (§7.2, RF-B6). Los cambios se **guardan automáticamente**
 * en `notification_settings` (§8.7) con actualización optimista: si la red falla, se
 * revierte y la UI muestra el error (RNF-05). Solo preferencias: el envío real de
 * recordatorios no es de este módulo.
 */
class NotificationSettingsViewModel(
    private val repository: NotificationSettingsRepository,
) : ViewModel() {

    /** §7.3: `UiState` con Loading, Content(data) y Error(type). */
    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: NotificationSettings) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    init { refresh() }

    fun consumeError() { _error.value = null }

    fun refresh() {
        viewModelScope.launch {
            _state.value = try {
                UiState.Content(repository.get())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    fun toggleDailyReminder() = update { it.copy(dailyReminder = !it.dailyReminder) }
    fun toggleWeeklySummary() = update { it.copy(weeklySummary = !it.weeklySummary) }
    fun toggleBudgetAlert() = update { it.copy(budgetAlert = !it.budgetAlert) }
    fun toggleGoalsProgress() = update { it.copy(goalsProgress = !it.goalsProgress) }
    fun toggleChannelEmail() = update { it.copy(channelEmail = !it.channelEmail) }

    /** RN-07: el chip SMS solo con teléfono verificado; por ahora no se puede activar. */
    fun toggleChannelSms() = update { it.copy(channelSms = !it.channelSms) }

    /** RN-08: entero entre 50 y 100 (por defecto 80). */
    fun setBudgetThreshold(value: Int) =
        update { it.copy(budgetThreshold = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)) }

    /** Actualización optimista: la UI cambia al instante y se revierte si falla. */
    private fun update(transform: (NotificationSettings) -> NotificationSettings) {
        val current = (_state.value as? UiState.Content)?.data ?: return
        val next = transform(current)
        _state.value = UiState.Content(next)
        viewModelScope.launch {
            try {
                repository.save(next)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = UiState.Content(current)
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _state.value = UiState.Content(current)
                _error.value = ErrorType.Unknown
            }
        }
    }

    companion object {
        const val MIN_THRESHOLD = 50
        const val MAX_THRESHOLD = 100
    }
}
