package com.pocketpal.app.data.security

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Seguridad en Supabase (RF-B4, §8.6): el cambio de contraseña se hace
 * reautenticando con la contraseña actual (`signInWith`) y luego `updateUser`;
 * `password_changed_at` se guarda en `profiles` para el subtítulo "Última
 * actualización" (§8.5).
 */
class SupabaseSecurityRepository(private val supabase: SupabaseClient) : SecurityRepository {

    override fun hasPassword(): Boolean =
        supabase.auth.currentUserOrNull()?.identities
            ?.any { it.provider == "email" } ?: false

    override fun currentEmail(): String? = supabase.auth.currentUserOrNull()?.email

    override suspend fun reauthenticate(email: String, password: String): Boolean = try {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        true
    } catch (e: Exception) {
        // Credenciales inválidas u otro error de auth: la UI muestra "Contraseña incorrecta".
        false
    }

    override suspend fun changePassword(newPassword: String) {
        supabase.auth.updateUser { password = newPassword }
    }

    override suspend fun markPasswordChanged(): String {
        val now = Instant.now().toString()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return now
        supabase.postgrest.from("profiles")
            .update(
                buildJsonObject { put("password_changed_at", now) }
            ) { filter { eq("id", userId) } }
        return now
    }

    override suspend fun passwordChangedAt(): String? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<PasswordChangedRow>()?.at
    }

    @Serializable
    private data class PasswordChangedRow(
        @SerialName("password_changed_at") val at: String? = null,
    )
}
