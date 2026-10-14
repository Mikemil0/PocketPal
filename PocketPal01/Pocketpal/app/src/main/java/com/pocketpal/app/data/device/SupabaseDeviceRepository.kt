package com.pocketpal.app.data.device

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Sesiones activas en Supabase (tabla `user_devices` de la Fase 2 con RLS propia).
 *
 * - El registro del dispositivo ocurre al entrar a la app (§8.5): nombre del equipo,
 *   `session_id` del claim del JWT y `last_active_at`. La ubicación aproximada se
 *   omite (§8.5 lo permite; no hay geolocalización por IP).
 * - Revocar (D-01 opción B) llama a la Edge Function `revoke-device`, que intenta
 *   matar la sesión con `service_role` y marca `revoked_at`. Si eso no tiene efecto,
 *   se aplica el fallback A: `SignOutScope.OTHERS` desde este dispositivo.
 */
class SupabaseDeviceRepository(
    private val supabase: SupabaseClient,
    private val deviceName: String,
) : DeviceRepository {

    override fun currentSessionId(): String? =
        supabase.auth.currentSessionOrNull()?.accessToken?.let(::sessionIdFromJwt)

    override suspend fun registerCurrentDevice() {
        val userId = currentUserId() ?: return
        val sessionId = currentSessionId()
        val now = Instant.now().toString()
        val existing = fetch(userId).firstOrNull { sessionId != null && it.sessionId == sessionId }
        if (existing != null) {
            supabase.postgrest.from(TABLE).update(
                buildJsonObject {
                    put("last_active_at", now)
                    put("device_name", deviceName)
                }
            ) { filter { eq("id", existing.id) } }
        } else {
            supabase.postgrest.from(TABLE).insert(
                buildJsonObject {
                    put("user_id", userId)
                    if (sessionId != null) put("session_id", sessionId)
                    put("device_name", deviceName)
                    put("platform", "android")
                    put("last_active_at", now)
                }
            )
        }
    }

    override suspend fun list(): List<DeviceSession> {
        val userId = currentUserId() ?: return emptyList()
        return fetch(userId)
            .filter { it.revokedAt == null }
            .sortedByDescending { it.lastActiveAt }
            .map {
                DeviceSession(
                    id = it.id,
                    deviceName = it.deviceName,
                    platform = it.platform,
                    lastActiveAt = it.lastActiveAt,
                    sessionId = it.sessionId,
                )
            }
    }

    override suspend fun revoke(deviceId: String) {
        val serverSide = runCatching {
            val text = supabase.functions.invoke(
                FUNCTION,
                buildJsonObject { put("device_id", deviceId) },
            ).bodyAsText()
            text.contains("\"revoked\":true")
        }.getOrDefault(false)
        if (!serverSide) {
            // Fallback A (D-01): sin revocación server-side por sesión, se cierran
            // las demás sesiones desde este dispositivo (§14, fila 2).
            revokeAllOthers()
        }
    }

    override suspend fun revokeAllOthers() {
        val userId = currentUserId() ?: return
        val current = currentSessionId()
        val now = Instant.now().toString()
        for (row in fetch(userId)) {
            if (row.revokedAt == null && row.sessionId != current) {
                supabase.postgrest.from(TABLE).update(
                    buildJsonObject { put("revoked_at", now) }
                ) { filter { eq("id", row.id) } }
            }
        }
        supabase.auth.signOut(SignOutScope.OTHERS)
    }

    private suspend fun fetch(userId: String): List<DeviceRow> =
        supabase.postgrest.from(TABLE)
            .select { filter { eq("user_id", userId) } }
            .decodeList()

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Claim `session_id` del payload del JWT (lo usa la Edge Function para revocar). */
    private fun sessionIdFromJwt(token: String): String? = try {
        val payload = token.split(".")[1]
        val decoded = String(
            android.util.Base64.decode(
                payload,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP,
            )
        )
        Regex("\"session_id\"\\s*:\\s*\"([^\"]+)\"").find(decoded)?.groupValues?.get(1)
    } catch (e: Exception) {
        null
    }

    @Serializable
    private data class DeviceRow(
        val id: String,
        @SerialName("device_name") val deviceName: String = "",
        val platform: String = "android",
        @SerialName("last_active_at") val lastActiveAt: String = "",
        @SerialName("session_id") val sessionId: String? = null,
        @SerialName("revoked_at") val revokedAt: String? = null,
    )

    companion object {
        const val TABLE = "user_devices"
        const val FUNCTION = "revoke-device"
    }
}
