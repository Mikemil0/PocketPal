package com.pocketpal.app.ui.goals

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.FakeGoalRepository
import com.pocketpal.app.data.goal.Goal
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

/** Flujo 4 · Editar y eliminar meta: [GoalFormViewModel] en modo edición. */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalEditViewModelTest {

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

    private fun newVm() = GoalFormViewModel(repository, today = { today })

    private fun draft(name: String = "Viaje", target: Double = 1_000.0, initial: Double = 0.0, deadline: LocalDate? = null) =
        GoalDraft(name = name, icon = "savings", color = "#FF9B9B", targetAmount = target, initialSaved = initial, deadline = deadline)

    private suspend fun editVm(goal: Goal): GoalFormViewModel {
        val vm = newVm()
        vm.prepareEdit(goal.id)
        return vm
    }

    @Test
    fun `abrir editar trae la meta y cuantos aportes tiene`() = runTest(dispatcher) {
        val g = repository.create(draft(initial = 300.0), 0)
        repository.addContribution(g.id, 100.0, today, null)
        val vm = editVm(g)
        advanceUntilIdle()
        assertEquals("Viaje", vm.editing.value?.name)
        assertEquals(400.0, vm.editing.value!!.savedAmount, 0.0)
        assertEquals(2, vm.contributionsCount.value)
    }

    @Test
    fun `guardar cambios actualiza nombre, monto y fecha sin tocar lo ahorrado`() = runTest(dispatcher) {
        val g = repository.create(draft(initial = 300.0), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.update(draft(name = "Viaje a Cartagena", target = 2_000.0, deadline = LocalDate.of(2027, 3, 1)))
        advanceUntilIdle()

        val updated = (vm.event.value as GoalFormViewModel.Event.Updated).goal
        assertEquals("Viaje a Cartagena", updated.name)
        assertEquals(2_000.0, updated.targetAmount, 0.0)
        assertEquals("2027-03-01", updated.deadline)
        assertEquals(300.0, updated.savedAmount, 0.0)
        assertEquals(1, repository.contributions(g.id).size)
    }

    @Test
    fun `quitar la fecha limite la deja sin fecha`() = runTest(dispatcher) {
        val g = repository.create(draft(deadline = LocalDate.of(2026, 12, 1)), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.update(draft(deadline = null))
        advanceUntilIdle()
        assertNull((vm.event.value as GoalFormViewModel.Event.Updated).goal.deadline)
    }

    @Test
    fun `el monto no puede quedar por debajo de lo ahorrado`() = runTest(dispatcher) {
        val g = repository.create(draft(initial = 500.0), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.update(draft(target = 400.0))
        assertEquals(ErrorType.GoalAmountBelowSaved, vm.error.value)
        assertEquals(1_000.0, repository.get(g.id)!!.targetAmount, 0.0)
    }

    @Test
    fun `bajar el monto hasta lo ahorrado completa la meta`() = runTest(dispatcher) {
        val g = repository.create(draft(initial = 500.0), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.update(draft(target = 500.0))
        advanceUntilIdle()
        assertTrue((vm.event.value as GoalFormViewModel.Event.Updated).goal.isCompleted)
    }

    @Test
    fun `la propia meta no cuenta como nombre repetido pero otra si`() = runTest(dispatcher) {
        val g = repository.create(draft(name = "Viaje"), 0)
        repository.create(draft(name = "Moto"), 1)
        val vm = editVm(g)
        advanceUntilIdle()
        assertFalse(vm.isDuplicateName("viaje"))
        assertTrue(vm.isDuplicateName("MOTO"))
        vm.update(draft(name = "Moto"))
        assertEquals(ErrorType.DuplicateGoalName, vm.error.value)
    }

    @Test
    fun `fecha limite de hoy o pasada no se acepta al editar`() = runTest(dispatcher) {
        val g = repository.create(draft(), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.update(draft(deadline = today))
        assertEquals(ErrorType.DeadlineInPast, vm.error.value)
    }

    @Test
    fun `eliminar borra la meta y sus aportes`() = runTest(dispatcher) {
        val g = repository.create(draft(initial = 300.0), 0)
        repository.addContribution(g.id, 100.0, today, null)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.delete()
        advanceUntilIdle()

        assertEquals(GoalFormViewModel.Event.Deleted, vm.event.value)
        assertEquals(listOf(g.id), repository.deletedIds)
        assertNull(repository.get(g.id))
        assertTrue(repository.contributions(g.id).isEmpty())
        assertNull(vm.editing.value)
    }

    @Test
    fun `sin internet al eliminar se avisa y la meta sigue`() = runTest(dispatcher) {
        val g = repository.create(draft(), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        repository.failNextWithNetwork = true
        vm.delete()
        advanceUntilIdle()

        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertFalse(vm.deleting.value)
        assertEquals(g.id, repository.get(g.id)?.id)
        assertNull(vm.event.value)
    }

    @Test
    fun `volver a crear despues de editar limpia el modo edicion`() = runTest(dispatcher) {
        val g = repository.create(draft(), 0)
        val vm = editVm(g)
        advanceUntilIdle()
        vm.prepareNew()
        assertNull(vm.editing.value)
        assertEquals(0, vm.contributionsCount.value)
    }
}
