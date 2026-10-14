package com.pocketpal.app.auth

/**
 * Tipos de error. RF-A3: los ViewModels **no emiten texto**, emiten este tipo
 * y la UI lo traduce (ver `ui/ErrorMessages.kt`).
 */
enum class ErrorType {
    InvalidCredentials, EmptyPassword, EmailTaken, NoNetwork, SessionExpired, GoogleFailed,
    InvalidOtp, ExpiredOtp, OtpIncomplete,
    InvalidEmail, InvalidName, NameTooLong, PasswordTooShort, PasswordNeedsUpperAndDigit, PasswordMismatch,
    TermsNotAccepted, Unknown,
}

/** Las dos variantes del antiguo `WeakPassword` (RF-A3: mensaje específico por regla). */
val ErrorType.isPasswordProblem: Boolean
    get() = this == ErrorType.PasswordTooShort || this == ErrorType.PasswordNeedsUpperAndDigit

enum class AuthStep {
    LoggedIn, SignupOtpSent, EmailVerified,
    RecoveryCodeSent, RecoveryCodeVerified, PasswordUpdated,
    CodeResent, GoogleConnected, LoggedOut,
}

enum class OtpMode { Signup, Recovery }

sealed interface AuthState {
    /** UI por defecto. */
    data object Idle : AuthState
    /** Bloquea la UI y muestra spinners. */
    data object Loading : AuthState
    /** Navega a la siguiente pantalla según [step]. */
    data class Success<T>(val step: AuthStep, val data: T) : AuthState
    /**
     * Renderiza banners o pantallas de error según [type].
     * [args] son los datos que el texto necesita (p. ej. el correo en un código expirado);
     * la traducción ocurre en la UI, nunca aquí (RF-A3).
     */
    data class Error(val type: ErrorType, val args: List<String> = emptyList()) : AuthState
}
