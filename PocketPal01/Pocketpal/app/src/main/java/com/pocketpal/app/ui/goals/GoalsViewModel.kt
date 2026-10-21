package com.pocketpal.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalRepository
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.data.goal.GoalSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Flujo 1 · Ver metas: lista con pestañas Activas/Completadas, total ahorrado, filtro
 * (orden) y reordenar arrastrando. También alimenta la sección "Metas de ahorro" del Home.
 *
 * Depende de [GoalRepository] (interfaz), no de Supabase (RNF-02).
 */
class GoalsViewModel(private val repository: GoalRepository) : ViewModel() {

    enum class Tab { Active, Completed }

    /** Todo lo que necesita la pantalla de la lista. */
    data class GoalsUi(
        val goals: List<Goal> = emptyList(),
        val tab: Tab = Tab.Active,
        val sort: GoalSort = GoalSort.Manual,
    ) {
        /** Metas activas en el orden elegido. */
        val active: List<Goal> get() = GoalRules.sort(goals.filter { !it.isCompleted }, sort)

        /** Metas completadas en el orden elegido. */
        val completed: List<Goal> get() = GoalRules.sort(goals.filter { it.isCompleted }, sort)

        /** Lista de la pestaña visible. */
        val current: List<Goal> get() = if (tab == Tab.Active) active else completed

        /** "Total ahorrado en todas tus metas" (activas y completadas). */
        val totalSaved: Double get() = goals.sumOf { it.savedAmount }

        /** Solo se reordena arrastrando en "Activas" y con el orden manual. */
        val canReorder: Boolean get() = tab == Tab.Active && sort == GoalSort.Manual
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: GoalsUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private var reorderJob: Job? = null

    init { refresh() }

    fun consumeError() { _error.value = null }

    /** Recarga las metas. Si ya hay contenido no vuelve a "cargando" (evita parpadeos). */
    fun refresh() {
        viewModelScope.launch {
            val previous = (_state.value as? UiState.Content)?.data ?: GoalsUi()
            if (_state.value !is UiState.Content) _state.value = UiState.Loading
            _state.value = try {
                UiState.Content(previous.copy(goals = repository.list()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    fun selectTab(tab: Tab) = update { it.copy(tab = tab) }

    fun selectSort(sort: GoalSort) = update { it.copy(sort = sort) }

    /**
     * Reordenar arrastrando (como en categorías): el cambio se ve de inmediato y el nuevo
     * orden se guarda en la base un momento después de soltar.
     */
    fun move(fromId: String, toId: String) {
        val data = (_state.value as? UiState.Content)?.data ?: return
        if (!data.canReorder) return
        val list = data.active.toMutableList()
        val fromIndex = list.indexOfFirst { it.id == fromId }
        val toIndex = list.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return
        list.add(toIndex, list.removeAt(fromIndex))

        // Las activas toman las posiciones 0..n-1; las completadas quedan después.
        val newPositions = list.mapIndexed { index, goal -> goal.id to index }.toMap()
        val updated = data.goals.map { goal ->
            newPositions[goal.id]?.let { goal.copy(position = it) } ?: goal
        }
        update { it.copy(goals = updated) }

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(REORDER_PERSIST_DELAY_MS)
            try {
                repository.reorder(list.map { it.id })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = if (e is IOException) ErrorType.NoNetwork else ErrorType.Unknown
            }
        }
    }

    private fun update(transform: (GoalsUi) -> GoalsUi) {
        val current = _state.value as? UiState.Content ?: return
        _state.value = UiState.Content(transform(current.data))
    }

    companion object {
        const val REORDER_PERSIST_DELAY_MS = 400L
    }
}
