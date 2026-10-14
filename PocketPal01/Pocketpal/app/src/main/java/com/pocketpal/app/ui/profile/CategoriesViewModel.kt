package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.category.Category
import com.pocketpal.app.data.category.CategoryDraft
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.data.category.CategoryNamed
import com.pocketpal.app.data.category.CategoryRepository
import com.pocketpal.app.data.category.CategoryRules
import com.pocketpal.app.data.category.CategoryStats
import com.pocketpal.app.data.category.DuplicateCategoryNameException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Categorías de gastos e ingresos (§7.2): lista por tipo, crear, editar,
 * eliminar y reordenar (RF-B7).
 *
 * RNF-02: depende de [CategoryRepository] (interfaz), no de Supabase.
 * RNF-05: los errores se emiten como [ErrorType] y la UI los traduce (RF-A3).
 * D-11: la lista empieza **vacía** (no se siembran categorías: sus nombres no
 * se podrían traducir).
 */
class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {

    /** Fila de la lista (§8.8). Los contadores son 0 hasta que exista `movements` (§6). */
    data class CategoryUi(
        override val id: String,
        override val name: String,
        val kind: CategoryKind,
        val icon: String,
        val color: String,
        val monthlyLimit: Double?,
        val position: Int = 0,
        val movementCount: Int = 0,
        val total: Double = 0.0,
    ) : CategoryNamed

    /** Listas de ambas pestañas + la pestaña activa (§8.8: "Gastos" / "Ingresos"). */
    data class CategoryListUi(
        val kind: CategoryKind,
        val expenses: List<CategoryUi>,
        val incomes: List<CategoryUi>,
    ) {
        val current: List<CategoryUi>
            get() = if (kind == CategoryKind.Expense) expenses else incomes
    }

    /** §7.3: `UiState` con Loading, Content(data) y Error(type). */
    sealed interface UiState {
        data object Loading : UiState
        data class Content(val data: CategoryListUi) : UiState
        data class Error(val type: ErrorType) : UiState
    }

    /** Eventos de una sola vez que la UI consume (igual que `consume()` del Módulo 1). */
    enum class Event { Saved, Deleted }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _error = MutableStateFlow<ErrorType?>(null)
    val error: StateFlow<ErrorType?> = _error.asStateFlow()

    private var reorderJob: Job? = null

    init { refresh() }

    fun consumeEvent() { _event.value = null }
    fun consumeError() { _error.value = null }

    /** Pestaña activa sin volver a red (las dos listas ya están en memoria). */
    fun selectKind(kind: CategoryKind) {
        updateList { it.copy(kind = kind) }
    }

