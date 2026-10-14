package com.pocketpal.app.auth

object AuthValidators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String) = EMAIL.matches(email.trim())
    fun hasMinLength(p: String) = p.length >= 8
    fun hasUpperAndDigit(p: String) = p.any { it.isUpperCase() } && p.any { it.isDigit() }
}

/** Las 3 reglas del PasswordStrengthMeter. */
data class PasswordRules(
    val minLength: Boolean,
    val upperAndDigit: Boolean,
    val matches: Boolean,
) {
    val score: Int get() = listOf(minLength, upperAndDigit, matches).count { it }
    val allValid: Boolean get() = score == 3

    companion object {
        fun of(password: String, confirm: String) = PasswordRules(
            minLength = AuthValidators.hasMinLength(password),
            upperAndDigit = AuthValidators.hasUpperAndDigit(password),
            matches = password.isNotEmpty() && password == confirm,
        )
    }
}
