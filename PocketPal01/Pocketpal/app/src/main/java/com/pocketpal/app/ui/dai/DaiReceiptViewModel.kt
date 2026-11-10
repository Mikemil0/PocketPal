package com.pocketpal.app.ui.dai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiRepository
import com.pocketpal.app.data.dai.DaiServerMapping
import com.pocketpal.app.data.dai.DaiRole
import com.pocketpal.app.data.dai.FinanceSnapshot
import com.pocketpal.app.data.dai.ReceiptDraft
import com.pocketpal.app.data.dai.ReceiptImpact
import com.pocketpal.app.data.dai.ReceiptParser
import com.pocketpal.app.data.dai.ReceiptProblem
import com.pocketpal.app.data.dai.ReceiptAnalyzer
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 3 · Factura con la cámara: foto → OCR (ML Kit) → "Confirmar" → guardar.
 *
 * La pantalla de la cámara y la de "Confirmar" comparten este ViewModel (por eso se
 * crea en MainActivity). El OCR corre en el teléfono: aquí solo llega el TEXTO que leyó.
 *
 * Regla de oro: NADA se guarda solo. El texto se convierte en un borrador
 * ([ReceiptParser.parse]), el usuario lo corrige, ve "¿Dónde se guardará?" y solo al
 * confirmar se crea el movimiento.
 */
