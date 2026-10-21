package com.pocketpal.app.ui.goals

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.FakeGoalRepository
import com.pocketpal.app.data.goal.GoalDraft
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 3 · Detalle y aportes: [GoalDetailViewModel] con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 20)
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

    private fun newVm() = GoalDetailViewModel(repository, today = { today })

    private suspend fun goal(target: Double = 1_000.0, initial: Double = 0.0) =
        repository.create(
            GoalDraft(name = "Viaje", icon = "savings", color = "#FF9B9B", targetAmount = target, initialSaved = initial),
            0,
        )

    private fun GoalDetailViewModel.detail() = (state.value as GoalDetailViewModel.UiState.Content).data

    @Test
    fun `abrir la meta trae su historial con el ahorro inicial`() = runTest(dispatcher) {
        val g = goal(initial = 300.0)
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        assertEquals(300.0, vm.detail().goal.savedAmount, 0.0)
        assertTrue(vm.detail().contributions.single().isInitial)
    }

    @Test
    fun `agregar un aporte suma al ahorro y aparece primero en el historial`() = runTest(dispatcher) {
        val g = goal(initial = 300.0)
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        vm.addContribution(200.0, today, "  Prima  ")
        advanceUntilIdle()

        assertEquals(500.0, vm.detail().goal.savedAmount, 0.0)
        val first = vm.detail().contributions.first()
        assertEquals(200.0, first.amount, 0.0)
        assertEquals("Prima", first.note)
        assertEquals(GoalDetailViewModel.Event.ContributionAdded(completedNow = false), vm.event.value)
        assertNull(vm.celebration.value)
    }

    @Test
    fun `llegar al cien por ciento muestra la celebracion y la meta queda cumplida`() = runTest(dispatcher) {
        val g = goal(initial = 900.0)
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        vm.addContribution(100.0, today, null)
        advanceUntilIdle()

        assertTrue(vm.detail().goal.isCompleted)
        assertEquals(GoalDetailViewModel.Event.ContributionAdded(completedNow = true), vm.event.value)
        assertNotNull(vm.celebration.value)
        vm.dismissCelebration()
        assertNull(vm.celebration.value)
    }

    @Test
    fun `aportar de mas a una meta cumplida se permite pero no repite la celebracion`() = runTest(dispatcher) {
        val g = goal(initial = 900.0)
        repository.addContribution(g.id, 100.0, today, null)
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        vm.addContribution(50.0, today, null)
        advanceUntilIdle()

        assertEquals(1_050.0, vm.detail().goal.savedAmount, 0.0)
        assertEquals(GoalDetailViewModel.Event.ContributionAdded(completedNow = false), vm.event.value)
        assertNull(vm.celebration.value)
    }

    @Test
    fun `validaciones del aporte`() = runTest(dispatcher) {
        val g = goal()
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()

        vm.addContribution(0.0, today, null)
        assertEquals(ErrorType.InvalidContributionAmount, vm.error.value)
        vm.addContribution(100.0, today.plusDays(1), null)
        assertEquals(ErrorType.ContributionDateInFuture, vm.error.value)
        vm.addContribution(100.0, today, "x".repeat(81))
        assertEquals(ErrorType.ContributionNoteTooLong, vm.error.value)
        assertTrue(repository.contributions(g.id).isEmpty())
    }

    @Test
    fun `un aporte con fecha pasada se acepta`() = runTest(dispatcher) {
        val g = goal()
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        vm.addContribution(100.0, today.minusDays(5), null)
        advanceUntilIdle()
        assertEquals("2026-10-15", vm.detail().contributions.single().contributedOn)
    }

    @Test
    fun `sin internet al aportar se avisa y se conserva lo que se veia`() = runTest(dispatcher) {
        val g = goal(initial = 100.0)
        val vm = newVm()
        vm.load(g.id)
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.addContribution(100.0, today, null)
        advanceUntilIdle()

        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertFalse(vm.saving.value)
        assertEquals(100.0, vm.detail().goal.savedAmount, 0.0)
        assertNull(vm.event.value)
    }

    @Test
    fun `sin internet al abrir muestra la pantalla de error`() = runTest(dispatcher) {
        val g = goal()
        val vm = newVm()
        repository.failNextWithNetwork = true
        vm.load(g.id)
        advanceUntilIdle()
        assertEquals(GoalDetailViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
    }

    @Test
    fun `una meta que ya no existe muestra no encontrada`() = runTest(dispatcher) {
        val vm = newVm()
        vm.load("no-existe")
        advanceUntilIdle()
        assertEquals(GoalDetailViewModel.UiState.NotFound, vm.state.value)
    }
}
