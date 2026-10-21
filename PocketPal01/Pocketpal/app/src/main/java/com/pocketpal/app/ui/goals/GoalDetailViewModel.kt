package com.pocketpal.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalContribution
import com.pocketpal.app.data.goal.GoalRepository
import com.pocketpal.app.data.goal.GoalRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

/**
 * Flujo 3 · Detalle de la meta y aportes.
 *
 * Carga la meta con su historial de aportes, valida y guarda un aporte nuevo (RN-M6) y,
 * si con ese aporte la meta llega al 100 %, deja lista la celebración "¡Lo lograste!".
 * Lo ahorrado y la fecha de cumplimiento los recalcula la base (triggers
 * `recalc_goal_saved` y `set_goal_completed`); aquí solo se vuelve a leer la meta.
 *
 * [today] se recibe como función para poder fijar la fecha en las pruebas.
 */
class GoalDetailViewModel(
    private val repository: GoalRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** La meta abierta y sus aportes (el más reciente primero). */
    data class Detail(val goal: Goal, val contributions: List<GoalContribution>)

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: Detail) : UiState
        /** La meta ya no existe (por ejemplo, se borró desde otro teléfono). */
        data object NotFound : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** Eventos de una sola vez que la pantalla consume. */
    sealed interface Event {
        /** El aporte se guardó; [completedNow] = con este aporte la meta llegó al 100 %. */
        data class ContributionAdded(val completedNow: Boolean) : Event
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    /** Meta recién cumplida: mientras no sea `null` se muestra "¡Lo lograste! 🎉". */
    private val _celebration = MutableStateFlow<Goal?>(null)
    val celebration: StateFlow<Goal?> = _celebration.asStateFlow()

    private var goalId: String? = null

    fun consumeError() { _error.value = null }
    fun consumeEvent() { _event.value = null }
    fun dismissCelebration() { _celebration.value = null }

    /** Abre la meta [id]. Si es otra meta distinta a la anterior, muestra "cargando". */
    fun load(id: String) {
        if (id != goalId) {
            goalId = id
            _state.value = UiState.Loading
            _celebration.value = null
        }
        refresh()
    }

    /** Vuelve a leer la meta y su historial desde la base. */
    fun refresh() {
        val id = goalId ?: return
        viewModelScope.launch {
            try {
                val goal = repository.get(id)
                _state.value = if (goal == null) {
                    UiState.NotFound
                } else {
                    UiState.Content(Detail(goal, repository.contributions(id)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showFailure(ErrorType.NoNetwork)
            } catch (e: Exception) {
                showFailure(ErrorType.Unknown)
            }
        }
    }

    /**
     * Valida y guarda un aporte. Se permite aportar de más (pasar del 100 %) y aportar a
     * una meta ya cumplida; la celebración solo sale la primera vez que llega al 100 %.
     */
    fun addContribution(amount: Double, date: LocalDate, note: String?) {
        val current = (_state.value as? UiState.Content)?.data?.goal ?: return
        val problem = GoalRules.validateContribution(amount, date, today(), note)
        if (problem != null) {
            _error.value = problem
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
                val updated = repository.addContribution(current.id, amount, date, cleanNote)
                val completedNow = !current.isCompleted && updated.isCompleted
                _state.value = UiState.Content(Detail(updated, repository.contributions(current.id)))
                if (completedNow) _celebration.value = updated
                _event.value = Event.ContributionAdded(completedNow)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    /** Si ya se veía la meta, se conserva y solo se avisa del error; si no, pantalla de error. */
    private fun showFailure(type: ErrorType) {
        if (_state.value is UiState.Content) _error.value = type else _state.value = UiState.Error(type)
    }
}
