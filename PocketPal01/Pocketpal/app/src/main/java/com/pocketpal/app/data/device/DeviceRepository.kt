package com.pocketpal.app.data.device

/**
 * Dispositivo con sesión (fila de `user_devices`, D-01 opción B).
 * [lastActiveAt] y [revokedAt] son instantes ISO; [sessionId] identifica la sesión
 * del JWT y permite saber cuál es el dispositivo actual.
 */
data class DeviceSession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val lastActiveAt: String,
    val sessionId: String?,
)

/**
 * Contrato de sesiones activas (RNF-02, §8.5 / RF-B5).
 */
interface DeviceRepository {

    /** Claim `session_id` del JWT actual (o `null` si no está disponible). */
    fun currentSessionId(): String?

    /** Registra o actualiza el dispositivo actual al entrar a la app (§8.5). */
    suspend fun registerCurrentDevice()

    /** Dispositivos con sesión activa, del más reciente al más antiguo. */
    suspend fun list(): List<DeviceSession>

    /**
     * Revoca la sesión de un dispositivo (D-01 opción B): llama a la Edge Function
     * `revoke-device` (que intenta la revocación server-side con `service_role` y
     * marca `revoked_at`). Si la revocación server-side no tiene efecto, aplica el
     * **fallback A** (D-01): cierra las demás sesiones desde el dispositivo actual
     * con `SignOutScope.OTHERS` y marca los demás dispositivos como revocados.
     */
    suspend fun revoke(deviceId: String)

    /** Fallback A (D-01): `SignOutScope.OTHERS` y marca revocados los demás dispositivos. */
    suspend fun revokeAllOthers()
}
