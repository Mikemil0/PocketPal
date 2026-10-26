package com.pocketpal.app.ui.movements

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.FakeMovementRepository
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.QuickFilter
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

/** Flujo 1 · Lista principal: [MovementsViewModel] con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 25)
    private lateinit var repository: FakeMovementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMovementRepository()
        repository.addCategory("comida", "Comida")
        // Este mes
        repository.add("Supermercado Carulla", 86_450.0, today, categoryId = "comida", hour = 10)
        repository.add("Salario", 1_200_000.0, today, kind = MovementKind.Income, hour = 7)
        repository.add("Mercado Central", 42_800.0, today.minusDays(1), categoryId = "comida")
        repository.add("Arriendo", 650_000.0, today.minusDays(9))
        // Mes pasado
        repository.add("Salario", 1_200_000.0, LocalDate.of(2026, 9, 25), kind = MovementKind.Income)
        repository.add("Arriendo", 700_000.0, LocalDate.of(2026, 9, 16))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = MovementsViewModel(repository, today = { today }, zone = { FakeMovementRepository.ZONE })

    private fun MovementsViewModel.ui() = (state.value as MovementsViewModel.UiState.Content).data

    @Test
    fun `por defecto muestra este mes agrupado por dia con el resumen del mes`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()

        val ui = vm.ui()
        assertEquals(MovementsViewModel.Period.Quick(QuickFilter.ThisMonth), ui.period)
        assertEquals(listOf(today, today.minusDays(1), today.minusDays(9)), ui.groups.map { it.date })
        assertEquals(listOf("Supermercado Carulla", "Salario"), ui.groups.first().movements.map { it.title })
        assertEquals(1_200_000.0, ui.monthSummary.income, 0.0)
        assertEquals(779_250.0, ui.monthSummary.expense, 0.0)
        assertEquals("Comida", ui.categories["comida"]?.name)
    }

    @Test
    fun `compara el balance del mes con el del mes anterior`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        // Octubre: 1.200.000 - 779.250 = 420.750; septiembre: 1.200.000 - 700.000 = 500.000 → -16 %
        assertEquals(-16, vm.ui().monthChange)
    }

    @Test
    fun `filtro Hoy y 7 dias cambian la lista pero no el resumen del mes`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()

        vm.selectFilter(QuickFilter.Today)
        advanceUntilIdle()
        assertEquals(listOf(today), vm.ui().groups.map { it.date })
        assertEquals(779_250.0, vm.ui().monthSummary.expense, 0.0)

        vm.selectFilter(QuickFilter.Last7Days)
        advanceUntilIdle()
        assertEquals(listOf(today, today.minusDays(1)), vm.ui().groups.map { it.date })
    }

    @Test
    fun `un rango del calendario fuera del mes consulta esos dias`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        val september = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        vm.selectRange(september)
        advanceUntilIdle()

        assertEquals(MovementsViewModel.Period.Custom(september), vm.ui().period)
        assertEquals(listOf("Salario", "Arriendo"), vm.ui().groups.flatMap { g -> g.movements.map { it.title } })
        assertEquals(september, vm.currentRange())
    }

    @Test
    fun `un rango invalido avisa y deja el periodo como estaba`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        vm.selectRange(DateRange(today, today.minusDays(3)))
        assertEquals(ErrorType.InvalidDateRange, vm.error.value)
        assertEquals(MovementsViewModel.Period.Quick(QuickFilter.ThisMonth), vm.ui().period)
        vm.consumeError()
        assertNull(vm.error.value)
    }

    @Test
    fun `sin movimientos en el mes queda la pantalla vacia`() = runTest(dispatcher) {
        repository.movements.clear()
        val vm = newVm()
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.ui().monthIsEmpty)
        assertTrue(vm.ui().isEmpty)
        assertNull(vm.ui().monthChange)
    }

    @Test
    fun `periodo vacio dentro de un mes con datos no es la pantalla vacia completa`() = runTest(dispatcher) {
        repository.movements.removeAll { it.localDate(FakeMovementRepository.ZONE) == today }
        val vm = newVm()
        vm.selectFilter(QuickFilter.Today)
        advanceUntilIdle()
        assertTrue(vm.ui().isEmpty)
        assertFalse(vm.ui().monthIsEmpty)
    }

    @Test
    fun `sin internet muestra el error y reintentar lo recupera`() = runTest(dispatcher) {
        val vm = newVm()
        repository.failNextWithNetwork = true
        vm.refresh()
        advanceUntilIdle()
        assertEquals(MovementsViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)

        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.state.value is MovementsViewModel.UiState.Content)
    }

    @Test
    fun `el inicio recibe los 3 movimientos mas recientes con sus categorias`() = runTest(dispatcher) {
        val vm = newVm()
        vm.refreshRecent()
        advanceUntilIdle()
        val home = vm.home.value
        assertEquals(listOf("Supermercado Carulla", "Salario", "Mercado Central"), home.movements.map { it.title })
        assertEquals("Comida", home.categories["comida"]?.name)
    }
}
