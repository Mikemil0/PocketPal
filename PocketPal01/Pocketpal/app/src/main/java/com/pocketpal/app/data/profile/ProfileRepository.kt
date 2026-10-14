package com.pocketpal.app.data.profile

/**
 * Contrato del perfil (RNF-02): las ViewModels dependen de esta interfaz, **no** de
 * Supabase directamente, para poder probarse con datos simulados.
 */
interface ProfileRepository {

    /** Perfil del usuario con sesión activa, o `null` si no hay sesión o falta la fila. */
    suspend fun getMyProfile(): Profile?

    /** Contadores de la pantalla Mi perfil; 0 hasta que existan `movements` y `goals`. */
    suspend fun getStats(): ProfileStats

    /** Correo de la cuenta con sesión (vive en `auth.users`, no en `profiles`). */
    fun currentEmail(): String?

    /**
     * Guarda los datos personales (RN-01, RN-09) y devuelve la fila actualizada.
     * `full_name` es obligatorio y de máximo 60 caracteres (la validación la hace la UI).
     */
    suspend fun savePersonalData(fullName: String, country: String, currency: String): Profile?

    /**
     * Guarda las preferencias generales (RNF-06, RN-09) y devuelve la fila actualizada.
     * [language] vacío = "Automático" (idioma del dispositivo): en ese caso **no se
     * escribe el idioma** en la cuenta porque el CHECK de `profiles.language` solo
     * admite `es`/`en`/`pt`/`fr` y "Automático" es una preferencia del equipo (§14).
     */
    suspend fun savePreferences(
        language: String,
        theme: String,
        currency: String,
        dateFormat: String,
        weekStart: Int,
    ): Profile?

    /**
     * Sube la foto de perfil (RNF-04: la recibe ya comprimida como JPEG), borra la
     * anterior (§8.3) y devuelve la fila actualizada con su nuevo `avatar_path`.
     */
    suspend fun updateAvatar(imageBytes: ByteArray): Profile?

    /** Quita la foto actual: borra el archivo de Storage y pone `avatar_path` a null. */
    suspend fun removeAvatar(): Profile?

    /** URL pública del bucket `avatars` (D-09) o `null` si no hay foto. */
    fun avatarUrl(avatarPath: String?): String?
}
