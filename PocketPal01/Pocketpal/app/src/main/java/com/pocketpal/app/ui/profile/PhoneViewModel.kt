package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.phone.PhoneRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Teléfono (§7.2: "Número, envío y verificación del código SMS", RF-B3 / RN-06).
 *
 * El teléfono **solo queda guardado tras verificarlo**. El código expira a los
 * 10 minutos con temporizador en el cliente (D-15 / RF-A5): si se intenta verificar
 * después, se muestra "expirado" sin llamar al servidor.
 */
class PhoneViewModel(private val repository: PhoneRepository) : ViewModel() {

    enum class Step { NUMBER, CODE }

    data class PhoneUi(
        val step: Step = Step.NUMBER,
        val prefix: String = "+57",
        val number: String = "",
        val code: String = "",
        val sending: Boolean = false,
        val verifying: Boolean = false,
        /** Teléfono ya verificado (E.164) o null (RN-06). */
        val currentPhone: String? = null,
    )

    /** Eventos de una sola vez que la UI consume (§7.3). */
    enum class Event { CodeSent, CodeResent, Verified }

    private val _state = MutableStateFlow(PhoneUi())
    val state: StateFlow<PhoneUi> = _state.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _formError = MutableStateFlow<ErrorType?>(null)
    val formError: StateFlow<ErrorType?> = _formError.asStateFlow()

    /** D-15: expiración real del código (milisegundos) para el contador de la UI. */
    private val _codeExpiresAt = MutableStateFlow(0L)
    val codeExpiresAt: StateFlow<Long> = _codeExpiresAt.asStateFlow()

    private var timerJob: Job? = null

    init {
        _state.value = _state.value.copy(currentPhone = repository.currentPhone())
    }

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _error.value = null }
    fun consumeFormError() { _formError.value = null }

    fun setPrefix(prefix: String) { _state.value = _state.value.copy(prefix = prefix) }
    fun setNumber(number: String) { _state.value = _state.value.copy(number = number) }
    fun setCode(code: String) { _state.value = _state.value.copy(code = code) }

    /** "¿Ese no es tu número?": vuelve al paso 1 (§8.4). */
    fun backToNumber() {
        _state.value = _state.value.copy(step = Step.NUMBER, code = "")
    }

    fun sendCode() {
        val e164 = e164()
        if (!isValidNumber(_state.value.number)) {
            _formError.value = ErrorType.InvalidPhone
            return
        }
        _state.value = _state.value.copy(sending = true)
        viewModelScope.launch {
            try {
                repository.sendVerificationCode(e164)
                startTimer()
                _state.value = _state.value.copy(step = Step.CODE, sending = false, code = "")
                _event.value = Event.CodeSent
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(sending = false)
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                // Sin proveedor SMS o número no aceptado (D-02 / RF-B3).
                _state.value = _state.value.copy(sending = false)
                _error.value = ErrorType.SmsUnavailable
            }
        }
    }

    fun resend() {
        _state.value = _state.value.copy(sending = true)
        viewModelScope.launch {
            try {
                repository.sendVerificationCode(e164())
                startTimer()
                _state.value = _state.value.copy(sending = false, code = "")
                _event.value = Event.CodeResent
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(sending = false)
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _state.value = _state.value.copy(sending = false)
                _error.value = ErrorType.SmsUnavailable
            }
        }
    }

    fun verifyCode() {
        val code = _state.value.code
        if (code.length != CODE_LENGTH) {
            _formError.value = ErrorType.OtpIncomplete
            return
        }
        // D-15: el temporizador del cliente distingue "expirado" (Supabase no lo distingue).
        if (System.currentTimeMillis() > _codeExpiresAt.value) {
            _formError.value = ErrorType.ExpiredOtp
            return
        }
        _state.value = _state.value.copy(verifying = true)
        viewModelScope.launch {
            try {
                repository.verifyCode(e164(), code)
                _state.value = _state.value.copy(
                    verifying = false,
                    currentPhone = e164(),
                    step = Step.NUMBER,
                    number = "",
                    code = "",
                )
                _event.value = Event.Verified
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(verifying = false)
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _state.value = _state.value.copy(verifying = false)
                _formError.value = ErrorType.InvalidOtp
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _codeExpiresAt.value = System.currentTimeMillis() + CODE_TTL_MS
        timerJob = viewModelScope.launch {
            delay(CODE_TTL_MS)
        }
    }

    private fun e164(): String = _state.value.prefix + _state.value.number.filter { it.isDigit() }

    /** RN-06: número nacional razonable (7 a 12 dígitos) antes de pedir el SMS. */
    private fun isValidNumber(number: String): Boolean {
        val digits = number.filter { it.isDigit() }
        return digits.length in MIN_NUMBER_DIGITS..MAX_NUMBER_DIGITS
    }

    companion object {
        const val CODE_LENGTH = 6
        const val MIN_NUMBER_DIGITS = 7
        const val MAX_NUMBER_DIGITS = 12

        /** D-15: 10 minutos reales (coincide con `otp_expiry = 600` en Supabase). */
        const val CODE_TTL_MS = 10 * 60 * 1000L
    }
}
