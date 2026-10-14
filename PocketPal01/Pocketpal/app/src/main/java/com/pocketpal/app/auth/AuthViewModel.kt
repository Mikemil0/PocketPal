package com.pocketpal.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException

private class EmailTakenException : Exception("already registered")

class AuthViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _displayName = MutableStateFlow("")
    /** Primer nombre del usuario autenticado, para el saludo del Home. */
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    /** Email del flujo activo (registro o recuperación), usado por las pantallas OTP. */
    var pendingEmail: String = ""; private set
    var lastKnownEmail: String = ""; private set

    init { observeSession() }

    fun consume() { _state.value = AuthState.Idle }

    // ---------- Login ----------
    fun login(email: String, password: String, rememberMe: Boolean) {
        if (!AuthValidators.isValidEmail(email)) return fail("Correo electrónico no válido", ErrorType.InvalidEmail)
        if (password.isEmpty()) return fail("Ingresa tu contraseña", ErrorType.InvalidCredentials)
        // TODO: persistir rememberMe (DataStore) y cerrar sesión al salir si es false.
        run(AuthStep.LoggedIn) {
            supabase.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        }
    }

    // ---------- Registro ----------
    fun register(name: String, email: String, password: String, confirm: String, acceptedTerms: Boolean) {
        if (name.isBlank()) return fail("Ingresa tu nombre completo", ErrorType.InvalidName)
        if (!AuthValidators.isValidEmail(email)) return fail("Correo electrónico no válido", ErrorType.InvalidEmail)
        passwordError(password, confirm)?.let { _state.value = it; return }
        if (!acceptedTerms) return fail("Es obligatorio aceptar los Términos y condiciones y la Política de privacidad.", ErrorType.TermsNotAccepted)

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
        }
    }

    // ---------- OTP (registro y recuperación) ----------
    fun verifyOtp(mode: OtpMode, code: String) {
        if (code.length != 6) return fail("Ingresa los 6 dígitos", ErrorType.InvalidOtp)
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
    }

    // ---------- Recuperación ----------
    fun sendRecoveryCode(email: String) {
        if (!AuthValidators.isValidEmail(email)) return fail("Correo electrónico no válido", ErrorType.InvalidEmail)
        pendingEmail = email.trim()
        run(AuthStep.RecoveryCodeSent) { supabase.auth.resetPasswordForEmail(pendingEmail) }
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
                AuthState.Success(AuthStep.GoogleConnected, Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AuthState.Error("No pudimos conectar tu cuenta", ErrorType.GoogleFailed)
            }
        }
    }

    // ---------- Internos ----------
    /** Devuelve el primer requisito que falla, con un mensaje específico. */
    private fun passwordError(p: String, c: String): AuthState.Error? {
        val r = PasswordRules.of(p, c)
        return when {
            !r.minLength -> AuthState.Error("Mínimo 8 caracteres", ErrorType.WeakPassword)
            !r.upperAndDigit -> AuthState.Error("Incluye una mayúscula y un número", ErrorType.WeakPassword)
            !r.matches -> AuthState.Error("Las contraseñas no coinciden", ErrorType.PasswordMismatch)
            else -> null
        }
    }

    private fun fail(message: String, type: ErrorType) { _state.value = AuthState.Error(message, type) }

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

    private fun Throwable.toAuthError(): AuthState.Error {
        val msg = message.orEmpty().lowercase()
        return when {
            this is IOException -> AuthState.Error("Sin conexión a internet", ErrorType.NoNetwork)
            "invalid login credentials" in msg ->
                AuthState.Error("Correo o contraseña incorrectos. Verifica tus datos", ErrorType.InvalidCredentials)
            "already registered" in msg || "already exists" in msg ->
                AuthState.Error("Este correo ya tiene una cuenta en PocketPal.", ErrorType.EmailTaken)
            "token" in msg && ("expired" in msg || "invalid" in msg) ->
                AuthState.Error("Código inválido o expirado", ErrorType.InvalidOtp)
            else -> AuthState.Error("Algo salió mal. Inténtalo de nuevo", ErrorType.Unknown)
        }
    }

    /** Si había sesión y se pierde sin signOut explícito => sesión expirada. */
    private fun observeSession() = viewModelScope.launch {
        var wasAuthenticated = false
        supabase.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    wasAuthenticated = true
                    lastKnownEmail = status.session.user?.email.orEmpty()
                    val meta = status.session.user?.userMetadata
                    val fullName = (meta?.get("full_name") ?: meta?.get("name"))?.jsonPrimitive?.contentOrNull
                    _displayName.value = (fullName ?: lastKnownEmail.substringBefore("@"))
                        .trim().substringBefore(" ")
                }
                is SessionStatus.NotAuthenticated -> {
                    if (wasAuthenticated && !status.isSignOut)
                        _state.value = AuthState.Error("Tu sesión expiró", ErrorType.SessionExpired)
                    wasAuthenticated = false
                }
                else -> Unit
            }
        }
    }
}