    fun refresh() {
        viewModelScope.launch {
            if (_state.value !is UiState.Content) _state.value = UiState.Loading
            _state.value = try {
                val kind = (_state.value as? UiState.Content)?.data?.kind ?: CategoryKind.Expense
                val totals = repository.getCategoryTotals()
                UiState.Content(
                    CategoryListUi(
                        kind = kind,
                        expenses = repository.list(CategoryKind.Expense).map { it.toUi(totals) },
                        incomes = repository.list(CategoryKind.Income).map { it.toUi(totals) },
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                UiState.Error(ErrorType.NoNetwork)
            } catch (e: Exception) {
                UiState.Error(ErrorType.Unknown)
            }
        }
    }

    /**
     * Crea (id = `null`) o edita la categoría. Valida RN-02/RN-03 antes de
     * escribir; la restricción única de la base es la autoridad y, si rechaza
     * la escritura, se muestra el mismo banner (§8.9).
     */
    fun save(id: String?, draft: CategoryDraft) {
        val existing = existingOf(draft.kind)
        if (CategoryRules.isDuplicateName(draft.name, existing, excludeId = id)) {
            _error.value = ErrorType.DuplicateCategoryName
            return
        }
        if (!CategoryRules.isValidName(draft.name)) {
            _error.value = if (draft.name.trim().length > CategoryRules.MAX_NAME_LENGTH) {
                ErrorType.CategoryNameTooLong
            } else {
                ErrorType.CategoryNameEmpty
            }
            return
        }
        if (!CategoryRules.isValidLimit(draft.monthlyLimit)) {
            _error.value = ErrorType.InvalidMonthlyLimit
            return
        }
        val clean = draft.copy(name = draft.name.trim())
        _saving.value = true
        viewModelScope.launch {
            try {
                if (id == null) {
                    val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
                    repository.create(clean, position)
                } else {
                    repository.update(id, clean)
                }
                _event.value = Event.Saved
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: DuplicateCategoryNameException) {
                _error.value = ErrorType.DuplicateCategoryName
            } catch (e: IOException) {
                _error.value = ErrorType.NoNetwork
            } catch (e: Exception) {
                _error.value = ErrorType.Unknown
            } finally {
                _saving.value = false
            }
        }
    }

    fun delete(id: String) {
        _saving.value = true
        viewModelScope.launch {
            try {
                repository.delete(id)
                _event.value = Event.Deleted
                reload()
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

    /**
     * Reordenar con arrastre (D-10). El cambio se aplica de inmediato en la UI y
     * se persiste `position` 0..n-1 al soltar (con un pequeño retardo para no
     * escribir en cada paso del arrastre). Los índices se resuelven por id para
     * no depender de la posición en la lista de la pantalla.
     */
    fun move(fromId: String, toId: String) {
        val content = _state.value as? UiState.Content ?: return
        val list = content.data.current.toMutableList()
        val fromIndex = list.indexOfFirst { it.id == fromId }
        val toIndex = list.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val kind = content.data.kind
        updateList { if (kind == CategoryKind.Expense) it.copy(expenses = list) else it.copy(incomes = list) }
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(REORDER_PERSIST_DELAY_MS)
            try {
                repository.reorder(kind, list.map { it.id })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = if (e is IOException) ErrorType.NoNetwork else ErrorType.Unknown
            }
        }
    }

    /** Nombre duplicado en vivo para el banner del formulario (§8.9, RN-02). */
    fun isDuplicateName(name: String, kind: CategoryKind, excludeId: String?): Boolean {
        val existing = if (kind == CategoryKind.Expense) {
            (_state.value as? UiState.Content)?.data?.expenses
        } else {
            (_state.value as? UiState.Content)?.data?.incomes
        } ?: emptyList()
        return CategoryRules.isDuplicateName(name, existing, excludeId)
    }

    /** Categoría por id para precargar el formulario de edición. */
    fun find(id: String): CategoryUi? =
        (_state.value as? UiState.Content)?.data?.let { it.expenses + it.incomes }?.firstOrNull { it.id == id }

    private fun existingOf(kind: CategoryKind): List<CategoryUi> =
        (_state.value as? UiState.Content)?.data?.let {
            if (kind == CategoryKind.Expense) it.expenses else it.incomes
        } ?: emptyList()

    private suspend fun reload() {
        val totals = repository.getCategoryTotals()
        val kind = (_state.value as? UiState.Content)?.data?.kind ?: CategoryKind.Expense
        val expenses = repository.list(CategoryKind.Expense).map { it.toUi(totals) }
        val incomes = repository.list(CategoryKind.Income).map { it.toUi(totals) }
        updateList { CategoryListUi(kind = kind, expenses = expenses, incomes = incomes) }
    }

    private fun updateList(transform: (CategoryListUi) -> CategoryListUi) {
        val current = _state.value as? UiState.Content ?: return
        _state.value = UiState.Content(transform(current.data))
    }

    private fun Category.toUi(totals: Map<String, CategoryStats>): CategoryUi {
        val stats = totals[id] ?: CategoryStats()
        return CategoryUi(
            id = id,
            name = name,
            kind = categoryKind,
            icon = icon,
            color = color,
            monthlyLimit = monthlyLimit,
            position = position,
            movementCount = stats.movementCount,
            total = stats.total,
        )
    }

    companion object {
        /** Escritura del nuevo orden tras soltar el arrastre (D-10). */
        const val REORDER_PERSIST_DELAY_MS = 400L
    }
}
