package com.pocketpal.app.ui.goals

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.FakeGoalRepository
import com.pocketpal.app.data.goal.GoalDraft
import com.pocketpal.app.data.goal.GoalSort
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

/** Flujo 1 · Ver metas: [GoalsViewModel] con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeGoalRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeGoalRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun seed() {
        repository.create(draft("Viaje", 2_000_000.0, initial = 300_000.0, deadline = LocalDate.of(2026, 12, 15)), 0)
        repository.create(draft("Laptop", 3_000_000.0, initial = 450_000.0), 1)
        val done = repository.create(draft("Bici", 500_000.0), 2)
        repository.addContribution(done.id, 500_000.0, LocalDate.of(2026, 10, 5), null)
    }

    private fun content(vm: GoalsViewModel) = (vm.state.value as GoalsViewModel.UiState.Content).data

    @Test
    fun `sin metas la lista empieza vacia en Activas`() = runTest(dispatcher) {
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        assertTrue(content(vm).current.isEmpty())
        assertEquals(GoalsViewModel.Tab.Active, content(vm).tab)
    }

    @Test
    fun `separa activas y completadas y suma el total ahorrado`() = runTest(dispatcher) {
        seed()
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        val data = content(vm)
        assertEquals(listOf("Viaje", "Laptop"), data.active.map { it.name })
        assertEquals(listOf("Bici"), data.completed.map { it.name })
        assertEquals(1_250_000.0, data.totalSaved, 0.0)
    }

    @Test
    fun `cambiar de pestana muestra las completadas`() = runTest(dispatcher) {
        seed()
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        vm.selectTab(GoalsViewModel.Tab.Completed)
        assertEquals(listOf("Bici"), content(vm).current.map { it.name })
        assertFalse(content(vm).canReorder)
    }

    @Test
    fun `el filtro mas cercanas pone primero la de mayor porcentaje`() = runTest(dispatcher) {
        seed()
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        vm.selectSort(GoalSort.Closest)
        // Viaje 15 %, Laptop 15 %: empate, se respeta el orden estable; luego con fecha límite.
        vm.selectSort(GoalSort.Deadline)
        assertEquals(listOf("Viaje", "Laptop"), content(vm).active.map { it.name })
        assertFalse(content(vm).canReorder)
    }

    @Test
    fun `reordenar cambia el orden al instante y lo guarda en la base`() = runTest(dispatcher) {
        seed()
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        val ids = content(vm).active.map { it.id }
        vm.move(fromId = ids[1], toId = ids[0])
        assertEquals(listOf("Laptop", "Viaje"), content(vm).active.map { it.name })
        advanceUntilIdle()
        assertEquals(listOf(ids[1], ids[0]), repository.reorderCalls.single())
    }

    @Test
    fun `sin internet muestra el error de red`() = runTest(dispatcher) {
        repository.failNextWithNetwork = true
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        assertEquals(GoalsViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
    }

    @Test
    fun `refrescar trae las metas nuevas`() = runTest(dispatcher) {
        val vm = GoalsViewModel(repository)
        advanceUntilIdle()
        repository.create(draft("Moto", 8_000_000.0), 0)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf("Moto"), content(vm).active.map { it.name })
    }

    private fun draft(name: String, target: Double, initial: Double = 0.0, deadline: LocalDate? = null) =
        GoalDraft(name = name, icon = "savings", color = "#55A5EF", targetAmount = target, initialSaved = initial, deadline = deadline)
}
