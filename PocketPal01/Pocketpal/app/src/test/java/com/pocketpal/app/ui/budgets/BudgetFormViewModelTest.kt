package com.pocketpal.app.ui.budgets

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.BudgetDraft
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.budget.FakeBudgetRepository
import com.pocketpal.app.data.movement.MovementKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 2 · Nuevo presupuesto, elegir categoría y presupuesto creado. */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetFormViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27)
    private lateinit var repository: FakeBudgetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBudgetRepository()
        repository.addCategory("comida", "Comida", monthlyLimit = 700_000.0)
        repository.addCategory("transporte", "Transporte")
        repository.addCategory("hogar", "Hogar")
        repository.addCategory("salario", "Salario", kind = MovementKind.Income)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = BudgetFormViewModel(
        repository,
        weekStart = { 1 },
        today = { today },
        zone = { FakeBudgetRepository.ZONE },
    )

    private fun draft(name: String = "Hogar", amount: Double = 900_000.0, period: BudgetPeriod = BudgetPeriod.Monthly, alert: Boolean = false) =
        BudgetDraft(categoryId = null, name = name, amountLimit = amount, period = period, alertEnabled = alert)

    @Test
    fun `al abrir elige la primera categoria libre y trae lo gastado del mes`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        repository.addMovement("Taxi", 12_000.0, today, "transporte")
        repository.addMovement("Gasolina", 120_000.0, today.minusDays(5), "transporte")
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()

        assertTrue(vm.loaded.value)
        assertEquals("transporte", vm.selectedCategoryId.value)
        assertEquals(listOf("Transporte", "Hogar"), vm.availableCategories().map { it.name })
        assertEquals(132_000.0, vm.monthSpent.value["transporte"]!!, 0.001)
    }

    @Test
    fun `elegir otra categoria cambia la seleccion`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        assertEquals("comida", vm.selectedCategoryId.value)
        vm.selectCategory("hogar")
        assertEquals("Hogar", vm.selectedCategory()?.name)
    }

    @Test
    fun `crear guarda en la base y deja listo Presupuesto creado`() = runTest(dispatcher) {
        repository.addMovement("Arriendo", 650_000.0, today.minusDays(9), "hogar")
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.selectCategory("hogar")
        vm.create(draft(alert = true))
        advanceUntilIdle()

        val saved = repository.budgets.single()
        assertEquals("hogar", saved.categoryId)
        assertEquals("Hogar", saved.name)
        assertEquals(900_000.0, saved.amountLimit, 0.001)
        assertTrue(saved.alertEnabled)
        assertTrue(vm.event.value is BudgetFormViewModel.Event.Created)
        val created = vm.created.value!!
        assertEquals(650_000.0, created.spent, 0.001)
        assertEquals(72, created.percent)
        assertFalse(vm.saving.value)
    }

    @Test
    fun `se guarda el periodo semanal`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.create(draft(name = "Mercado", amount = 150_000.0, period = BudgetPeriod.Weekly))
        advanceUntilIdle()
        assertEquals(BudgetPeriod.Weekly, repository.budgets.single().budgetPeriod)
    }

    @Test
    fun `nombre vacio no guarda`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.create(draft(name = "   "))
        assertEquals(ErrorType.BudgetNameEmpty, vm.error.value)
        assertTrue(repository.budgets.isEmpty())
    }

    @Test
    fun `monto en cero no guarda`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.create(draft(amount = 0.0))
        assertEquals(ErrorType.InvalidBudgetAmount, vm.error.value)
        assertTrue(repository.budgets.isEmpty())
    }

    @Test
    fun `si todas las categorias tienen presupuesto no hay categoria elegida`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 1.0)
        repository.addBudget("transporte", "Transporte", 1.0)
        repository.addBudget("hogar", "Hogar", 1.0)
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        assertTrue(vm.availableCategories().isEmpty())
        assertNull(vm.selectedCategoryId.value)
        vm.create(draft())
        assertEquals(ErrorType.BudgetCategoryRequired, vm.error.value)
    }

    @Test
    fun `si otro telefono ya creo el presupuesto de esa categoria se muestra el aviso del Figma`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.selectCategory("hogar")
        assertFalse(vm.isDuplicate())
        // Mientras tanto, desde otro dispositivo, alguien creó el de Hogar.
        repository.addBudget("hogar", "Hogar", 800_000.0)

        vm.create(draft())
        advanceUntilIdle()
        assertEquals(ErrorType.DuplicateBudget, vm.error.value)
        assertTrue(vm.isDuplicate()) // se recargó la lista: el aviso queda en pantalla
        assertEquals(1, repository.budgets.size)
    }

    @Test
    fun `sin internet al guardar muestra el error y se puede reintentar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.create(draft())
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertFalse(vm.saving.value)

        vm.consumeError()
        vm.create(draft())
        advanceUntilIdle()
        assertEquals(1, repository.budgets.size)
    }
}
