package com.pocketpal.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.Budget
import com.pocketpal.app.data.budget.BudgetDraft
import com.pocketpal.app.data.budget.BudgetRepository
import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.budget.BudgetStatus
import com.pocketpal.app.data.budget.DuplicateBudgetException
import com.pocketpal.app.data.movement.MovementCategory
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
 * Flujo 2 · "Nuevo presupuesto", "Elegir categoría" y "Presupuesto creado".
 *
 * - Guarda la **categoría elegida** aquí (y no en la pantalla) porque se escoge en otra
 *   pantalla ("Elegir categoría") y el formulario la necesita al volver.
 * - Valida con [BudgetRules.validate] antes de ir a la base; la base es la autoridad del
 *   "un presupuesto por categoría" (índice único) y su rechazo se traduce al mismo error.
 *
 * Flujo 4 · El mismo formulario sirve para "Editar presupuesto" ([prepareEdit], [update])
 * y desde allí se elimina ([delete]). Al editar, la categoría propia sigue disponible y el
 * presupuesto no choca consigo mismo.
 */
class BudgetFormViewModel(
    private val repository: BudgetRepository,
    private val weekStart: () -> Int = { 1 },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    /** Eventos de una sola vez que la pantalla consume. */
    sealed interface Event {
        data class Created(val budget: Budget) : Event
        data class Updated(val budget: Budget) : Event
        data object Deleted : Event
    }

    /** Categorías de gastos del usuario (Módulo 2). */
    private val _categories = MutableStateFlow<List<MovementCategory>>(emptyList())
    val categories: StateFlow<List<MovementCategory>> = _categories.asStateFlow()

    /** Presupuestos que ya existen (para saber qué categorías están ocupadas). */
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    /** Lo gastado este mes en cada categoría ("$0 de $500.000" en "Elegir categoría"). */
    private val _monthSpent = MutableStateFlow<Map<String, Double>>(emptyMap())
    val monthSpent: StateFlow<Map<String, Double>> = _monthSpent.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    /** El presupuesto recién creado con lo ya gastado (pantalla "Presupuesto creado"). */
    private val _created = MutableStateFlow<BudgetStatus?>(null)
    val created: StateFlow<BudgetStatus?> = _created.asStateFlow()

    /** Presupuesto que se está editando (flujo 4); `null` = se está creando uno nuevo. */
    private val _editing = MutableStateFlow<Budget?>(null)
    val editing: StateFlow<Budget?> = _editing.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    fun consumeError() { _error.value = null }
    fun consumeEvent() { _event.value = null }

    /**
     * Se llama al abrir "Nuevo presupuesto": limpia el formulario, trae categorías y
     * presupuestos, y deja elegida la primera categoría libre.
     */
    fun prepareNew() {
        _error.value = null
        _event.value = null
        _selectedCategoryId.value = null
        _editing.value = null
        load(keepCategoryId = null) { available ->
            _selectedCategoryId.value = available.firstOrNull()?.id
        }
    }

    /**
     * Flujo 4 · Se llama al abrir "Editar presupuesto": trae el presupuesto [id] para
     * prellenar el formulario y deja elegida su categoría.
     */
    fun prepareEdit(id: String) {
        _error.value = null
        _event.value = null
        if (_editing.value?.id != id) _editing.value = null
        viewModelScope.launch {
            try {
                val budget = repository.get(id)
                if (budget == null) {
                    _error.value = ErrorType.Unknown
                    return@launch
                }
                _selectedCategoryId.value = budget.categoryId
                _editing.value = budget
                load(keepCategoryId = budget.categoryId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            }
        }
    }

    /** Categorías que se pueden elegir (sin presupuesto). Vacía = "Ya tienes presupuesto en todas tus categorías". */
    fun availableCategories(): List<MovementCategory> =
        BudgetRules.availableCategories(_categories.value, _budgets.value, keepCategoryId = _editing.value?.categoryId)

    fun selectedCategory(): MovementCategory? =
        _categories.value.firstOrNull { it.id == _selectedCategoryId.value }

    /** "Guardar" en "Elegir categoría". */
    fun selectCategory(id: String) {
        _selectedCategoryId.value = id
        _error.value = null
    }

    /** ¿La categoría elegida ya tiene otro presupuesto? (aviso en vivo bajo NOMBRE). */
    fun isDuplicate(): Boolean =
        BudgetRules.isCategoryTaken(_selectedCategoryId.value, _budgets.value, excludeBudgetId = _editing.value?.id)

    /** Valida y crea el presupuesto con la categoría elegida. */
    fun create(draft: BudgetDraft) {
        val clean = draft.copy(categoryId = _selectedCategoryId.value, name = draft.name.trim())
        val problem = BudgetRules.validate(clean, _budgets.value)
        if (problem != null) {
            _error.value = problem
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val budget = repository.create(clean)
                _budgets.value = _budgets.value + budget
                _created.value = statusOf(budget)
                _event.value = Event.Created(budget)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateBudgetException) {
                onDuplicate()
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    /** Flujo 4 · Guarda los cambios del presupuesto en edición (categoría, nombre, límite, período y aviso). */
    fun update(draft: BudgetDraft) {
        val budget = _editing.value ?: return
        val clean = draft.copy(categoryId = _selectedCategoryId.value, name = draft.name.trim())
        val problem = BudgetRules.validate(clean, _budgets.value, excludeBudgetId = budget.id)
        if (problem != null) {
            _error.value = problem
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val updated = repository.update(budget.id, clean)
                _budgets.value = _budgets.value.map { if (it.id == updated.id) updated else it }
                _editing.value = updated
                _event.value = Event.Updated(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateBudgetException) {
                onDuplicate()
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
     * Flujo 4 · Elimina el presupuesto en edición. Solo se borra la fila de `budgets`: los
     * gastos de `movements` no se tocan ("Los gastos ya registrados no se verán afectados").
     */
    fun delete() {
        val budget = _editing.value ?: return
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.delete(budget.id)
                _budgets.value = _budgets.value.filterNot { it.id == budget.id }
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

    // ---------------- Apoyo ----------------

    /**
     * La base dijo que esa categoría ya tiene presupuesto (por ejemplo, se creó desde otro
     * teléfono): se recargan los presupuestos para que el aviso quede en pantalla.
     */
    private suspend fun onDuplicate() {
        _error.value = ErrorType.DuplicateBudget
        try {
            _budgets.value = repository.list()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // El aviso ya está puesto; la lista se actualiza la próxima vez.
        }
    }

    /** Lo gastado en el período actual del presupuesto (para la tarjeta de "Presupuesto creado"). */
    private suspend fun statusOf(budget: Budget): BudgetStatus {
        val category = _categories.value.firstOrNull { it.id == budget.categoryId }
        val range = BudgetRules.periodRange(budget.budgetPeriod, today(), weekStart())
        val (from, to) = MovementRules.toInstants(range, zone())
        val movements = try {
            repository.expenses(from, to, budget.categoryId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        return BudgetRules.status(budget, category, movements, today(), weekStart(), zone())
    }

    /** Trae categorías, presupuestos y lo gastado del mes; luego llama [then] con las libres. */
    private fun load(keepCategoryId: String?, then: (List<MovementCategory>) -> Unit = {}) {
        _loaded.value = false
        viewModelScope.launch {
            try {
                _categories.value = repository.categories()
                _budgets.value = repository.list()
                val (from, to) = MovementRules.toInstants(MovementRules.monthRange(YearMonth.from(today())), zone())
                _monthSpent.value = repository.expenses(from, to)
                    .filter { it.categoryId != null }
                    .groupBy { it.categoryId!! }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                then(BudgetRules.availableCategories(_categories.value, _budgets.value, keepCategoryId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _loaded.value = true
            }
        }
    }
}
