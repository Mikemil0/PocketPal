package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.auth.PasswordRules
import com.pocketpal.app.data.device.DeviceRepository
import com.pocketpal.app.data.device.DeviceSession
import com.pocketpal.app.data.security.SecurityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Seguridad (§7.2: "Cambio de contraseña y sesiones activas", RF-B4 y RF-B5).
 *
 * - **Cambiar contraseña** (§8.6): valida las reglas (RN-04), reautentica con la
 *   contraseña actual, cambia la contraseña, guarda `password_changed_at` y cierra
 *   las demás sesiones (D-12, `SignOutScope.OTHERS`).
 * - **Sesiones activas** (D-01 opción B): lista de `user_devices` y revocación de
 *   un dispositivo concreto (Edge Function + fallback A).
 */
class SecurityViewModel(
    private val securityRepository: SecurityRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    data class DeviceUi(
        val id: String,
        val deviceName: String,
        val platform: String,
        val lastActiveAt: String,
        val isCurrent: Boolean,
    )

    data class SecurityUi(
        /** RN-05: false para cuentas solo de Google (sin contraseña que cambiar). */
        val hasPassword: Boolean = true,
        /** Instante ISO de `password_changed_at` o null si nunca (§8.5). */
        val passwordUpdatedAt: String? = null,
        val devices: List<DeviceUi> = emptyList(),
    )

    /** §7.3: `UiState` con Loading, Content(data) y Error(type). */
    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: SecurityUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** Evento de una sola vez que la UI consume (§7.3). */
    enum class Event { PasswordChanged }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    /** Errores del formulario (campo actual / reglas) sin tocar la red. */
    private val _formError = MutableStateFlow<ErrorType?>(null)
    val formError: StateFlow<ErrorType?> = _formError.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    init { refresh() }

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _error.value = null }
    fun consumeFormError() { _formError.value = null }

    fun refresh() {
        viewModelScope.launch {
            _state.value = try {
                val current = deviceRepository.currentSessionId()
                UiState.Content(
                    SecurityUi(
                        hasPassword = securityRepository.hasPassword(),
                        passwordUpdatedAt = securityRepository.passwordChangedAt(),
                        devices = deviceRepository.list().map {
                            DeviceUi(
                                id = it.id,
                                deviceName = it.deviceName,
                                platform = it.platform,
                                lastActiveAt = it.lastActiveAt,
                                isCurrent = it.sessionId != null && it.sessionId == current,
                            )
                        },
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

    /**
     * RN-04: aplica las 3 reglas del Módulo 1 y, además, la nueva debe ser distinta
     * de la actual. Luego el flujo de §8.6: reautenticar, cambiar, marcar la fecha y
     * cerrar las demás sesiones (D-12).
     */
    fun changePassword(current: String, new: String, confirm: String) {
        if (current.isEmpty()) return formFail(ErrorType.EmptyPassword)
        val rules = PasswordRules.of(new, confirm)
        when {
            !rules.minLength -> return formFail(ErrorType.PasswordTooShort)
            !rules.upperAndDigit -> return formFail(ErrorType.PasswordNeedsUpperAndDigit)
            !rules.matches -> return formFail(ErrorType.PasswordMismatch)
            new == current -> return formFail(ErrorType.NewPasswordSameAsCurrent)
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val email = securityRepository.currentEmail().orEmpty()
                if (!securityRepository.reauthenticate(email, current)) {
                    _formError.value = ErrorType.CurrentPasswordIncorrect
                } else {
                    securityRepository.changePassword(new)
                    securityRepository.markPasswordChanged()
                    // D-12: se mantiene la sesión actual y se cierran las demás.
                    deviceRepository.revokeAllOthers()
                    _event.value = Event.PasswordChanged
                }
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

    /** D-01: revoca la sesión de un dispositivo concreto (fallback A dentro). */
    fun revokeDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                deviceRepository.revoke(deviceId)
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            }
        }
    }

    /** §8.5: registra o actualiza este dispositivo al entrar a la app. Mejor esfuerzo. */
    fun registerCurrentDevice() {
        viewModelScope.launch {
            runCatching { deviceRepository.registerCurrentDevice() }
        }
    }

    private fun formFail(type: ErrorType) {
        _formError.value = type
    }
}
