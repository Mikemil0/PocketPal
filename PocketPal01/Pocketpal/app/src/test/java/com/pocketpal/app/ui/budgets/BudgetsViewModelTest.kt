package com.pocketpal.app.ui.budgets

import com.pocketpal.app.auth.ErrorType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 1 · Lista de presupuestos y tarjeta del inicio. */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27) // martes
    private lateinit var repository: FakeBudgetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBudgetRepository()
        repository.addCategory("comida", "Comida")
        repository.addCategory("transporte", "Transporte")
        repository.addCategory("hogar", "Hogar")
        repository.addCategory("salario", "Salario", kind = MovementKind.Income)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm(weekStart: Int = 1) = BudgetsViewModel(
        repository,
        weekStart = { weekStart },
        today = { today },
        zone = { FakeBudgetRepository.ZONE },
    )

    private fun BudgetsViewModel.ui() = (state.value as BudgetsViewModel.UiState.Content).data

    @Test
    fun `calcula lo gastado de cada presupuesto y los totales`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        repository.addBudget("transporte", "Transporte", 250_000.0)
        repository.addMovement("Mercado", 420_000.0, today, "comida")
        repository.addMovement("Taxi", 95_000.0, today.minusDays(3), "transporte")
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()

        val ui = vm.ui()
        assertEquals(listOf(420_000.0, 95_000.0), ui.statuses.map { it.spent })
        assertEquals(listOf(84, 38), ui.statuses.map { it.percent })
        assertEquals(750_000.0, ui.summary.limit, 0.001)
        assertEquals(515_000.0, ui.summary.spent, 0.001)
        assertEquals(2, ui.summary.active)
        assertEquals("Comida", ui.statuses[0].category?.name)
    }

    @Test
    fun `los gastos de otro mes no cuentan`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        repository.addMovement("Septiembre", 300_000.0, LocalDate.of(2026, 9, 28), "comida")
        repository.addMovement("Octubre", 50_000.0, LocalDate.of(2026, 10, 1), "comida")
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(50_000.0, vm.ui().statuses.single().spent, 0.001)
    }

    @Test
    fun `un presupuesto semanal solo suma la semana actual segun el inicio de semana`() = runTest(dispatcher) {
        repository.addBudget("transporte", "Transporte", 50_000.0, period = BudgetPeriod.Weekly)
        repository.addMovement("Domingo", 10_000.0, LocalDate.of(2026, 10, 25), "transporte")
        repository.addMovement("Lunes", 20_000.0, LocalDate.of(2026, 10, 26), "transporte")

        val desdeLunes = newVm(weekStart = 1)
        desdeLunes.refresh()
        advanceUntilIdle()
        assertEquals(20_000.0, desdeLunes.ui().statuses.single().spent, 0.001)

        val desdeDomingo = newVm(weekStart = 0)
        desdeDomingo.refresh()
        advanceUntilIdle()
        assertEquals(30_000.0, desdeDomingo.ui().statuses.single().spent, 0.001)
    }

    @Test
    fun `el inicio muestra las dos primeras tarjetas`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        repository.addBudget("transporte", "Transporte", 250_000.0)
        repository.addBudget("hogar", "Hogar", 900_000.0)
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        val home = vm.home.value
        assertTrue(home.loaded)
        assertEquals(listOf("Comida", "Transporte"), home.statuses.map { it.budget.name })
        assertFalse(home.canAddMore) // todas las categorías de gastos ya tienen presupuesto
    }

    @Test
    fun `con un solo presupuesto el inicio invita a crear otro`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.home.value.statuses.size)
        assertTrue(vm.home.value.canAddMore)
        assertEquals(2, vm.ui().freeCategories) // transporte y hogar (salario es ingreso)
    }

    @Test
    fun `sin presupuestos la lista queda vacia y el inicio lo sabe`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.ui().isEmpty)
        assertEquals(0.0, vm.ui().summary.limit, 0.001)
        assertTrue(vm.home.value.loaded)
        assertTrue(vm.home.value.statuses.isEmpty())
    }

    @Test
    fun `sin internet muestra error y reintentar recupera la lista`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        repository.failNextWithNetwork = true
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(BudgetsViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)

        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.ui().statuses.size)
    }

    @Test
    fun `al volver a cargar se ven los cambios hechos en la base`() = runTest(dispatcher) {
        repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(0.0, vm.ui().summary.spent, 0.001)

        repository.addMovement("Mercado", 100_000.0, today, "comida")
        vm.refresh()
        advanceUntilIdle()
        assertEquals(100_000.0, vm.ui().summary.spent, 0.001)
        assertEquals(20, vm.ui().statuses.single().percent)
    }
}
