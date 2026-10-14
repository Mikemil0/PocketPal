package com.pocketpal.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.data.settings.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.SignOutScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

private class EmailTakenException : Exception("already registered")

class AuthViewModel(
    private val supabase: SupabaseClient,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * D-13: casilla "Recordarme" persistida en DataStore. Se precarga en el login
     * con el último valor elegido y se guarda al iniciar sesión.
     */
    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    /**
     * Momento en que vence el código OTP actual (RF-A5 / D-15). Supabase responde igual
     * para un código inválido que para uno expirado, así que el cliente distingue el
     * estado "expirado" con este temporizador, que empieza al enviar (o reenviar) el código.
     */
    private val _otpExpiresAt = MutableStateFlow<Long?>(null)
    val otpExpiresAt: StateFlow<Long?> = _otpExpiresAt.asStateFlow()

    /** Email del flujo activo (registro o recuperación), usado por las pantallas OTP. */
    var pendingEmail: String = ""; private set
    var lastKnownEmail: String = ""; private set

    init {
        observeSession()
        viewModelScope.launch { _rememberMe.value = settings.isRememberMe() }
    }

    fun consume() { _state.value = AuthState.Idle }

    // ---------- Login ----------
    fun login(email: String, password: String, rememberMe: Boolean) {
        if (!AuthValidators.isValidEmail(email)) return fail(ErrorType.InvalidEmail)
        if (password.isEmpty()) return fail(ErrorType.EmptyPassword)
        run(AuthStep.LoggedIn) {
            supabase.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
            settings.setRememberMe(rememberMe) // D-13: se guarda solo si el login tuvo éxito
            _rememberMe.value = rememberMe
        }
    }

    // ---------- Registro ----------
    fun register(name: String, email: String, password: String, confirm: String, acceptedTerms: Boolean) {
        if (name.isBlank()) return fail(ErrorType.InvalidName)
        if (!AuthValidators.isValidEmail(email)) return fail(ErrorType.InvalidEmail)
        passwordError(password, confirm)?.let { _state.value = it; return }
        if (!acceptedTerms) return fail(ErrorType.TermsNotAccepted)

        pendingEmail = email.trim()
        run(AuthStep.SignupOtpSent) {
            val user = supabase.auth.signUpWith(Email) {
                this.email = pendingEmail
                this.password = password
                data = buildJsonObject { put("full_name", name.trim()) }
            }
            // Con confirmación de email activa, Supabase no da error si el correo ya existe:
            // devuelve un usuario sin identities.
            if (user != null && user.identities.orEmpty().isEmpty()) throw EmailTakenException()
            startOtpTimer()
        }
    }

    // ---------- OTP (registro y recuperación) ----------
    fun verifyOtp(mode: OtpMode, code: String) {
        if (code.length != 6) return fail(ErrorType.OtpIncomplete)
        // Si el temporizador del cliente ya venció, ni siquiera se llama al servidor (RF-A5).
        if (isOtpExpired()) {
            _state.value = AuthState.Error(ErrorType.ExpiredOtp, listOf(pendingEmail))
            return
        }
        when (mode) {
            OtpMode.Signup -> run(AuthStep.EmailVerified) {
                supabase.auth.verifyEmailOtp(OtpType.Email.SIGNUP, pendingEmail, code)
            }
            OtpMode.Recovery -> run(AuthStep.RecoveryCodeVerified) {
                supabase.auth.verifyEmailOtp(OtpType.Email.RECOVERY, pendingEmail, code)
            }
        }
    }

    fun resendOtp(mode: OtpMode) = run(AuthStep.CodeResent) {
        when (mode) {
            OtpMode.Signup -> supabase.auth.resendEmail(OtpType.Email.SIGNUP, pendingEmail)
            OtpMode.Recovery -> supabase.auth.resetPasswordForEmail(pendingEmail)
        }
        startOtpTimer()
    }

    private fun startOtpTimer() {
        _otpExpiresAt.value = System.currentTimeMillis() + OTP_EXPIRY_MS
    }

    private fun isOtpExpired(): Boolean {
        val expiresAt = _otpExpiresAt.value ?: return false
        return System.currentTimeMillis() >= expiresAt
    }

    // ---------- Recuperación ----------
    fun sendRecoveryCode(email: String) {
        if (!AuthValidators.isValidEmail(email)) return fail(ErrorType.InvalidEmail)
        pendingEmail = email.trim()
        run(AuthStep.RecoveryCodeSent) {
            supabase.auth.resetPasswordForEmail(pendingEmail)
            startOtpTimer()
        }
    }

    fun updatePassword(password: String, confirm: String) {
        passwordError(password, confirm)?.let { _state.value = it; return }
        run(AuthStep.PasswordUpdated) {
            supabase.auth.updateUser { this.password = password }
            supabase.auth.signOut(SignOutScope.GLOBAL) // "cerramos tu sesión en todos los dispositivos"
        }
    }

    fun signOut() = run(AuthStep.LoggedOut) { supabase.auth.signOut() }

    /**
     * true si esta cuenta ya vio el tutorial y los permisos.
     * Se guarda en la cuenta de Supabase (no en el teléfono), así vale en cualquier dispositivo.
     */
    fun isOnboardingDone(): Boolean =
        supabase.auth.currentUserOrNull()?.userMetadata
            ?.get("onboarding_done")?.jsonPrimitive?.booleanOrNull == true

    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                supabase.auth.updateUser { data = buildJsonObject { put("onboarding_done", true) } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Sin red: no pasa nada, se volverá a mostrar una vez más.
            }
        }
    }

    // ---------- Google ----------
    /** [getIdToken] abre el selector de cuentas (Credential Manager) y devuelve el idToken. */
    fun signInWithGoogle(getIdToken: suspend () -> String) {
        if (_state.value is AuthState.Loading) return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = try {
                val token = getIdToken()
                supabase.auth.signInWith(IDToken) { idToken = token; provider = Google }
                // D-13: con Google no hay casilla "Recordarme"; se restaura siempre.
                settings.setRememberMe(true)
                _rememberMe.value = true
                AuthState.Success(AuthStep.GoogleConnected, Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AuthState.Error(ErrorType.GoogleFailed)
            }
        }
    }

    // ---------- Internos ----------
    /** Devuelve el primer requisito que falla, con el tipo de error correspondiente. */
    private fun passwordError(p: String, c: String): AuthState.Error? {
        val r = PasswordRules.of(p, c)
        return when {
            !r.minLength -> AuthState.Error(ErrorType.PasswordTooShort)
            !r.upperAndDigit -> AuthState.Error(ErrorType.PasswordNeedsUpperAndDigit)
            !r.matches -> AuthState.Error(ErrorType.PasswordMismatch)
            else -> null
        }
    }

    private fun fail(type: ErrorType, arg: String? = null) {
        _state.value = AuthState.Error(type, listOfNotNull(arg))
    }

    private fun run(step: AuthStep, block: suspend () -> Unit) {
        if (_state.value is AuthState.Loading) return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = try {
                block()
                AuthState.Success(step, Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toAuthError()
            }
        }
    }

    /** Los errores del servidor se mapean a tipos; el texto lo pone la UI (RF-A3). */
    private fun Throwable.toAuthError(): AuthState.Error {
        val msg = message.orEmpty().lowercase()
        return when {
            this is IOException -> AuthState.Error(ErrorType.NoNetwork)
            "invalid login credentials" in msg -> AuthState.Error(ErrorType.InvalidCredentials)
            "already registered" in msg || "already exists" in msg -> AuthState.Error(ErrorType.EmailTaken)
            // Supabase no distingue código inválido de expirado: aquí llega siempre como
            // inválido; el estado "expirado" lo decide el temporizador del cliente (RF-A5).
            "token" in msg && ("expired" in msg || "invalid" in msg) -> AuthState.Error(ErrorType.InvalidOtp)
            else -> AuthState.Error(ErrorType.Unknown)
        }
    }

    /**
     * RF-A4 / D-13: decide el destino inicial mientras se muestra el splash.
     * Devuelve `true` si hay sesión que restaurar (va al Home) o `false` (Welcome).
     *
     * Con "Recordarme" sin marcar la sesión guardada se limpia aquí: dura solo
     * mientras la app está abierta y el próximo arranque entra a Welcome.
     * Si Supabase sigue cargando la sesión pasado un tiempo (sin red y sin token
     * válido) se entra a Welcome en lugar de dejar el splash colgado (RNF-05).
     */
    suspend fun restoreSession(): Boolean {
        val status = withTimeoutOrNull(SESSION_RESOLVE_TIMEOUT_MS) {
            supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        } ?: return false
        if (status !is SessionStatus.Authenticated) return false
        if (!settings.isRememberMe()) {
            runCatching { supabase.auth.signOut() }
            return false
        }
        return true
    }

    /** Si había sesión y se pierde sin signOut explícito => sesión expirada. */
    private fun observeSession() = viewModelScope.launch {
        var wasAuthenticated = false
        supabase.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    wasAuthenticated = true
                    lastKnownEmail = status.session.user?.email.orEmpty()
                }
                is SessionStatus.NotAuthenticated -> {
                    if (wasAuthenticated && !status.isSignOut)
                        _state.value = AuthState.Error(ErrorType.SessionExpired)
                    wasAuthenticated = false
                }
                else -> Unit
            }
        }
    }

    companion object {
        /**
         * Expiración del código OTP mostrada y real: 10 minutos (decisión D-15, cerrada en
         * la §14 fila 16). Debe coincidir con `otp_expiry = 600` configurado en Supabase.
         */
        const val OTP_EXPIRY_MS: Long = 10 * 60 * 1000L

        /** Espera máxima para resolver la sesión guardada antes de entrar a Welcome. */
        const val SESSION_RESOLVE_TIMEOUT_MS: Long = 5_000L
    }
}
