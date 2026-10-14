package com.pocketpal.app.data.security

/**
 * Contrato de seguridad (RNF-02): las ViewModels dependen de esta interfaz, no de
 * Supabase. Cubre el cambio de contraseña (§8.6, RF-B4).
 */
interface SecurityRepository {

    /** RN-05: `false` si la cuenta es solo de Google (sin identidad de contraseña). */
    fun hasPassword(): Boolean

    /** Correo de la cuenta con sesión (para reautenticar al cambiar la contraseña). */
    fun currentEmail(): String?

    /**
     * Reautentica con la contraseña actual antes de cambiarla (§8.6). `false` si no
     * coincide (la pantalla muestra "Contraseña incorrecta").
     */
    suspend fun reauthenticate(email: String, password: String): Boolean

    /** Cambia la contraseña (la cuenta ya debe estar reautenticada). */
    suspend fun changePassword(newPassword: String)

    /** Guarda `profiles.password_changed_at` (§8.6) y devuelve el instante ISO. */
    suspend fun markPasswordChanged(): String

    /** Última actualización de contraseña (ISO) o `null` si nunca se cambió (§8.5). */
    suspend fun passwordChangedAt(): String?
}
