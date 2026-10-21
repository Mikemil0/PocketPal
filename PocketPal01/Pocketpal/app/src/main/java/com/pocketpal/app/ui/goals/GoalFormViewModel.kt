package com.pocketpal.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.DuplicateGoalNameException
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalDraft
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
 * Flujo 2 · Crear meta: valida el formulario (RN-M1 a RN-M4), guarda la meta (y su
 * ahorro inicial) y deja lista la meta creada para la pantalla "Meta creada".
 *
 * Flujo 4 · Editar y eliminar: el mismo formulario se abre con los datos de la meta
 * ([prepareEdit]); al editar no se puede bajar el monto por debajo de lo ya ahorrado
 * (RN-M5) y eliminar borra también sus aportes (ON DELETE CASCADE en la base).
 *
 * [today] se recibe como función para poder fijar la fecha en las pruebas.
 */
class GoalFormViewModel(
    private val repository: GoalRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Eventos de una sola vez que la pantalla consume. */
    sealed interface Event {
        data class Created(val goal: Goal) : Event
        data class Updated(val goal: Goal) : Event
        data object Deleted : Event
    }

    /** Metas existentes, para avisar en vivo si el nombre ya está usado (RN-M1). */
    private val _existing = MutableStateFlow<List<Goal>>(emptyList())
    val existing: StateFlow<List<Goal>> = _existing.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    /** Última meta creada (la muestra la pantalla "Meta creada"). */
    private val _created = MutableStateFlow<Goal?>(null)
    val created: StateFlow<Goal?> = _created.asStateFlow()

    /** Meta que se está editando (flujo 4); `null` = se está creando una nueva. */
    private val _editing = MutableStateFlow<Goal?>(null)
    val editing: StateFlow<Goal?> = _editing.asStateFlow()

    /** Cuántos aportes tiene la meta en edición (para el aviso de "Eliminar meta"). */
    private val _contributionsCount = MutableStateFlow(0)
    val contributionsCount: StateFlow<Int> = _contributionsCount.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    fun consumeError() { _error.value = null }
    fun consumeEvent() { _event.value = null }

    /** Se llama al abrir "Crear nueva meta": limpia el estado y trae las metas existentes. */
    fun prepareNew() {
        _error.value = null
        _event.value = null
        _editing.value = null
        _contributionsCount.value = 0
        loadExisting()
    }

    /**
     * Flujo 4 · Se llama al tocar el lápiz ✏️: trae la meta [id] y sus aportes para
     * prellenar el formulario "Editar meta".
     */
    fun prepareEdit(id: String) {
        _error.value = null
        _event.value = null
        if (_editing.value?.id != id) _editing.value = null
        loadExisting()
        viewModelScope.launch {
            try {
                val goal = repository.get(id)
                if (goal == null) {
                    _error.value = ErrorType.Unknown
                    return@launch
                }
                _contributionsCount.value = repository.contributions(id).size
                _editing.value = goal
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            }
        }
    }

    /**
     * ¿El nombre ya lo usa otra meta? (para el aviso en vivo bajo el campo NOMBRE).
     * Al editar, la meta en edición no cuenta como repetida de sí misma.
     */
    fun isDuplicateName(name: String, excludeId: String? = _editing.value?.id): Boolean =
        name.isNotBlank() && GoalRules.isDuplicateName(name, _existing.value, excludeId)

    /**
     * Valida y crea la meta. La posición es la última (+1) para que aparezca al final de
     * "Activas". Si la base rechaza el nombre por duplicado, se muestra el mismo error.
     */
    fun create(draft: GoalDraft) {
        val clean = draft.copy(name = draft.name.trim())
        val problem = GoalRules.validateGoal(clean, _existing.value, today())
        if (problem != null) {
            _error.value = problem
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val position = (_existing.value.maxOfOrNull { it.position } ?: -1) + 1
                val goal = repository.create(clean, position)
                _existing.value = _existing.value + goal
                _created.value = goal
                _event.value = Event.Created(goal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateGoalNameException) {
                _error.value = ErrorType.DuplicateGoalName
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Flujo 4 · Guarda los cambios de la meta en edición. Lo ahorrado no se toca aquí
     * (solo cambia con aportes), por eso "Ya tengo ahorrado" no aparece al editar.
     */
    fun update(draft: GoalDraft) {
        val goal = _editing.value ?: return
        val clean = draft.copy(name = draft.name.trim(), initialSaved = 0.0)
        val problem = GoalRules.validateGoal(
            clean, _existing.value, today(),
            excludeId = goal.id,
            currentSaved = goal.savedAmount,
        )
        if (problem != null) {
            _error.value = problem
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val updated = repository.update(goal.id, clean)
                _existing.value = _existing.value.map { if (it.id == updated.id) updated else it }
                _editing.value = updated
                _event.value = Event.Updated(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateGoalNameException) {
                _error.value = ErrorType.DuplicateGoalName
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    /** Flujo 4 · Elimina la meta en edición (y, en cascada, todos sus aportes). */
    fun delete() {
        val goal = _editing.value ?: return
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.delete(goal.id)
                _existing.value = _existing.value.filterNot { it.id == goal.id }
                _editing.value = null
                _event.value = Event.Deleted
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _deleting.value = false
            }
        }
    }

    private fun loadExisting() {
        viewModelScope.launch {
            try {
                _existing.value = repository.list()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin la lista igual se puede guardar: la base es la autoridad del nombre único.
                _existing.value = emptyList()
            }
        }
    }
}
