package com.pocketpal.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.DayGroup
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
import com.pocketpal.app.data.movement.PeriodSummary
import com.pocketpal.app.data.movement.QuickFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Flujo 1 · Lista principal de movimientos (Figma "01_lista_principal").
 *
 * - Arriba, siempre del **mes actual**: "Total del mes" (ingresos - gastos), la variación
 *   frente al mes anterior y el "Resumen del mes" (ingresos y gastos).
 * - Abajo, la lista del **período elegido** (Hoy / 7 días / Este mes, o un rango del
 *   calendario), agrupada por día.
 * - También alimenta "Movimientos recientes" del inicio ([home]).
 *
 * [today] y [zone] se reciben como funciones para poder fijarlas en las pruebas.
 */
class MovementsViewModel(
    private val repository: MovementRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    /** Período de la lista: un filtro rápido o un rango elegido en el calendario. */
    sealed interface Period {
        data class Quick(val filter: QuickFilter) : Period
        data class Custom(val range: DateRange) : Period
    }

    /** Todo lo que necesita la pantalla. */
    data class MovementsUi(
        val period: Period,
        val range: DateRange,
        val monthSummary: PeriodSummary,
        /** % del balance frente al mes anterior; `null` si no hay con qué comparar. */
        val monthChange: Int?,
        val groups: List<DayGroup>,
        val categories: Map<String, MovementCategory>,
        /** `true` si no hay ningún movimiento en todo el mes (pantalla vacía completa). */
        val monthIsEmpty: Boolean,
    ) {
        val isEmpty: Boolean get() = groups.isEmpty()
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: MovementsUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** "Movimientos recientes" del inicio. */
    data class HomeMovements(
        val movements: List<Movement> = emptyList(),
        val categories: Map<String, MovementCategory> = emptyMap(),
    )

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Errores de un momento (por ejemplo, un rango inválido); la pantalla los muestra y los consume. */
    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _home = MutableStateFlow(HomeMovements())
    val home: StateFlow<HomeMovements> = _home.asStateFlow()

    /** Período elegido. Arranca en "Este mes". */
    private var period: Period = Period.Quick(QuickFilter.ThisMonth)

    fun consumeError() { _error.value = null }

    /** Toca un filtro rápido ("Hoy", "7 días", "Este mes"). */
    fun selectFilter(filter: QuickFilter) {
        period = Period.Quick(filter)
        refresh()
    }

    /**
     * Aplica un rango del calendario (lo llama el flujo 3). Si el rango es inválido no
     * cambia nada y avisa con [error].
     */
    fun selectRange(range: DateRange) {
        val problem = MovementRules.validateRange(range)
        if (problem != null) {
            _error.value = problem
            return
        }
        period = Period.Custom(range)
        refresh()
    }

    /** Rango de días del período actual. */
    fun currentRange(): DateRange = rangeOf(period)

    /**
     * Carga el mes actual, el mes anterior (para comparar) y, si el período no cabe
     * dentro del mes, también el período. Si ya había contenido no vuelve a "cargando".
     */
    fun refresh() {
        viewModelScope.launch {
            if (_state.value !is UiState.Content) _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(load())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    /** Recarga los últimos movimientos del inicio. Si falla, el inicio simplemente no los muestra. */
    fun refreshRecent() {
        viewModelScope.launch {
            try {
                val categories = repository.categories().associateBy { it.id }
                _home.value = HomeMovements(repository.recent(MovementRepository.RECENT_LIMIT), categories)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _home.value = HomeMovements()
            }
        }
    }

    private suspend fun load(): MovementsUi {
        val zone = zone()
        val today = today()
        val month = MovementRules.monthRange(YearMonth.from(today))
        val previous = MovementRules.previousMonthRange(today)

        val categories = repository.categories().associateBy { it.id }
        val monthList = fetch(month, zone)
        val previousList = fetch(previous, zone)

        // Si el período cabe dentro del mes ("Hoy", "7 días" a mitad de mes…), se filtra
        // lo que ya se trajo en vez de volver a consultar la base.
        val range = rangeOf(period)
        val periodList = if (range.start in month && range.end in month) {
            monthList.filter { MovementRules.isInRange(it, range, zone) }
        } else {
            fetch(range, zone)
        }

        val summary = MovementRules.summary(monthList)
        val previousSummary = MovementRules.summary(previousList)
        return MovementsUi(
            period = period,
            range = range,
            monthSummary = summary,
            monthChange = MovementRules.percentChange(summary.balance, previousSummary.balance),
            groups = MovementRules.groupByDay(periodList, zone),
            categories = categories,
            monthIsEmpty = monthList.isEmpty() && periodList.isEmpty(),
        )
    }

    private suspend fun fetch(range: DateRange, zone: ZoneId): List<Movement> {
        val (from, to) = MovementRules.toInstants(range, zone)
        return repository.list(from, to)
    }

    private fun rangeOf(period: Period): DateRange = when (period) {
        is Period.Quick -> MovementRules.rangeFor(period.filter, today())
        is Period.Custom -> period.range
    }
}
