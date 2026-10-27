package com.pocketpal.app.ui.movements

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DaiInsight
import com.pocketpal.app.data.movement.FakeMovementRepository
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.Recurrence
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

/** Flujo 4 · Detalle, recurrente y comprobante. */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 25)
    private lateinit var repository: FakeMovementRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMovementRepository()
        repository.addCategory("comida", "Comida", monthlyLimit = 700_000.0)
        repository.addCategory("ocio", "Entretenimiento")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = MovementDetailViewModel(repository, today = { today }, zone = { FakeMovementRepository.ZONE })

    private fun MovementDetailViewModel.ui() = (state.value as MovementDetailViewModel.UiState.Content).data

    @Test
    fun `carga el movimiento con su categoria y el mensaje de Dai sobre el presupuesto`() = runTest(dispatcher) {
        val carulla = repository.add("Supermercado Carulla", 86_450.0, today, categoryId = "comida", description = "Compra en Carulla")
        val vm = newVm()
        vm.load(carulla.id)
        advanceUntilIdle()

        val ui = vm.ui()
        assertEquals("Supermercado Carulla", ui.movement.title)
        assertEquals("Comida", ui.category?.name)
        assertEquals(DaiInsight.OfBudget(12, "Comida"), ui.insight)
        assertNull(ui.nextCharge)
    }

    @Test
    fun `sin limite Dai compara con lo gastado en la categoria ese mes`() = runTest(dispatcher) {
        val spotify = repository.add("Spotify", 25_000.0, today, categoryId = "ocio")
        repository.add("Cine", 75_000.0, today.minusDays(2), categoryId = "ocio")
        val vm = newVm()
        vm.load(spotify.id)
        advanceUntilIdle()
        assertEquals(DaiInsight.OfCategorySpending(25, "Entretenimiento"), vm.ui().insight)
    }

    @Test
    fun `Dai usa el mes del movimiento y no el mes actual`() = runTest(dispatcher) {
        val septiembre = repository.add("Cine", 50_000.0, LocalDate.of(2026, 9, 10), categoryId = "ocio")
        repository.add("Concierto", 50_000.0, LocalDate.of(2026, 9, 20), categoryId = "ocio")
        repository.add("Teatro", 400_000.0, today, categoryId = "ocio") // octubre: no cuenta
        val vm = newVm()
        vm.load(septiembre.id)
        advanceUntilIdle()
        assertEquals(DaiInsight.OfCategorySpending(50, "Entretenimiento"), vm.ui().insight)
    }

    @Test
    fun `un recurrente calcula el proximo cobro`() = runTest(dispatcher) {
        val spotify = repository.add(
            "Spotify", 24_900.0, LocalDate.of(2026, 10, 12), categoryId = "ocio", recurrence = Recurrence.Monthly,
        )
        val vm = newVm()
        vm.load(spotify.id)
        advanceUntilIdle()
        assertTrue(vm.ui().movement.isRecurring)
        assertEquals(LocalDate.of(2026, 11, 12), vm.ui().nextCharge)
    }

    @Test
    fun `un ingreso se compara con los ingresos del mes`() = runTest(dispatcher) {
        val salario = repository.add("Salario", 1_200_000.0, today, kind = MovementKind.Income)
        repository.add("Venta", 300_000.0, today.minusDays(3), kind = MovementKind.Income)
        val vm = newVm()
        vm.load(salario.id)
        advanceUntilIdle()
        assertEquals(DaiInsight.OfMonthIncome(80), vm.ui().insight)
    }

    @Test
    fun `un movimiento que ya no existe muestra no encontrado`() = runTest(dispatcher) {
        val vm = newVm()
        vm.load("no-existe")
        advanceUntilIdle()
        assertEquals(MovementDetailViewModel.UiState.NotFound, vm.state.value)
    }

    @Test
    fun `sin internet muestra el error y reintentar lo recupera`() = runTest(dispatcher) {
        val cafe = repository.add("Café", 8_500.0, today)
        val vm = newVm()
        repository.failNextWithNetwork = true
        vm.load(cafe.id)
        advanceUntilIdle()
        assertEquals(MovementDetailViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
        vm.refresh()
        advanceUntilIdle()
        assertEquals("Café", vm.ui().movement.title)
    }

    @Test
    fun `el resultado de descargar se muestra una vez`() = runTest(dispatcher) {
        val vm = newVm()
        vm.onReceiptResult(saved = true)
        assertEquals(MovementDetailViewModel.ReceiptResult.Saved, vm.receipt.value)
        vm.consumeReceipt()
        assertNull(vm.receipt.value)
        vm.onReceiptResult(saved = false)
        assertEquals(MovementDetailViewModel.ReceiptResult.Failed, vm.receipt.value)
    }

    @Test
    fun `el comprobante arma el texto para compartir sin campos vacios`() {
        val receipt = ReceiptContent(
            heading = "Comprobante de movimiento · PocketPal",
            title = "Supermercado Carulla",
            amount = "- $86.450",
            fields = listOf("Categoría" to "Comida", "Fecha" to "12 abr 2025 · 10:24 a. m.", "Descripción" to ""),
            footer = "Generado con PocketPal",
            movementId = "8f2c-41ab/x",
        )
        val text = receipt.shareText()
        assertTrue(text.startsWith("Comprobante de movimiento · PocketPal"))
        assertTrue(text.contains("Categoría: Comida"))
        assertFalse(text.contains("Descripción"))
        assertEquals(listOf("Supermercado Carulla", "- $86.450", "Categoría: Comida", "Fecha: 12 abr 2025 · 10:24 a. m."), receipt.pdfLines())
        assertEquals("PocketPal_comprobante_8f2c-41abx.pdf", receipt.fileName())
    }
}
