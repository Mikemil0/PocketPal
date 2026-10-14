package com.pocketpal.app.ui.profile

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.category.Category
import com.pocketpal.app.data.category.CategoryDraft
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.data.category.CategoryRepository
import com.pocketpal.app.data.category.CategoryRules
import com.pocketpal.app.data.category.CategoryStats
import com.pocketpal.app.data.category.DuplicateCategoryNameException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [CategoriesViewModel] con un repositorio simulado (RNF-02, §10):
 * crear, editar, eliminar y reordenar (RF-B7), más los errores de RN-02/RN-03.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeCategoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCategoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga las dos pestanas y empieza vacio`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        val state = vm.state.value as CategoriesViewModel.UiState.Content
        assertTrue(state.data.expenses.isEmpty())
        assertTrue(state.data.incomes.isEmpty())
        assertEquals(CategoryKind.Expense, state.data.kind)
    }

    @Test
    fun `crear agrega la categoria al final de su pestana`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos"))
        advanceUntilIdle()
        val state = vm.state.value as CategoriesViewModel.UiState.Content
        assertEquals(1, state.data.expenses.size)
        assertEquals("Alimentos", state.data.expenses.first().name)
        assertEquals(CategoriesViewModel.Event.Saved, vm.event.value)
    }

    @Test
    fun `nombre duplicado muestra el error de RN-02`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos"))
        advanceUntilIdle()
        vm.save(null, draft("  alimentos "))
        val state = vm.state.value as CategoriesViewModel.UiState.Content
        assertEquals(ErrorType.DuplicateCategoryName, vm.error.value)
        assertEquals(1, state.data.expenses.size)
    }

    @Test
    fun `al editar se excluye la propia categoria`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos"))
        advanceUntilIdle()
        val id = (vm.state.value as CategoriesViewModel.UiState.Content).data.expenses.first().id
        vm.save(id, draft("Alimentos"))
        assertEquals(null, vm.error.value)
    }

    @Test
    fun `nombre de mas de 30 caracteres no se guarda`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("x".repeat(31)))
        assertEquals(ErrorType.CategoryNameTooLong, vm.error.value)
        assertTrue((vm.state.value as CategoriesViewModel.UiState.Content).data.expenses.isEmpty())
    }

    @Test
    fun `limite mensual debe ser mayor que 0 (RN-03)`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos", monthlyLimit = 0.0))
        assertEquals(ErrorType.InvalidMonthlyLimit, vm.error.value)
        assertTrue((vm.state.value as CategoriesViewModel.UiState.Content).data.expenses.isEmpty())
    }

    @Test
    fun `la base como autoridad traduce el duplicado al mismo error`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        repository.failNextWithDuplicate = true
        vm.save(null, draft("Transporte"))
        assertEquals(ErrorType.DuplicateCategoryName, vm.error.value)
    }

    @Test
    fun `eliminar quita la categoria de la lista`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos"))
        advanceUntilIdle()
        val id = (vm.state.value as CategoriesViewModel.UiState.Content).data.expenses.first().id
        vm.delete(id)
        advanceUntilIdle()
        assertTrue((vm.state.value as CategoriesViewModel.UiState.Content).data.expenses.isEmpty())
        assertEquals(CategoriesViewModel.Event.Deleted, vm.event.value)
    }

    @Test
    fun `reordenar cambia el orden y lo persiste (TC-22)`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Alimentos"))
        vm.save(null, draft("Transporte"))
        advanceUntilIdle()
        val list = (vm.state.value as CategoriesViewModel.UiState.Content).data.expenses
        val first = list[0].id
        val second = list[1].id

        vm.move(fromId = second, toId = first)
        val reordered = (vm.state.value as CategoriesViewModel.UiState.Content).data.expenses
        assertEquals(listOf(second, first), reordered.map { it.id })

        advanceUntilIdle()
        assertEquals(listOf(second, first), repository.reorderCalls.single().second)
    }

    @Test
    fun `la lista de una pestana no mezcla la otra`() = runTest(dispatcher) {
        val vm = CategoriesViewModel(repository)
        advanceUntilIdle()
        vm.save(null, draft("Sueldo", kind = CategoryKind.Income))
        advanceUntilIdle()
        val state = vm.state.value as CategoriesViewModel.UiState.Content
        assertTrue(state.data.expenses.isEmpty())
        assertEquals(1, state.data.incomes.size)
        vm.selectKind(CategoryKind.Income)
        assertEquals("Sueldo", (vm.state.value as CategoriesViewModel.UiState.Content).data.current.single().name)
    }

    private fun draft(
        name: String,
        kind: CategoryKind = CategoryKind.Expense,
        monthlyLimit: Double? = null,
    ) = CategoryDraft(kind = kind, name = name, icon = "restaurant", color = "#FF9B9B", monthlyLimit = monthlyLimit)
}

/** Repositorio en memoria para probar el ViewModel sin Supabase (RNF-02). */
private class FakeCategoryRepository : CategoryRepository {

    private val categories = mutableListOf<Category>()
    val reorderCalls = mutableListOf<Pair<CategoryKind, List<String>>>()
    var failNextWithDuplicate = false
    private var nextId = 0

    override suspend fun list(kind: CategoryKind): List<Category> =
        categories.filter { it.categoryKind == kind }.sortedBy { it.position }

    override suspend fun get(id: String): Category? = categories.firstOrNull { it.id == id }

    override suspend fun create(draft: CategoryDraft, position: Int): Category {
        if (failNextWithDuplicate) {
            failNextWithDuplicate = false
            throw DuplicateCategoryNameException()
        }
        val category = Category(
            id = "c${nextId++}",
            kind = draft.kind.wire,
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            monthlyLimit = draft.monthlyLimit,
            position = position,
        )
        categories.add(category)
        return category
    }

    override suspend fun update(id: String, draft: CategoryDraft): Category {
        val index = categories.indexOfFirst { it.id == id }
        val updated = categories[index].copy(
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            monthlyLimit = draft.monthlyLimit,
        )
        categories[index] = updated
        return updated
    }

    override suspend fun delete(id: String) {
        categories.removeAll { it.id == id }
    }

    override suspend fun reorder(kind: CategoryKind, orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            val i = categories.indexOfFirst { it.id == id }
            categories[i] = categories[i].copy(position = index)
        }
        reorderCalls += kind to orderedIds
    }

    override suspend fun nameExists(kind: CategoryKind, name: String, excludeId: String?): Boolean =
        categories.any {
            it.categoryKind == kind && it.id != excludeId &&
                CategoryRules.normalize(it.name) == CategoryRules.normalize(name)
        }

    override suspend fun getCategoryTotals(): Map<String, CategoryStats> = emptyMap()
}
