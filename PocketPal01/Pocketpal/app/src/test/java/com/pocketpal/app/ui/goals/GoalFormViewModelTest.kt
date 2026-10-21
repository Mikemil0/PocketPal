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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 2 · Crear meta: [GoalFormViewModel] con el repositorio en memoria. */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalFormViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 9, 13)
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

    private fun newVm() = GoalFormViewModel(repository, today = { today })

    private fun draft(
        name: String = "Viaje",
        target: Double = 2_000_000.0,
        initial: Double = 0.0,
        deadline: LocalDate? = null,
    ) = GoalDraft(name = name, icon = "restaurant", color = "#FF9B9B", targetAmount = target, initialSaved = initial, deadline = deadline)

    @Test
    fun `crear guarda la meta con su ahorro inicial y emite Created`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        vm.create(draft(initial = 300_000.0, deadline = LocalDate.of(2026, 12, 15)))
        advanceUntilIdle()

        val event = vm.event.value as GoalFormViewModel.Event.Created
        assertEquals("Viaje", event.goal.name)
        assertEquals(300_000.0, event.goal.savedAmount, 0.0)
        assertEquals("2026-12-15", event.goal.deadline)
        assertEquals(event.goal, vm.created.value)
        // El ahorro inicial quedó como aporte de tipo "initial".
        assertTrue(repository.contributions(event.goal.id).single().isInitial)
    }

    @Test
    fun `sin ahorro inicial no se registra ningun aporte`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        vm.create(draft())
        advanceUntilIdle()
        val goal = (vm.event.value as GoalFormViewModel.Event.Created).goal
        assertTrue(repository.contributions(goal.id).isEmpty())
        assertEquals(0.0, goal.savedAmount, 0.0)
    }

    @Test
    fun `nombre repetido muestra el error y no crea nada`() = runTest(dispatcher) {
        repository.create(draft(), 0)
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        assertTrue(vm.isDuplicateName(" viaje "))
        vm.create(draft(name = "VIAJE"))
        assertEquals(ErrorType.DuplicateGoalName, vm.error.value)
        assertEquals(1, repository.list().size)
    }

    @Test
    fun `la base como autoridad traduce el duplicado al mismo error`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        repository.failNextWithDuplicate = true
        vm.create(draft(name = "Laptop"))
        advanceUntilIdle()
        assertEquals(ErrorType.DuplicateGoalName, vm.error.value)
        assertNull(vm.event.value)
    }

    @Test
    fun `validaciones del formulario`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        vm.create(draft(name = " "))
        assertEquals(ErrorType.GoalNameEmpty, vm.error.value)
        vm.create(draft(target = 0.0))
        assertEquals(ErrorType.InvalidGoalAmount, vm.error.value)
        vm.create(draft(target = 100.0, initial = 150.0))
        assertEquals(ErrorType.InitialSavedTooHigh, vm.error.value)
        vm.create(draft(deadline = today))
        assertEquals(ErrorType.DeadlineInPast, vm.error.value)
        assertTrue(repository.list().isEmpty())
    }

    @Test
    fun `sin internet muestra el error de red y deja de guardar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.create(draft())
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertFalse(vm.saving.value)
    }

    @Test
    fun `la nueva meta queda al final de la lista`() = runTest(dispatcher) {
        repository.create(draft(name = "Moto"), 0)
        repository.create(draft(name = "Casa"), 1)
        val vm = newVm()
        vm.prepareNew()
        advanceUntilIdle()
        vm.create(draft(name = "Viaje"))
        advanceUntilIdle()
        assertEquals(2, (vm.event.value as GoalFormViewModel.Event.Created).goal.position)
    }
}
