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
}
