package com.pocketpal.app.ui.budgets

import com.pocketpal.app.auth.ErrorType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Flujo 3 · Detalle del presupuesto e historial. */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 14)
    private lateinit var repository: FakeBudgetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeBudgetRepository()
        repository.addCategory("comida", "Comida")
        repository.addCategory("transporte", "Transporte")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = BudgetDetailViewModel(
        repository,
        weekStart = { 1 },
        today = { today },
        zone = { FakeBudgetRepository.ZONE },
    )

    private fun BudgetDetailViewModel.detail() = (state.value as BudgetDetailViewModel.UiState.Content).data
    private fun BudgetDetailViewModel.historyUi() = (history.value as BudgetDetailViewModel.HistoryState.Content).data

    @Test
    fun `muestra lo gastado del mes, cuanto queda y cuando se reinicia`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 500_000.0)
        repository.addMovement("Mercado", 375_000.0, today.minusDays(3), "comida")
        repository.addMovement("Tienda de barrio", 45_000.0, today, "comida", hour = 14)
        repository.addMovement("Taxi", 12_000.0, today, "transporte")            // otra categoría
        repository.addMovement("Mes pasado", 99_000.0, LocalDate.of(2026, 9, 30), "comida")
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()

        val detail = vm.detail()
        assertEquals(420_000.0, detail.status.spent, 0.001)
        assertEquals(84, detail.status.percent)
        assertEquals(80_000.0, detail.status.remaining, 0.001)
        assertEquals(18, detail.daysUntilReset) // del 14 al 31 de octubre
        assertEquals(listOf("Tienda de barrio", "Mercado"), detail.movements.map { it.title })
    }

    @Test
    fun `un presupuesto semanal solo muestra los gastos de esta semana`() = runTest(dispatcher) {
        val budget = repository.addBudget("transporte", "Transporte", 60_000.0, period = BudgetPeriod.Weekly)
        repository.addMovement("Lunes", 10_000.0, LocalDate.of(2026, 10, 12), "transporte")    // esta semana
        repository.addMovement("Domingo", 20_000.0, LocalDate.of(2026, 10, 11), "transporte")  // semana pasada
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        assertEquals(10_000.0, vm.detail().status.spent, 0.001)
        assertEquals(5, vm.detail().daysUntilReset) // miércoles 14 → lunes 19
    }

    @Test
    fun `el aviso aparece si esta encendido y se llego al umbral`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 100_000.0, alert = true)
        repository.addMovement("Mercado", 85_000.0, today, "comida")
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        assertTrue(vm.detail().alertReached)
    }

    @Test
    fun `sin el aviso encendido no se avisa aunque se pase`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 100_000.0, alert = false)
        repository.addMovement("Mercado", 150_000.0, today, "comida")
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        assertFalse(vm.detail().alertReached)
        assertTrue(vm.detail().status.exceeded)
    }

    @Test
    fun `respeta el umbral elegido en Notificaciones`() = runTest(dispatcher) {
        repository.threshold = 90
        val budget = repository.addBudget("comida", "Comida", 100_000.0, alert = true)
        repository.addMovement("Mercado", 85_000.0, today, "comida")
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        assertFalse(vm.detail().alertReached)
    }

    @Test
    fun `si el presupuesto ya no existe`() = runTest(dispatcher) {
        val vm = newVm()
        vm.load("no-existe")
        advanceUntilIdle()
        assertEquals(BudgetDetailViewModel.UiState.NotFound, vm.state.value)
    }

    @Test
    fun `sin internet muestra el error y reintentar lo recupera`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 500_000.0)
        repository.failNextWithNetwork = true
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        assertEquals(BudgetDetailViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
        vm.refresh()
        advanceUntilIdle()
        assertEquals("Comida", vm.detail().status.budget.name)
    }

    @Test
    fun `historial de 6 meses y detalle solo de los meses con gastos`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 500_000.0)
        repository.addMovement("Septiembre", 420_000.0, LocalDate.of(2026, 9, 5), "comida")
        repository.addMovement("Julio", 510_000.0, LocalDate.of(2026, 7, 20), "comida")
        repository.addMovement("Octubre", 100_000.0, today, "comida")
        val vm = newVm()
        vm.load(budget.id)
        vm.loadHistory()
        advanceUntilIdle()

        val ui = vm.historyUi()
        assertEquals((5..10).map { YearMonth.of(2026, it) }, ui.months.map { it.month })
        assertEquals(
            listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9), YearMonth.of(2026, 7)),
            ui.monthsWithSpending.map { it.month },
        )
        assertEquals(102, ui.months[2].percent) // julio: se pasó
        assertEquals(1f, ui.months[2].fraction)
    }

    @Test
    fun `historial sin internet`() = runTest(dispatcher) {
        val budget = repository.addBudget("comida", "Comida", 500_000.0)
        val vm = newVm()
        vm.load(budget.id)
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.loadHistory()
        advanceUntilIdle()
        assertEquals(BudgetDetailViewModel.HistoryState.Error(ErrorType.NoNetwork), vm.history.value)
    }
}
