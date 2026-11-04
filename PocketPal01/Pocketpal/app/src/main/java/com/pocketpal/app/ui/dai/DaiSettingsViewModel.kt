package com.pocketpal.app.ui.dai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiRepository
import com.pocketpal.app.data.dai.DaiSettings
import com.pocketpal.app.data.dai.DaiSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Flujo 4 · "Configuración de Dai" (Figma "11_Configuraciones") y "Borrar historial"
 * (Figma "12_Borrar_Historial").
 *
 * - La voz de Dai se guarda en el teléfono con DataStore ([DaiSettingsRepository]).
 * - Borrar historial borra la conversación en Supabase (`dai_messages`) y nada más.
 */
class DaiSettingsViewModel(
    private val repository: DaiRepository,
    private val settingsRepository: DaiSettingsRepository,
) : ViewModel() {

    /**
     * Las preferencias tal como las entrega DataStore (un `Flow`: cada cambio llega solo).
     * La pantalla lo observa con `collectAsStateWithLifecycle(initialValue = DaiSettings())`.
     */
    val settings: Flow<DaiSettings> = settingsRepository.settings

    sealed interface Event {
        data object HistoryCleared : Event
    }

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _error.value = null }

    /** Interruptor "Voz": Dai lee en voz alta sus respuestas. */
    fun setVoiceReplies(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVoiceReplies(enabled) }
    }

    /** "Eliminar historial". */
    fun clearHistory() {
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.clearHistory()
                _event.value = Event.HistoryCleared
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _deleting.value = false
            }
        }
    }
}
