package com.pocketpal.app.ui.register

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.FakeMovementRepository
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.NewMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

/** Módulo 6 · Flujo 5 · Registro manual y por voz desde el + del inicio. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 11, 4)
    private lateinit var repository: FakeMovementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMovementRepository()
        repository.addCategory("comida", "Comida")
        repository.addCategory("transporte", "Transporte")
        repository.addCategory("salario", "Salario", kind = MovementKind.Income)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = RegisterViewModel(repository, today = { today }, zone = { FakeMovementRepository.ZONE })

    @Test
    fun `registro manual guarda el gasto en movements con source manual`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startManual()
        assertEquals(today, vm.state.value.form.date)
        assertEquals(listOf("comida", "transporte"), vm.state.value.kindCategories.map { it.id })

        vm.setAmount("45000")
        vm.setCategory("comida")
        vm.setReason("Mercado de la semana")
        assertTrue(vm.state.value.hasChanges)
        vm.save()

        val saved = repository.created.single()
        assertEquals(MovementKind.Expense, saved.kind)
        assertEquals(45_000.0, saved.amount, 0.0)
        assertEquals("Mercado de la semana", saved.title)
        assertEquals(NewMovement.SOURCE_MANUAL, saved.source)
        assertTrue(vm.event.value is RegisterViewModel.Event.Saved)
        assertFalse(vm.state.value.changed)
    }

    @Test
    fun `sin monto o sin categoria no guarda y dice que falta`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startManual()
        vm.save()
        assertEquals(RegisterViewModel.Problem.Amount, vm.state.value.problem)

        vm.setAmount("12000")
        vm.save()
        assertEquals(RegisterViewModel.Problem.Category, vm.state.value.problem)

        vm.setCategory("transporte")
        vm.setDate(today.plusDays(1))
        vm.save()
        assertEquals(RegisterViewModel.Problem.FutureDate, vm.state.value.problem)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun `cambiar a ingreso borra la categoria de gasto y muestra las de ingreso`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startManual()
        vm.setCategory("comida")
        vm.setKind(MovementKind.Income)
        assertNull(vm.state.value.form.categoryId)
        assertEquals(listOf("salario"), vm.state.value.kindCategories.map { it.id })
    }

    @Test
    fun `por voz Dai llena el formulario y se guarda con source dai`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startVoice("Gasté 45 mil pesos en mercado, mercado de la semana")
        val form = vm.state.value.form
        assertEquals(RegisterViewModel.Mode.Voice, vm.state.value.mode)
        assertEquals(MovementKind.Expense, form.kind)
        assertEquals("45000", form.amountDigits)
        assertEquals("comida", form.categoryId)
        assertEquals("Mercado de la semana", form.reason)
        assertTrue(vm.state.value.hasChanges)

        vm.save()
        assertEquals(NewMovement.SOURCE_DAI, repository.created.single().source)
    }

    @Test
    fun `por voz entiende un ingreso`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startVoice("me pagaron el salario 2 millones 500 mil")
        val form = vm.state.value.form
        assertEquals(MovementKind.Income, form.kind)
        assertEquals("2500000", form.amountDigits)
        assertEquals("salario", form.categoryId)
    }

    @Test
    fun `sin internet muestra el error y deja guardar de nuevo`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startManual()
        vm.setAmount("8000")
        vm.setCategory("transporte")
        repository.failNextWithNetwork = true
        vm.save()
        assertEquals(ErrorType.NoNetwork, vm.state.value.error)
        assertFalse(vm.state.value.saving)
        assertNull(vm.event.value)

        vm.consumeError()
        vm.save()
        assertEquals(1, repository.created.size)
        assertTrue(vm.event.value is RegisterViewModel.Event.Saved)
    }
}
