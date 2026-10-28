package com.pocketpal.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.Budget
import com.pocketpal.app.data.budget.BudgetDetail
import com.pocketpal.app.data.budget.BudgetRepository
import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.budget.MonthUsage
import com.pocketpal.app.data.movement.MovementCategory
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
 * Flujo 3 · Detalle del presupuesto (Figma "DETALLE PRE") e "Historial" (Figma "HISTORIAL").
 *
 * Detalle: lo gastado en el período actual, "Se reinicia en N días", lo que queda y los
 * gastos de ese período (de `movements`, solo de su categoría).
 * Historial: lo gastado en cada uno de los últimos 6 meses frente al límite.
 */
class BudgetDetailViewModel(
    private val repository: BudgetRepository,
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: BudgetDetail) : UiState
        /** El presupuesto ya no existe (se borró desde otro dispositivo, por ejemplo). */
        data object NotFound : UiState
        data class Error(val type: ErrorType) : UiState
    }

    data class HistoryUi(
        val budget: Budget,
        val category: MovementCategory?,
        /** Del mes más antiguo al actual (6 barras). */
        val months: List<MonthUsage>,
    ) {
        /** "Detalles por mes": solo los meses con gastos, el más reciente primero. */
        val monthsWithSpending: List<MonthUsage> get() = months.filter { it.spent > 0.0 }.reversed()
    }

    sealed interface HistoryState {
        data object Loading : HistoryState
        data class Content(val data: HistoryUi) : HistoryState
        data class Error(val type: ErrorType) : HistoryState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _history = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val history: StateFlow<HistoryState> = _history.asStateFlow()

    private var budgetId: String? = null

    /** Abre el presupuesto [id]. Si es otro distinto al anterior, primero muestra "cargando". */
    fun load(id: String) {
        if (id != budgetId) {
            _state.value = UiState.Loading
            _history.value = HistoryState.Loading
        }
        budgetId = id
        refresh()
    }

    fun refresh() {
        val id = budgetId ?: return
        viewModelScope.launch {
            try {
                val budget = repository.get(id)
                if (budget == null) {
                    _state.value = UiState.NotFound
                    return@launch
                }
                val category = repository.categories().firstOrNull { it.id == budget.categoryId }
                val day = today()
                val tz = zone()
                val range = BudgetRules.periodRange(budget.budgetPeriod, day, weekStart())
                val (from, to) = MovementRules.toInstants(range, tz)
                val movements = repository.expenses(from, to, budget.categoryId)
                val status = BudgetRules.status(budget, category, movements, day, weekStart(), tz)
                _state.value = UiState.Content(
                    BudgetDetail(
                        status = status,
                        movements = MovementRules.newestFirst(movements),
                        daysUntilReset = BudgetRules.daysUntilReset(budget.budgetPeriod, day, weekStart()),
                        alertReached = BudgetRules.isAlertReached(status, repository.alertThreshold()),
                    )
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

    /** "Ver historial": trae con UNA consulta los gastos de la categoría en los últimos 6 meses. */
    fun loadHistory() {
        val id = budgetId ?: return
        if (_history.value !is HistoryState.Content) _history.value = HistoryState.Loading
        viewModelScope.launch {
            try {
                val budget = repository.get(id)
                if (budget == null) {
                    _state.value = UiState.NotFound
                    return@launch
                }
                val category = repository.categories().firstOrNull { it.id == budget.categoryId }
                val day = today()
                val tz = zone()
                val (from, to) = MovementRules.toInstants(BudgetRules.historyRange(day), tz)
                val movements = repository.expenses(from, to, budget.categoryId)
                _history.value = HistoryState.Content(
                    HistoryUi(budget, category, BudgetRules.history(budget, movements, day, tz))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _history.value = HistoryState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                _history.value = HistoryState.Error(ErrorType.Unknown)
            }
        }
    }
}
