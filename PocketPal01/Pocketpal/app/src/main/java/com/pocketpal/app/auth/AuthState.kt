package com.pocketpal.app.auth

enum class ErrorType {
    InvalidCredentials, EmailTaken, NoNetwork, SessionExpired, GoogleFailed, InvalidOtp,
    InvalidEmail, InvalidName, WeakPassword, PasswordMismatch, TermsNotAccepted, Unknown,
}

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
    /** Renderiza banners o pantallas de error según [type]. */
    data class Error(val message: String, val type: ErrorType) : AuthState
}
