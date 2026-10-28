package com.pocketpal.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.BudgetRepository
import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.budget.BudgetStatus
import com.pocketpal.app.data.budget.BudgetsSummary
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 1 · Lista de presupuestos (Figma "PRESUPUESTOS LISTA") y tarjeta "Presupuestos"
 * del inicio ([home]).
 *
 * 1. Trae los presupuestos y las categorías de gastos.
 * 2. Trae con UNA consulta los gastos del período más amplio (mes y/o semana actual).
 * 3. Calcula lo gastado de cada uno con [BudgetRules] (la base no guarda lo gastado).
 *
 * [weekStart] es "Inicio de semana" de Preferencias (Módulo 2); [today] y [zone] se
 * reciben como funciones para poder fijarlos en las pruebas.
 */
class BudgetsViewModel(
    private val repository: BudgetRepository,
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    data class BudgetsUi(
        val statuses: List<BudgetStatus>,
        val summary: BudgetsSummary,
        /** Categorías de gastos que aún no tienen presupuesto. */
        val freeCategories: Int,
    ) {
        val isEmpty: Boolean get() = statuses.isEmpty()
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: BudgetsUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /**
     * Tarjeta del inicio. Variantes del Figma:
     * - sin presupuestos → "Aún no tienes presupuestos hechos. ¡Empieza ya!"
     * - uno → la tarjeta + "¿Necesitas presupuestar otra categoría? ¡Hazlo ya!"
     * - dos o más → las dos primeras tarjetas.
     */
    data class HomeBudgets(
        val statuses: List<BudgetStatus> = emptyList(),
        val canAddMore: Boolean = false,
        val loaded: Boolean = false,
    )

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _home = MutableStateFlow(HomeBudgets())
    val home: StateFlow<HomeBudgets> = _home.asStateFlow()

    /** Recarga la lista y la tarjeta del inicio (al abrir cualquiera de las dos y al volver de crear/editar). */
    fun refresh() {
        // Si ya hay datos se dejan en pantalla mientras llegan los nuevos (no parpadea).
        if (_state.value !is UiState.Content) _state.value = UiState.Loading
        viewModelScope.launch {
            try {
                val ui = load()
                _state.value = UiState.Content(ui)
                _home.value = HomeBudgets(
                    statuses = ui.statuses.take(HOME_CARDS),
                    canAddMore = ui.freeCategories > 0,
                    loaded = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                _state.value = UiState.Error(ErrorType.Unknown)
            }
        }
    }

    private suspend fun load(): BudgetsUi {
        val day = today()
        val tz = zone()
        val week = weekStart()
        val budgets = repository.list()
        val categories = repository.categories()
        val range = BudgetRules.loadRange(budgets, day, week)
        val movements: List<Movement> = if (range == null) {
            emptyList()
        } else {
            val (from, to) = MovementRules.toInstants(range, tz)
            repository.expenses(from, to)
        }
        val statuses = BudgetRules.statuses(budgets, categories, movements, day, week, tz)
        return BudgetsUi(
            statuses = statuses,
            summary = BudgetRules.summary(statuses),
            freeCategories = BudgetRules.availableCategories(categories, budgets).size,
        )
    }

    companion object {
        /** Tarjetas que caben en el inicio. */
        const val HOME_CARDS = 2
    }
}
