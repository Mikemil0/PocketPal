package com.pocketpal.app.ui.newmovement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.VoiceMovementParser
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.NewMovement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * Módulo 6 · Flujo 5 · Registrar un movimiento desde el botón **+** del inicio.
 *
 * ```
 * "Registro manual"  → startManual()      → formulario vacío (Figma "REG MANUAL")
 * "Registro por voz" → startVoice(texto)  → VoiceMovementParser llena el formulario
 *                                           (Figma "CONFI DATOS"); el usuario corrige
 * Guardar → save() → valida → repository.create(...) → insert en movements
 *        → Event.Saved → "Movimiento registrado correctamente" (Figma "REGISTRO EXITO")
 * ```
 * El "Registro mediante escáner" usa la cámara y el OCR de Dai (DaiReceiptViewModel, flujo 3).
 */
class NewMovementViewModel(
    private val repository: MovementRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    /** De dónde vienen los datos: el formulario (`source = 'manual'`) o la voz (`'dai'`). */
    enum class Mode { Manual, Voice }

    /** Lo que falta corregir antes de guardar. */
    enum class Problem { Amount, Category, FutureDate }

    /** Los campos del formulario. El monto son solo dígitos, como en metas y presupuestos. */
    data class Form(
        val kind: MovementKind = MovementKind.Expense,
        val amountDigits: String = "",
        val date: LocalDate? = null,
        val categoryId: String? = null,
        val reason: String = "",
    )

    data class NewMovementUi(
        val mode: Mode = Mode.Manual,
        val form: Form = Form(),
        /** Todas las categorías del usuario (gastos e ingresos). */
        val categories: List<MovementCategory> = emptyList(),
        /** Lo que se dijo por voz (se muestra entre comillas en "Confirmar"). */
        val heard: String = "",
        val saving: Boolean = false,
        val problem: Problem? = null,
        val error: ErrorType? = null,
        /** El usuario cambió algo: al salir se pregunta "¿Quieres descartar los cambios?". */
        val changed: Boolean = false,
    ) {
        /** Solo las categorías del tipo elegido (gasto o ingreso). */
        val kindCategories: List<MovementCategory> get() = categories.filter { it.kind == form.kind.wire }
        val selectedCategory: MovementCategory? get() = categories.firstOrNull { it.id == form.categoryId }
        /** Lo dicho por voz también es un cambio: si sale sin guardar, se pregunta. */
        val hasChanges: Boolean get() = changed || mode == Mode.Voice
    }

    sealed interface Event {
        /** Se guardó: abrir "Movimiento registrado correctamente". */
        data class Saved(val movement: Movement) : Event
    }

    private val _state = MutableStateFlow(NewMovementUi())
    val state: StateFlow<NewMovementUi> = _state.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private var loadJob: Job? = null

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _state.value = _state.value.copy(error = null) }

    /**
     * "Registro manual": formulario vacío con la fecha de hoy. Desde el detalle de un
     * presupuesto llega [categoryId] para que el gasto ya tenga esa categoría.
     */
    fun startManual(categoryId: String? = null) {
        _state.value = NewMovementUi(form = Form(date = today(), categoryId = categoryId))
        _event.value = null
        load(voiceText = null)
    }

    /** "Registro por voz": Dai convierte lo que se dijo en un movimiento para confirmar. */
    fun startVoice(text: String) {
        _state.value = NewMovementUi(mode = Mode.Voice, form = Form(date = today()), heard = text.trim())
        _event.value = null
        load(voiceText = text)
    }

    /** Trae las categorías y, si vino por voz, llena el formulario con lo que se dijo. */
    private fun load(voiceText: String?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val categories = try {
                repository.categories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(error = ErrorType.NoNetwork)
                emptyList()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = ErrorType.Unknown)
                emptyList()
            }
            var next = _state.value.copy(categories = categories)
            if (voiceText != null) {
                val heard = VoiceMovementParser.parse(voiceText, today(), categories)
                next = next.copy(
                    form = Form(
                        kind = heard.kind,
                        amountDigits = heard.amount?.takeIf { it > 0 }?.roundToLong()?.toString().orEmpty(),
                        date = heard.date,
                        categoryId = heard.categoryId,
                        reason = heard.reason,
                    )
                )
            }
            _state.value = next
        }
    }

    /** Gasto / Ingreso. Si cambia el tipo, la categoría elegida ya no sirve y se borra. */
    fun setKind(kind: MovementKind) {
        val form = _state.value.form
        if (form.kind == kind) return
        edit(form.copy(kind = kind, categoryId = null))
    }

    fun setAmount(digits: String) = edit(_state.value.form.copy(amountDigits = digits.filter(Char::isDigit).take(AMOUNT_MAX_DIGITS)))
    fun setDate(date: LocalDate) = edit(_state.value.form.copy(date = date))
    fun setCategory(id: String?) = edit(_state.value.form.copy(categoryId = id))
    fun setReason(reason: String) = edit(_state.value.form.copy(reason = reason.take(NewMovement.NOTE_MAX)))

    private fun edit(form: Form) {
        _state.value = _state.value.copy(form = form, changed = true, problem = null)
    }

    /** Guardar: valida y hace el insert en `movements`. */
    fun save() {
        val current = _state.value
        if (current.saving) return
        val problem = validate(current.form, today())
        if (problem != null) {
            _state.value = current.copy(problem = problem)
            return
        }
        val form = current.form
        val category = current.selectedCategory
        val movement = NewMovement(
            kind = form.kind,
            // Lo que se ve en la lista: el motivo; si no hay, el nombre de la categoría.
            title = form.reason.trim().ifBlank { category?.name ?: defaultTitle(form.kind) },
            amount = form.amountDigits.toDouble(),
            date = form.date ?: today(),
            categoryId = form.categoryId,
            note = form.reason,
            source = if (current.mode == Mode.Voice) NewMovement.SOURCE_DAI else NewMovement.SOURCE_MANUAL,
        )
        _state.value = current.copy(saving = true, problem = null, error = null)
        viewModelScope.launch {
            try {
                val saved = repository.create(movement, zone())
                _state.value = _state.value.copy(saving = false, changed = false)
                _event.value = Event.Saved(saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = _state.value.copy(saving = false, error = ErrorType.NoNetwork)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = ErrorType.Unknown)
            }
        }
    }

    companion object {
        const val AMOUNT_MAX_DIGITS = 12

        /** Reglas del formulario: monto mayor que 0, una categoría y no una fecha futura. */
        fun validate(form: Form, today: LocalDate): Problem? = when {
            (form.amountDigits.toDoubleOrNull() ?: 0.0) <= 0.0 -> Problem.Amount
            form.date != null && form.date.isAfter(today) -> Problem.FutureDate
            form.categoryId == null -> Problem.Category
            else -> null
        }

        private fun defaultTitle(kind: MovementKind) = if (kind == MovementKind.Income) "Ingreso" else "Gasto"
    }
}
