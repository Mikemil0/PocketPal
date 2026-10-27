package com.pocketpal.app.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.CategoryTotal
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
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
 * Flujo 3 · Movimientos por categorías (Figma "01_Movimientos por categorias 1").
 *
 * Tarjeta "Gastos este mes" con la variación frente al mes pasado y, debajo, cada
 * categoría de gasto con su total y una barra (la más alta va llena). Los gastos sin
 * categoría se agrupan en "Otros".
 */
class MovementCategoriesViewModel(
    private val repository: MovementRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    data class CategoriesUi(
        val monthExpense: Double,
        /** % de los gastos frente al mes pasado (negativo = gastaste menos). */
        val change: Int?,
        val totals: List<CategoryTotal>,
        val categories: Map<String, MovementCategory>,
    )

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: CategoriesUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

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

    private suspend fun load(): CategoriesUi {
        val today = today()
        val month = fetch(MovementRules.monthRange(YearMonth.from(today)))
        val previous = fetch(MovementRules.previousMonthRange(today))
        val expense = MovementRules.summary(month).expense
        val previousExpense = MovementRules.summary(previous).expense
        return CategoriesUi(
            monthExpense = expense,
            change = MovementRules.percentChange(expense, previousExpense),
            totals = MovementRules.categoryTotals(month),
            categories = repository.categories().associateBy { it.id },
        )
    }

    private suspend fun fetch(range: DateRange) =
        MovementRules.toInstants(range, zone()).let { (from, to) -> repository.list(from, to) }
}
