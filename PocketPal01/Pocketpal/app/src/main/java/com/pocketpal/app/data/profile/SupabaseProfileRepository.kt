package com.pocketpal.app.data.profile

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Perfil en Supabase: lectura/escritura con Postgrest y foto con Storage.
 *
 * Solo puede tocar la fila del usuario con sesión porque la política RLS
 * "own profile" filtra por `auth.uid() = id`; si la fila no existe devuelve `null`.
 * La foto vive en el bucket público `avatars` bajo `{user_id}/avatar_{timestamp}.jpg`
 * (D-09) y solo se escribe en la carpeta propia por las políticas de Storage.
 */
class SupabaseProfileRepository(private val supabase: SupabaseClient) : ProfileRepository {

    override suspend fun getMyProfile(): Profile? {
        val userId = currentUserId() ?: return null
        return supabase.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }

    override suspend fun getStats() = ProfileStats()

    override fun currentEmail(): String? = supabase.auth.currentUserOrNull()?.email

    override suspend fun savePersonalData(fullName: String, country: String, currency: String): Profile? {
        val userId = currentUserId() ?: return null
        supabase.postgrest.from("profiles")
            .update(
                buildJsonObject {
                    put("full_name", fullName)
                    put("country", country)
                    put("currency", currency)
                }
            ) { filter { eq("id", userId) } }
        return getMyProfile()
    }

    override suspend fun updateAvatar(imageBytes: ByteArray): Profile? {
        val userId = currentUserId() ?: return null
        val previous = getMyProfile()?.avatarPath
        val path = "$userId/avatar_${System.currentTimeMillis()}.jpg"
        supabase.storage.from(AVATARS_BUCKET).upload(path, imageBytes) {
            contentType = ContentType.Image.JPEG
            upsert = false
        }
        // La foto anterior se borra solo si la nueva subió bien (§8.3).
        previous?.let { runCatching { supabase.storage.from(AVATARS_BUCKET).delete(it) } }
        supabase.postgrest.from("profiles")
            .update(
                {
                    set("avatar_path", path)
                }
            ) { filter { eq("id", userId) } }
        return getMyProfile()
    }

    override suspend fun removeAvatar(): Profile? {
        val userId = currentUserId() ?: return null
        val previous = getMyProfile()?.avatarPath ?: return getMyProfile()
        runCatching { supabase.storage.from(AVATARS_BUCKET).delete(previous) }
        supabase.postgrest.from("profiles")
            .update(
                {
                    setToNull("avatar_path")
                }
            ) { filter { eq("id", userId) } }
        return getMyProfile()
    }

    override fun avatarUrl(avatarPath: String?): String? =
        if (avatarPath.isNullOrBlank()) null
        else supabase.storage.from(AVATARS_BUCKET).publicUrl(avatarPath)

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    companion object {
        const val AVATARS_BUCKET = "avatars"
    }
}
