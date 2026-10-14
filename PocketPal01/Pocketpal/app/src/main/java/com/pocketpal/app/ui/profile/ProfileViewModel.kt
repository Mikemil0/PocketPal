package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.profile.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Perfil del usuario (§7.2). **Compartido** entre el Home y las pantallas del perfil:
 * una sola instancia para que el saludo del Home y "Mi perfil" muestren lo mismo
 * (TC-08) sin recargar.
 *
 * RNF-02: depende de [ProfileRepository] (interfaz), no de Supabase.
 * RNF-05: las operaciones de red no cierran la app; los errores se emiten como
 * [ErrorType] y la UI los traduce (RF-A3).
 */
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    /** Datos que la UI necesita (ya con la URL de la foto resuelta). */
    data class ProfileUi(
        val fullName: String = "",
        val email: String = "",
        val avatarUrl: String? = null,
        val country: String = "CO",
        val currency: String = "COP",
        val registeredExpenses: Int = 0,
        val activeGoals: Int = 0,
    )

    /** §7.3: `UiState` con Loading, Content(data) y Error(type). */
    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: ProfileUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** Evento de una sola vez que la UI consume (igual que `consume()` del Módulo 1). */
    enum class Event { DataSaved }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** §7.3: bandera de acción puntual mientras se guarda. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    init { refresh() }

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _error.value = null }

    /**
     * Recarga el perfil. Si ya hay contenido no vuelve a Loading (evita parpadeos
     * al entrar y salir de las pantallas del perfil).
     */
    fun refresh() {
        viewModelScope.launch {
            if (_state.value !is UiState.Content) _state.value = UiState.Loading
            _state.value = try {
                val profile = repository.getMyProfile()
                val stats = repository.getStats()
                UiState.Content(
                    ProfileUi(
                        fullName = profile?.fullName.orEmpty(),
                        email = repository.currentEmail().orEmpty(),
                        avatarUrl = repository.avatarUrl(profile?.avatarPath),
                        country = profile?.country ?: "CO",
                        currency = profile?.currency ?: "COP",
                        registeredExpenses = stats.registeredExpenses,
                        activeGoals = stats.activeGoals,
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    /** RN-01: nombre obligatorio y de máximo 60 caracteres. */
    fun savePersonalData(fullName: String, country: String, currency: String) {
        val name = fullName.trim()
        if (name.isEmpty()) {
            _error.value = ErrorType.InvalidName
            return
        }
        if (name.length > MAX_NAME_LENGTH) {
            _error.value = ErrorType.NameTooLong
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val updated = repository.savePersonalData(name, country, currency)
                updateContent {
                    it.copy(
                        fullName = updated?.fullName ?: name,
                        country = updated?.country ?: country,
                        currency = updated?.currency ?: currency,
                    )
                }
                _event.value = Event.DataSaved
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Sube la foto ya comprimida (RNF-04) y actualiza el estado compartido.
     * `null` = la imagen no se pudo leer/codificar.
     */
    fun setAvatar(imageBytes: ByteArray?) {
        if (imageBytes == null) {
            _error.value = ErrorType.Unknown
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val updated = repository.updateAvatar(imageBytes)
                updateContent { it.copy(avatarUrl = repository.avatarUrl(updated?.avatarPath)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    fun removeAvatar() {
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.removeAvatar()
                updateContent { it.copy(avatarUrl = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    private fun updateContent(transform: (ProfileUi) -> ProfileUi) {
        val current = _state.value as? UiState.Content ?: return
        _state.value = UiState.Content(transform(current.data))
    }

    companion object {
        /** RN-01 */
        const val MAX_NAME_LENGTH = 60
    }
}
