package com.pocketpal.app.ui.budgets

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.BudgetDraft
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.budget.FakeBudgetRepository
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

/** Flujo 4 · Editar y eliminar presupuesto. */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetEditViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27)
    private lateinit var repository: FakeBudgetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBudgetRepository()
        repository.addCategory("comida", "Comida")
        repository.addCategory("transporte", "Transporte")
        repository.addCategory("hogar", "Hogar")
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

    private fun draft(name: String = "Alimentos", amount: Double = 600_000.0, period: BudgetPeriod = BudgetPeriod.Weekly, alert: Boolean = true) =
        BudgetDraft(categoryId = null, name = name, amountLimit = amount, period = period, alertEnabled = alert)

    @Test
    fun `al abrir Editar se prellena y su categoria sigue disponible`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        repository.addBudget("transporte", "Transporte", 250_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()

        assertEquals(comida, vm.editing.value)
        assertEquals("comida", vm.selectedCategoryId.value)
        assertEquals(listOf("comida", "hogar"), vm.availableCategories().map { it.id })
        assertFalse(vm.isDuplicate()) // no choca consigo mismo
    }

    @Test
    fun `guardar cambia nombre, limite, periodo y aviso en la base`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        vm.update(draft())
        advanceUntilIdle()

        val saved = repository.budgets.single()
        assertEquals("Alimentos", saved.name)
        assertEquals(600_000.0, saved.amountLimit, 0.001)
        assertEquals(BudgetPeriod.Weekly, saved.budgetPeriod)
        assertTrue(saved.alertEnabled)
        assertEquals("comida", saved.categoryId)
        assertTrue(vm.event.value is BudgetFormViewModel.Event.Updated)
    }

    @Test
    fun `se puede pasar a una categoria libre`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        vm.selectCategory("hogar")
        vm.update(draft(name = "Hogar"))
        advanceUntilIdle()
        assertEquals("hogar", repository.budgets.single().categoryId)
    }

    @Test
    fun `no se puede pasar a una categoria que ya tiene presupuesto`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        repository.addBudget("transporte", "Transporte", 250_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        vm.selectCategory("transporte")
        assertTrue(vm.isDuplicate())
        vm.update(draft())
        assertEquals(ErrorType.DuplicateBudget, vm.error.value)
        assertEquals("comida", repository.budgets.first { it.id == comida.id }.categoryId)
    }

    @Test
    fun `eliminar borra el presupuesto pero no los gastos`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        repository.addMovement("Mercado", 420_000.0, today, "comida")
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        vm.delete()
        advanceUntilIdle()

        assertTrue(repository.budgets.isEmpty())
        assertEquals(1, repository.movements.size)
        assertEquals(BudgetFormViewModel.Event.Deleted, vm.event.value)
        assertNull(vm.editing.value)
        assertFalse(vm.deleting.value)
    }

    @Test
    fun `eliminar sin internet deja el presupuesto y muestra el error`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.delete()
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertEquals(1, repository.budgets.size)
        assertFalse(vm.deleting.value)
    }

    @Test
    fun `si el presupuesto ya no existe no se puede editar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareEdit("no-existe")
        advanceUntilIdle()
        assertNull(vm.editing.value)
        assertEquals(ErrorType.Unknown, vm.error.value)
    }

    @Test
    fun `despues de editar, Nuevo presupuesto vuelve a modo crear`() = runTest(dispatcher) {
        val comida = repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.prepareEdit(comida.id)
        advanceUntilIdle()
        vm.prepareNew()
        advanceUntilIdle()
        assertNull(vm.editing.value)
        assertEquals(listOf("transporte", "hogar"), vm.availableCategories().map { it.id })
        assertEquals("transporte", vm.selectedCategoryId.value)
    }
}
