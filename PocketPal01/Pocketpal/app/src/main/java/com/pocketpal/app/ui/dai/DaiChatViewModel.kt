package com.pocketpal.app.ui.dai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.data.dai.DaiMessage
import com.pocketpal.app.data.dai.DaiReply
import com.pocketpal.app.data.dai.DaiRepository
import com.pocketpal.app.data.dai.DaiRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 1 · El chat con Dai (Figma "02_Chat_Principal" y "10_Chat_Nuevo").
 *
 * ```
 * send("¿Cómo voy?")
 *   1. guarda el mensaje del usuario en dai_messages      (aparece a la derecha)
 *   2. typing = true                                       ("• • •" de Dai)
 *   3. repository.snapshot(...)                            (movimientos, presupuestos, metas)
 *   4. DaiBrain.reply(texto, datos)                        (las reglas arman la respuesta)
 *   5. guarda la respuesta de Dai en dai_messages          (aparece a la izquierda)
 * ```
 * [userName], [currency], [weekStart], [today] y [zone] son funciones para poder fijarlos
 * en las pruebas y para leer siempre el valor más reciente del perfil.
 */
class DaiChatViewModel(
    private val repository: DaiRepository,
    private val userName: () -> String = { "" },
    private val currency: () -> String = { "COP" },
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    /** Pausa de "Dai está escribiendo…" para que la respuesta no aparezca de golpe. */
    private val replyDelayMs: Long = REPLY_DELAY_MS,
) : ViewModel() {

    data class ChatUi(
        val messages: List<DaiMessage> = emptyList(),
        /** Ya se trajo el historial (para no mostrar "chat nuevo" mientras carga). */
        val loaded: Boolean = false,
        /** Dai está "escribiendo" (se ven los puntos). */
        val typing: Boolean = false,
        val error: ErrorType? = null,
    ) {
        /** Sin mensajes: se muestra "¿En qué puedo ayudarte hoy?" (Figma "10_Chat_Nuevo"). */
        val isNew: Boolean get() = loaded && messages.isEmpty()

        /** Las respuestas rápidas solo se ven debajo del ÚLTIMO mensaje, si es de Dai. */
        val chips: List<String> get() = messages.lastOrNull()?.takeIf { !it.isUser && !typing }?.chips.orEmpty()
    }

    private val _state = MutableStateFlow(ChatUi())
    val state: StateFlow<ChatUi> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** El texto que no se pudo enviar (para "Reintentar"). */
    private var failedText: String? = null

    /** Al abrir el chat: trae el historial una sola vez. */
    fun open() {
        if (_state.value.loaded || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            try {
                _state.value = _state.value.copy(messages = repository.history(), loaded = true, error = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(loaded = true, error = ErrorType.NoNetwork)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loaded = true, error = ErrorType.Unknown)
            }
        }
    }

    /** Fuerza a volver a traer el historial (por ejemplo, después de guardar una factura). */
    fun reload() {
        loadJob?.cancel()
        _state.value = _state.value.copy(loaded = false)
        open()
    }

    /** Lo que escribe el usuario (o el texto de una respuesta rápida o de una sugerencia). */
    fun send(text: String) = respond(text, saveUserMessage = true)

    private fun respond(text: String, saveUserMessage: Boolean) {
        val clean = text.trim().take(DaiBrain.MAX_MESSAGE)
        if (clean.isEmpty() || _state.value.typing) return
        open()
        viewModelScope.launch {
            loadJob?.join()   // primero el historial, para no mezclar el orden
            failedText = null
            _state.value = _state.value.copy(typing = true, error = null)
            try {
                if (saveUserMessage) {
                    val mine = repository.save(DaiRole.User, clean)
                    _state.value = _state.value.copy(messages = _state.value.messages + mine)
                }
                val reply = answer(clean)
                delay(replyDelayMs)
                val saved = repository.save(DaiRole.Dai, reply.text, reply.card).copy(chips = reply.chips)
                _state.value = _state.value.copy(messages = _state.value.messages + saved, typing = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                failedText = clean
                _state.value = _state.value.copy(typing = false, error = ErrorType.NoNetwork)
            } catch (e: Exception) {
                failedText = clean
                _state.value = _state.value.copy(typing = false, error = ErrorType.Unknown)
            }
        }
    }

    /** Arma la respuesta con los datos reales del usuario. */
    private suspend fun answer(text: String): DaiReply {
        val tz = zone()
        val data = repository.snapshot(today(), tz, weekStart(), userName())
        return DaiBrain.reply(text, data, tz, currency())
    }

    /** "Reintentar": si falló un envío lo repite; si falló el historial, lo vuelve a pedir. */
    fun retry() {
        val text = failedText
        if (text != null) {
            // Si el mensaje del usuario sí alcanzó a guardarse, solo falta la respuesta de Dai.
            val alreadySaved = _state.value.messages.lastOrNull()?.let { it.isUser && it.text == text } == true
            respond(text, saveUserMessage = !alreadySaved)
        } else {
            reload()
        }
    }

    fun consumeError() { _state.value = _state.value.copy(error = null) }

    /** Después de "Borrar historial" (flujo 4): el chat queda nuevo. */
    fun clearLocal() {
        loadJob?.cancel()
        _state.value = ChatUi(loaded = true)
    }

    companion object {
        const val REPLY_DELAY_MS = 700L
    }
}
