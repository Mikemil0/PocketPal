package com.pocketpal.app.data.profile

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

/**
 * Lectura del perfil desde Supabase con Postgrest.
 *
 * Solo puede leer la fila del usuario con sesión porque la política RLS
 * "own profile" filtra por `auth.uid() = id`; si la fila no existe devuelve `null`.
 */
class SupabaseProfileRepository(private val supabase: SupabaseClient) : ProfileRepository {

    override suspend fun getMyProfile(): Profile? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }

    override suspend fun getStats() = ProfileStats()
}
