package com.pocketpal.app.ui.dai

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiRole
import com.pocketpal.app.data.dai.FakeDaiRepository
import com.pocketpal.app.data.dai.ReceiptProblem
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

/** Módulo 6 · Flujo 3 · Factura con la cámara: leer → confirmar → aprobar → guardar. */
@OptIn(ExperimentalCoroutinesApi::class)
class DaiReceiptViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27)
    private lateinit var repository: FakeDaiRepository

    /** Como llega de la cámara después de [ReceiptOcr]: filas ya armadas. */
    private val receipt = """
        TIENDA EXPRESS
        NIT 123456789-0
        12/10/2026 16:24
        Subtotal  $16.000
        IVA  $1.900
        Total  $17.900
        Efectivo  $20.000
        Cambio  $2.100
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeDaiRepository().apply {
            addCategory("comida", "Comida")
            addBudget("comida", "Comida", 500_000.0)
            addExpense("Mercado", 420_000.0, today, "comida")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = DaiReceiptViewModel(
        repository,
        today = { today },
        zone = { FakeDaiRepository.ZONE },
    ).also { it.start() }

    @Test
    fun `lee el total, no el efectivo ni el NIT, y abre Confirmar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onReading()
        assertEquals(DaiReceiptViewModel.Stage.Reading, vm.state.value.stage)
        vm.onTextRecognized(receipt)
        val state = vm.state.value
        assertEquals(DaiReceiptViewModel.Stage.Review, state.stage)
        assertEquals(17_900.0, state.draft.amount!!, 0.001)
        assertEquals("Tienda Express", state.draft.merchant)
        assertEquals("comida", state.draft.categoryId)
    }

    @Test
    fun `si no lee nada vuelve a la camara con el aviso`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onReading()
        vm.onTextRecognized("   ")
        assertEquals(DaiReceiptViewModel.Stage.Camera, vm.state.value.stage)
        assertTrue(vm.state.value.readFailed)
    }

    @Test
    fun `Guardar pide la aprobacion y todavia no escribe en la base`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onTextRecognized(receipt)
        val before = repository.movements.size
        vm.requestSave()
        val state = vm.state.value
        assertTrue(state.confirming)
        assertEquals(84, state.impact?.budgetPercentBefore)
        assertEquals(88, state.impact?.budgetPercentAfter)
        assertEquals(before, repository.movements.size)
    }

    @Test
    fun `sin monto no deja aprobar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.startManual()
        vm.update(vm.state.value.draft.copy(merchant = "Tienda"))
        vm.requestSave()
        assertEquals(ReceiptProblem.Amount, vm.state.value.problem)
        assertFalse(vm.state.value.confirming)
    }

    @Test
    fun `Si guardar crea el gasto y Dai lo cuenta en el chat`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onTextRecognized(receipt)
        vm.requestSave()
        vm.confirmSave()
        assertEquals(17_900.0, repository.movements.last().amount, 0.001)
        assertEquals(DaiRole.Dai, repository.messages.last().role)
        assertTrue(repository.messages.last().text.contains("Tienda Express"))
        assertEquals(DaiReceiptViewModel.Event.Saved, vm.event.value)
    }

    @Test
    fun `sin internet muestra el error y no se cierra`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onTextRecognized(receipt)
        vm.requestSave()
        repository.failNextWithNetwork = true
        vm.confirmSave()
        assertEquals(ErrorType.NoNetwork, vm.state.value.error)
        assertNull(vm.event.value)
        assertFalse(vm.state.value.saving)
    }
}
