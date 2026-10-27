package com.pocketpal.app.ui.movements

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.FakeMovementRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Flujo 3 · Por categorías y calendario: los dos ViewModels con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementCategoriesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 25)
    private lateinit var repository: FakeMovementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMovementRepository()
        repository.addCategory("comida", "Comida")
        repository.addCategory("ocio", "Entretenimiento")
        repository.add("Carulla", 175_400.0, today, categoryId = "comida")
        repository.add("Cine", 200_400.0, today.minusDays(3), categoryId = "ocio")
        repository.add("Farmacia", 35_400.0, today.minusDays(5))
        repository.add("Salario", 1_200_000.0, today, kind = MovementKind.Income)
        repository.add("Arriendo", 450_000.0, LocalDate.of(2026, 9, 10)) // mes pasado
        repository.add("Uber", 45_000.0, LocalDate.of(2026, 10, 2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categoriesVm() =
        MovementCategoriesViewModel(repository, today = { today }, zone = { FakeMovementRepository.ZONE })

    private fun calendarVm() =
        MovementCalendarViewModel(repository, today = { today }, zone = { FakeMovementRepository.ZONE })

    // ---------- Por categorías ----------

    @Test
    fun `gastos del mes por categoria de mayor a menor con otros al final`() = runTest(dispatcher) {
        val vm = categoriesVm()
        vm.refresh()
        advanceUntilIdle()
        val ui = (vm.state.value as MovementCategoriesViewModel.UiState.Content).data

        assertEquals(456_200.0, ui.monthExpense, 0.0)
        assertEquals(listOf("ocio", "comida", null), ui.totals.map { it.categoryId })
        assertEquals(1f, ui.totals.first().fraction, 0.001f)
        // Uber (sin categoría) + Farmacia (sin categoría) = 80.400 en "Otros"
        assertEquals(80_400.0, ui.totals.last().total, 0.0)
        assertEquals("Entretenimiento", ui.categories["ocio"]?.name)
    }

    @Test
    fun `compara los gastos con el mes pasado`() = runTest(dispatcher) {
        val vm = categoriesVm()
        vm.refresh()
        advanceUntilIdle()
        // 456.200 frente a 450.000 → +1 %
        assertEquals(1, (vm.state.value as MovementCategoriesViewModel.UiState.Content).data.change)
    }

    @Test
    fun `sin internet muestra el error`() = runTest(dispatcher) {
        val vm = categoriesVm()
        repository.failNextWithNetwork = true
        vm.refresh()
        advanceUntilIdle()
        assertEquals(MovementCategoriesViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
    }

    // ---------- Calendario ----------

    @Test
    fun `al abrir arranca con el rango de la lista y marca los dias con movimientos`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.open(DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
        advanceUntilIdle()
        val ui = vm.state.value
        assertEquals(YearMonth.of(2026, 10), ui.month)
        assertEquals(today, ui.end) // el fin no pasa de hoy
        assertEquals(
            setOf(today, today.minusDays(3), today.minusDays(5), LocalDate.of(2026, 10, 2)),
            ui.daysWithMovements,
        )
    }

    @Test
    fun `primer toque es el inicio y el segundo el fin`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.open(DateRange(today, today))
        vm.tapDay(LocalDate.of(2026, 10, 7))
        assertEquals(DateRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7)), vm.state.value.selection)
        vm.tapDay(LocalDate.of(2026, 10, 12))
        assertEquals(DateRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 12)), vm.state.value.selection)
        assertTrue(vm.state.value.isSelected(LocalDate.of(2026, 10, 9)))
        // Un tercer toque empieza un rango nuevo
        vm.tapDay(LocalDate.of(2026, 10, 20))
        assertEquals(LocalDate.of(2026, 10, 20), vm.state.value.start)
        assertNull(vm.state.value.end)
    }

    @Test
    fun `tocar un dia antes del inicio lo vuelve el nuevo inicio y los dias futuros se ignoran`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.open(DateRange(today, today))
        vm.tapDay(LocalDate.of(2026, 10, 12))
        vm.tapDay(LocalDate.of(2026, 10, 7))
        assertEquals(LocalDate.of(2026, 10, 7), vm.state.value.start)
        assertNull(vm.state.value.end)
        vm.tapDay(today.plusDays(1))
        assertNull(vm.state.value.end)
    }

    @Test
    fun `ultimos 30 dias y ver movimientos devuelve el rango`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.open(DateRange(today, today))
        vm.selectLast30Days()
        assertEquals(MovementCalendarViewModel.Mode.Last30Days, vm.state.value.mode)
        assertEquals(DateRange(LocalDate.of(2026, 9, 26), today), vm.confirm())
    }

    @Test
    fun `la cuadricula del mes respeta el inicio de semana del perfil`() {
        // 1 de octubre de 2026 es jueves.
        val mondayFirst = MovementCalendarViewModel.monthGrid(YearMonth.of(2026, 10), weekStart = 1)
        assertEquals(3, mondayFirst.takeWhile { it == null }.size) // L M M vacíos
        assertEquals(LocalDate.of(2026, 10, 1), mondayFirst[3])
        assertEquals(31 + 3, mondayFirst.size)
        val sundayFirst = MovementCalendarViewModel.monthGrid(YearMonth.of(2026, 10), weekStart = 0)
        assertEquals(4, sundayFirst.takeWhile { it == null }.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 0), MovementCalendarViewModel.weekOrder(1))
    }

    @Test
    fun `no se avanza a meses futuros y al retroceder se marcan los dias de ese mes`() = runTest(dispatcher) {
        val vm = calendarVm()
        vm.open(DateRange(today, today))
        vm.nextMonth()
        assertEquals(YearMonth.of(2026, 10), vm.state.value.month)
        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        assertEquals(setOf(LocalDate.of(2026, 9, 10)), vm.state.value.daysWithMovements)
    }
}
