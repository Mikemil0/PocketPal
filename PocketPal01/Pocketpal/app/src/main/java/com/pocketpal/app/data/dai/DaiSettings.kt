package com.pocketpal.app.data.dai

import kotlinx.coroutines.flow.Flow

/** Preferencias de Dai que se guardan EN EL TELÉFONO (no en Supabase). */
data class DaiSettings(
    /** "Voz · Activar o desactivar la voz de Dai": Dai lee en voz alta sus respuestas. */
    val voiceReplies: Boolean = false,
    /**
     * Dirección del servidor de Dai (proyecto de IA) que lee las facturas con PaddleOCR.
     * Vacía = las facturas se leen solo en el teléfono (ML Kit).
     */
    val serverUrl: String = "",
)

/**
 * Interfaz de las preferencias de Dai: la app usa DataStore ([DataStoreDaiSettings]) y
 * las pruebas una versión en memoria.
 */
interface DaiSettingsRepository {
    val settings: Flow<DaiSettings>
    suspend fun setVoiceReplies(enabled: Boolean)
    suspend fun setServerUrl(url: String)
}

/** Por qué no se pudo escuchar al usuario en el modo voz. */
enum class VoiceProblem {
    /** No se entendió nada: "Disculpa, ¿puedes repetirlo?". */
    NotUnderstood,
    /** Sin internet o el servicio no respondió: "Hemos tenido un problema de conexión". */
    Connection,
    /** Falta el permiso del micrófono: "Micrófono bloqueado". */
    Permission,
    /** El teléfono no tiene reconocimiento de voz o está ocupado. */
    Unavailable,
}

/**
 * Traduce los códigos de error de `android.speech.SpeechRecognizer` (son números fijos de
 * Android) a lo que muestra la pantalla. Es puro: se prueba con JUnit.
 */
object DaiVoice {
    const val ERROR_NETWORK_TIMEOUT = 1
    const val ERROR_NETWORK = 2
    const val ERROR_AUDIO = 3
    const val ERROR_SERVER = 4
    const val ERROR_CLIENT = 5
    const val ERROR_SPEECH_TIMEOUT = 6
    const val ERROR_NO_MATCH = 7
    const val ERROR_RECOGNIZER_BUSY = 8
    const val ERROR_INSUFFICIENT_PERMISSIONS = 9

    fun problemFor(errorCode: Int): VoiceProblem = when (errorCode) {
        ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT -> VoiceProblem.NotUnderstood
        ERROR_NETWORK, ERROR_NETWORK_TIMEOUT, ERROR_SERVER -> VoiceProblem.Connection
        ERROR_INSUFFICIENT_PERMISSIONS -> VoiceProblem.Permission
        else -> VoiceProblem.Unavailable
    }

    /** El mejor resultado del reconocimiento, limpio (o `null` si vino vacío). */
    fun bestResult(results: List<String>?): String? =
        results?.firstOrNull { it.isNotBlank() }?.trim()?.replaceFirstChar { it.uppercase() }
}
