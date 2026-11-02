package com.pocketpal.app.ui.dai

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.data.dai.DaiCard
import com.pocketpal.app.data.dai.DaiRole
import com.pocketpal.app.data.dai.FakeDaiRepository
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

/** Flujo 1 · Chat con Dai. */
@OptIn(ExperimentalCoroutinesApi::class)
class DaiChatViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27)
    private lateinit var repository: FakeDaiRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeDaiRepository().apply {
            addCategory("comida", "Comida")
            addBudget("comida", "Comida", 500_000.0)
            addExpense("Mercado", 340_000.0, LocalDate.of(2026, 10, 10), "comida")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = DaiChatViewModel(
        repository,
        userName = { "Juan Felipe" },
        weekStart = { 1 },
        today = { today },
        zone = { FakeDaiRepository.ZONE },
        replyDelayMs = 0,
    )

    @Test
    fun `sin historial el chat es nuevo`() = runTest(dispatcher) {
        val vm = newVm()
        vm.open()
        advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertTrue(vm.state.value.isNew)
    }

    @Test
    fun `trae el historial guardado`() = runTest(dispatcher) {
        repository.save(DaiRole.User, "Hola")
        repository.save(DaiRole.Dai, "¡Hola! ¿En qué puedo ayudarte?")
        val vm = newVm()
        vm.open()
        advanceUntilIdle()
        assertEquals(listOf("Hola", "¡Hola! ¿En qué puedo ayudarte?"), vm.state.value.messages.map { it.text })
        assertFalse(vm.state.value.isNew)
    }

    @Test
    fun `enviar guarda la pregunta y la respuesta de Dai con datos reales`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("¿Cómo voy con mi presupuesto?")
        advanceUntilIdle()

        val messages = vm.state.value.messages
        assertEquals(2, messages.size)
        assertTrue(messages[0].isUser)
        assertEquals("¿Cómo voy con mi presupuesto?", messages[0].text)
        val card = messages[1].card as DaiCard.Spending
        assertEquals(68, card.percent)                    // 340.000 de 500.000
        assertTrue(messages[1].text.contains("68 %"))
        // También quedaron en la "base".
        assertEquals(2, repository.messages.size)
        // Respuestas rápidas debajo del último mensaje de Dai.
        assertEquals(listOf(DaiBrain.CHIP_PATTERNS, DaiBrain.CHIP_ADVICE), vm.state.value.chips)
        assertFalse(vm.state.value.typing)
    }

    @Test
    fun `responde con el nombre del usuario`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("hola")
        advanceUntilIdle()
        assertTrue(vm.state.value.messages.last().text.startsWith("¡Hola, Juan!"))
    }

    @Test
    fun `no envia mensajes vacios`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("   ")
        advanceUntilIdle()
        assertTrue(repository.messages.isEmpty())
    }

    @Test
    fun `cada respuesta usa los datos del momento`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("¿cómo voy?")
        advanceUntilIdle()
        repository.addExpense("Restaurante", 100_000.0, today, "comida")
        vm.send("¿y ahora cómo voy?")
        advanceUntilIdle()
        assertEquals(88, (vm.state.value.messages.last().card as DaiCard.Spending).percent)
        assertEquals(2, repository.snapshotCalls)
    }

    @Test
    fun `sin internet muestra error y reintentar vuelve a enviar`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("¿cómo voy?")
        advanceUntilIdle()
        assertNull(vm.state.value.error)

        repository.failNextWithNetwork = true        // falla al guardar la pregunta
        vm.send("dame un consejo")
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.state.value.error)
        assertEquals(2, repository.messages.size)
        assertFalse(vm.state.value.typing)

        vm.retry()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(4, repository.messages.size)
        assertEquals("dame un consejo", repository.messages[2].text)
    }

    @Test
    fun `si la pregunta ya se guardo, reintentar no la repite`() = runTest(dispatcher) {
        val vm = newVm()
        vm.open()
        advanceUntilIdle()
        repository.failNextOnSnapshot = true          // la pregunta se guarda, falla al leer los datos
        vm.send("dame un consejo")
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.state.value.error)
        assertEquals(1, repository.messages.size)

        vm.retry()
        advanceUntilIdle()
        assertEquals(listOf("dame un consejo"), repository.messages.filter { it.isUser }.map { it.text })
        assertEquals(2, repository.messages.size)
    }

    @Test
    fun `borrar historial deja el chat nuevo`() = runTest(dispatcher) {
        val vm = newVm()
        vm.send("hola")
        advanceUntilIdle()
        vm.clearLocal()
        assertTrue(vm.state.value.isNew)
    }
}
