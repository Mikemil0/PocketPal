package com.pocketpal.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Flujo 3 · Filtro por fechas con calendario (Figma "02_calendario_filtro 1").
 *
 * - El calendario muestra un mes; los días con movimientos llevan un puntico.
 * - Primer toque = inicio del rango, segundo toque = fin (si se toca un día anterior al
 *   inicio, ese pasa a ser el nuevo inicio). Un tercer toque empieza otro rango.
 * - "Últimos 30 días" elige de una vez hoy y los 29 días anteriores.
 * - No se pueden elegir días futuros (todavía no hay movimientos ahí).
 * - "Ver movimientos" entrega el rango a la lista principal ([confirm]).
 */
class MovementCalendarViewModel(
    private val repository: MovementRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    enum class Mode { Last30Days, Custom }

    data class CalendarUi(
        val month: YearMonth,
        val start: LocalDate? = null,
        val end: LocalDate? = null,
        val mode: Mode = Mode.Custom,
        val daysWithMovements: Set<LocalDate> = emptySet(),
        val error: ErrorType? = null,
    ) {
        /** El rango que se aplicaría ahora (un solo día si aún no se eligió el fin). */
        val selection: DateRange? get() = start?.let { DateRange(it, end ?: it) }

        fun isSelected(date: LocalDate): Boolean = selection?.let { date in it } ?: false
        fun isEdge(date: LocalDate): Boolean = date == start || date == end
    }

    private val _state = MutableStateFlow(CalendarUi(YearMonth.from(today())))
    val state: StateFlow<CalendarUi> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Al abrir: arranca con el rango que ya tenía la lista y en el mes donde termina. */
    fun open(current: DateRange) {
        val end = minOf(current.end, today())
        _state.value = CalendarUi(
            month = YearMonth.from(end),
            start = current.start,
            end = end,
            mode = Mode.Custom,
        )
        loadDays()
    }

    fun previousMonth() = changeMonth(_state.value.month.minusMonths(1))

    /** No se avanza más allá del mes actual. */
    fun nextMonth() {
        val next = _state.value.month.plusMonths(1)
        if (!next.isAfter(YearMonth.from(today()))) changeMonth(next)
    }

    /** Toque en un día del calendario. */
    fun tapDay(date: LocalDate) {
        if (date.isAfter(today())) return
        val s = _state.value
        _state.value = when {
            s.start == null || s.end != null -> s.copy(start = date, end = null, mode = Mode.Custom, error = null)
            date.isBefore(s.start) -> s.copy(start = date, mode = Mode.Custom, error = null)
            else -> s.copy(end = date, mode = Mode.Custom, error = null)
        }
    }

    /** Botón "Últimos 30 días". */
    fun selectLast30Days() {
        val range = MovementRules.lastDays(30, today())
        val month = YearMonth.from(range.end)
        val monthChanged = month != _state.value.month
        _state.value = _state.value.copy(start = range.start, end = range.end, mode = Mode.Last30Days, month = month, error = null)
        if (monthChanged) loadDays()
    }

    /** Botón "Personalizado": deja el rango y vuelve al modo de elegir días. */
    fun selectCustom() {
        _state.value = _state.value.copy(mode = Mode.Custom)
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }

    /** "Ver movimientos": devuelve el rango válido, o `null` y deja el error en la pantalla. */
    fun confirm(): DateRange? {
        val range = _state.value.selection ?: return null
        val problem = MovementRules.validateRange(range)
        if (problem != null) {
            _state.value = _state.value.copy(error = problem)
            return null
        }
        return range
    }

    companion object {
        /**
         * Celdas del mes para dibujar el calendario en filas de 7: primero `null` (huecos
         * antes del día 1) y luego cada día. [weekStart] es la preferencia "Inicio de
         * semana" del Módulo 2: 0 = domingo, 1 = lunes, … 6 = sábado.
         */
        fun monthGrid(month: YearMonth, weekStart: Int): List<LocalDate?> {
            val first = month.atDay(1)
            val firstIndex = first.dayOfWeek.value % 7 // domingo = 0, lunes = 1, …
            val blanks = (firstIndex - weekStart.mod(7) + 7) % 7
            return List(blanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        }

        /** Orden de las iniciales de la semana (0 = domingo) empezando por [weekStart]. */
        fun weekOrder(weekStart: Int): List<Int> = (0 until 7).map { (weekStart.mod(7) + it) % 7 }
    }

    private fun changeMonth(month: YearMonth) {
        _state.value = _state.value.copy(month = month, daysWithMovements = emptySet())
        loadDays()
    }

    /** Trae los movimientos del mes visible solo para marcar los días con puntico. */
    private fun loadDays() {
        loadJob?.cancel()
        val month = _state.value.month
        loadJob = viewModelScope.launch {
            try {
                val (from, to) = MovementRules.toInstants(MovementRules.monthRange(month), zone())
                val days = MovementRules.daysWithMovements(repository.list(from, to), zone())
                if (_state.value.month == month) _state.value = _state.value.copy(daysWithMovements = days)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin los puntos el calendario igual sirve para elegir el rango.
            }
        }
    }
}