class DaiReceiptViewModel(
    private val repository: DaiRepository,
    private val userName: () -> String = { "" },
    private val currency: () -> String = { "COP" },
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    /**
     * Servidor de Dai (proyecto de IA, PaddleOCR + modelos entrenados). `null` o sin
     * dirección configurada = solo se usa el OCR del teléfono.
     */
    private val analyzer: ReceiptAnalyzer? = null,
) : ViewModel() {

    /** En qué paso va el usuario. */
    enum class Stage {
        /** La cámara está lista para tomar la foto. */
        Camera,

        /** La foto ya se tomó y el OCR está leyendo. */
        Reading,

        /** Hay datos (leídos o "Manual"): se abre "Confirmar". */
        Review,
    }

    data class UiState(
        val stage: Stage = Stage.Camera,
        /** Lo que se va a guardar; todo es editable en "Confirmar". */
        val draft: ReceiptDraft = ReceiptDraft(
            merchant = "",
            amount = null,
            date = LocalDate.now(),
            categoryId = null,
            reason = "",
        ),
        /** Categorías de gastos del usuario, para elegir en "Confirmar". */
        val categories: List<MovementCategory> = emptyList(),
        /** `true` = el OCR no encontró nada útil: la cámara ofrece reintentar o ir a "Manual". */
        val readFailed: Boolean = false,
        /** Primer problema del borrador al tocar Guardar (`null` = todo bien). */
        val problem: ReceiptProblem? = null,
        /** `true` = se muestra "¿Dónde se guardará?" antes de guardar. */
        val confirming: Boolean = false,
        val impact: ReceiptImpact? = null,
        val saving: Boolean = false,
        val error: ErrorType? = null,
    )

    sealed interface Event {
        /** La factura se guardó: se vuelve al chat. */
        data object Saved : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private var finance: FinanceSnapshot? = null

    /** La foto de la factura que se está leyendo (para enviarla al servidor de Dai). */
    private var photo: CompletableDeferred<ByteArray?>? = null
    private var financeJob: Job? = null

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _state.update { it.copy(error = null) } }

    /** Se llama al tocar la cámara en el chat: empieza una factura nueva desde cero. */
    fun start() {
        _event.value = null
        _state.value = UiState(draft = emptyDraft())
        loadFinance()
    }

    /** El usuario tomó la foto y el OCR empezó a leer. */
    fun onReading() {
        photo = CompletableDeferred()
        _state.update { it.copy(stage = Stage.Reading, readFailed = false) }
    }

    /** La foto ya preparada (enderezada y reducida) para el servidor de Dai; `null` si no se pudo. */
    fun onPhoto(jpeg: ByteArray?) {
        photo?.complete(jpeg)
    }

    /** ML Kit terminó: [text] es todo lo que leyó de la factura. */
    fun onTextRecognized(text: String) {
        viewModelScope.launch {
            // Las categorías vienen del mismo snapshot: se espera a que termine de cargar.
            financeJob?.join()
            val categories = _state.value.categories
            var draft = ReceiptParser.parse(text, today(), categories)
            // Si hay servidor de Dai, sus resultados (PaddleOCR + modelos) reemplazan a los del teléfono.
            val server = askServer()
            if (server != null) draft = DaiServerMapping.merge(draft, server, categories, today())
            // Montos en pesos enteros, como en el registro manual y en el Excel ($242.574,31 → $242.574).
            draft = draft.copy(amount = draft.amount?.let { Math.round(it).toDouble() })
            val foundSomething = draft.amount != null || draft.merchant.isNotBlank()
            if ((server == null && text.isBlank()) || !foundSomething) {
                onReadFailed()
                return@launch
            }
            _state.update {
                it.copy(
                    stage = Stage.Review,
                    draft = draft,
                    readFailed = false,
                    problem = null,
                    confirming = false,
                    impact = null,
                )
            }
        }
    }

    /** Envía la foto al servidor de Dai. Cualquier problema = `null` y sigue la lectura del teléfono. */
    private suspend fun askServer(): com.pocketpal.app.data.dai.ServerReceipt? {
        val api = analyzer ?: return null
        val pending = photo ?: return null
        photo = null
        val jpeg = withTimeoutOrNull(PHOTO_WAIT_MS) { pending.await() } ?: return null
        return try {
            withTimeoutOrNull(SERVER_WAIT_MS) { api.analyze(jpeg) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** El OCR falló o no encontró texto: se vuelve a la cámara con un aviso. */
    fun onReadFailed() {
        _state.update { it.copy(stage = Stage.Camera, readFailed = true) }
    }

    /** "Ingresar manual": se abre "Confirmar" con el borrador vacío. */
    fun startManual() {
        _state.update {
            it.copy(
                stage = Stage.Review,
                draft = emptyDraft(),
                readFailed = false,
                problem = null,
                confirming = false,
                impact = null,
            )
        }
    }

    /** "Tomar otra foto" (o atrás desde "Confirmar"): vuelve a la cámara sin guardar nada. */
    fun retake() {
        photo = null
        _state.update {
            it.copy(
                stage = Stage.Camera,
                draft = emptyDraft(),
                readFailed = false,
                problem = null,
                confirming = false,
                impact = null,
                saving = false,
            )
        }
    }

    /** El usuario cambió algún campo en "Confirmar". */
    fun update(draft: ReceiptDraft) {
        _state.update { it.copy(draft = draft, problem = null) }
    }

    /** Botón "Guardar": valida y, si está bien, muestra "¿Dónde se guardará?". */
    fun requestSave() {
        val current = _state.value
        if (current.saving) return
        val problem = ReceiptParser.validate(current.draft, today())
        if (problem != null) {
            _state.update { it.copy(problem = problem) }
            return
        }
        val data = finance
        val impact = if (data != null) {
            ReceiptParser.impact(current.draft, data, weekStart())
        } else {
            // Sin datos del presupuesto igual se puede guardar: solo se muestra la categoría.
            ReceiptImpact(
                categoryName = current.categories.firstOrNull { it.id == current.draft.categoryId }?.name,
                budgetName = null,
                budgetPercentBefore = null,
                budgetPercentAfter = null,
                exceedsBudget = false,
            )
        }
        _state.update { it.copy(problem = null, impact = impact, confirming = true) }
    }

    fun cancelConfirm() {
        _state.update { it.copy(confirming = false) }
    }

    /** "Sí, guardar": crea el movimiento y deja la respuesta de Dai en el chat. */
    fun confirmSave() {
        val current = _state.value
        val impact = current.impact ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val draft = current.draft.copy(merchant = ReceiptParser.titleFor(current.draft, impact.categoryName))
                repository.saveReceipt(draft, zone())
                // El gasto ya quedó guardado; si falla el mensaje del chat no se pierde nada.
                try {
                    val reply = ReceiptParser.savedReply(draft, impact, currency())
                    repository.save(DaiRole.Dai, reply.text, reply.card)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Se ignora a propósito.
                }
                _state.update { it.copy(saving = false, confirming = false) }
                _event.value = Event.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                fail(ErrorType.NoNetwork)
            } catch (e: Exception) {
                fail(ErrorType.Unknown)
            }
        }
    }

    private fun fail(type: ErrorType) {
        _state.update { it.copy(saving = false, confirming = false, error = type) }
    }

    private fun emptyDraft() = ReceiptDraft(
        merchant = "",
        amount = null,
        date = today(),
        categoryId = null,
        reason = "",
    )

    /** Carga categorías y presupuestos mientras el usuario toma la foto. */
    private fun loadFinance() {
        financeJob?.cancel()
        financeJob = viewModelScope.launch {
            try {
                val data = repository.snapshot(today(), zone(), weekStart(), userName())
                finance = data
                val expenses = data.categories.filter { it.kind == MovementKind.Expense.wire }
                _state.update { it.copy(categories = expenses) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                finance = null
            }
        }
    }

    private companion object {
        /** Lo máximo que se espera a que la foto quede lista. */
        const val PHOTO_WAIT_MS = 5_000L

        /** Lo máximo que se espera al servidor de Dai antes de usar la lectura del teléfono. */
        const val SERVER_WAIT_MS = 90_000L
    }
}
