package com.pocketpal.app.ui.dai

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.data.dai.DaiCard
import com.pocketpal.app.data.dai.DaiIntent
import com.pocketpal.app.data.dai.DaiTopic
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

/** Flujo 2 · Alertas, metas, educación y "faltan datos" del menú de Dai. */
@OptIn(ExperimentalCoroutinesApi::class)
class DaiInsightsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 10, 27)
    private lateinit var repository: FakeDaiRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeDaiRepository().apply {
            addCategory("comida", "Comida")
            addBudget("comida", "Comida", 300_000.0)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = DaiInsightsViewModel(
        repository,
        weekStart = { 1 },
        today = { today },
        zone = { FakeDaiRepository.ZONE },
    )

    private fun DaiInsightsViewModel.ui() = (state.value as DaiInsightsViewModel.UiState.Content).data

    @Test
    fun `alerta de los ultimos 3 dias con impacto estimado`() = runTest(dispatcher) {
        repository.addExpense("Mercado", 200_000.0, LocalDate.of(2026, 10, 5), "comida")
        repository.addExpense("Comida rápida", 120_000.0, LocalDate.of(2026, 10, 26), "comida")
        val vm = newVm()
        vm.load()
        advanceUntilIdle()

        val alert = vm.ui().alert!!
        assertEquals("Comida", alert.category)
        assertEquals(120_000.0, alert.amount, 0.001)
        assertEquals(40, alert.percentOfBudget)
        // 320.000 en 27 días → 367.407 en el mes: 67.407 por encima.
        assertEquals(67_407.0, alert.projectedOver!!, 1.0)
        assertTrue(vm.ui().alertText.startsWith("Noté que en los últimos 3 días"))
        // Ya se pasó del presupuesto: el video sugerido es de gastos hormiga.
        assertEquals("Gastos hormiga", vm.ui().suggestedLesson.topic)
    }

    @Test
    fun `sin gastos recientes no hay alerta`() = runTest(dispatcher) {
        repository.addExpense("Mercado", 50_000.0, LocalDate.of(2026, 10, 2), "comida")
        val vm = newVm()
        vm.load()
        advanceUntilIdle()
        assertNull(vm.ui().alert)
        assertTrue(vm.ui().hasData)
        assertEquals("Ahorro", vm.ui().suggestedLesson.topic)
    }

    @Test
    fun `sin datos muestra lo que falta`() = runTest(dispatcher) {
        repository.addGoal("Viaje", 100.0, 1_000.0)
        val vm = newVm()
        vm.load()
        advanceUntilIdle()
        val ui = vm.ui()
        assertFalse(ui.hasData)
        assertEquals(DaiCard.MissingData(needsIncome = true, needsExpenses = true, needsGoals = false), ui.missing)
    }

    @Test
    fun `metas activas con el mensaje de Dai`() = runTest(dispatcher) {
        repository.addGoal("Viaje", 350_000.0, 1_500_000.0)
        repository.addGoal("Universidad", 620_000.0, 1_000_000.0)
        val vm = newVm()
        vm.load()
        advanceUntilIdle()
        assertEquals(listOf(23, 62), vm.ui().goals.map { it.percent })
        assertTrue(vm.ui().goalsMessage.startsWith("¡Vas muy bien!"))
    }

    @Test
    fun `aprende con Dai cambia de tema`() = runTest(dispatcher) {
        val vm = newVm()
        assertEquals(DaiTopic.CompoundInterest, vm.topic.value)
        vm.selectTopic(DaiTopic.EmergencyFund)
        assertEquals("Fondo de emergencia", (vm.lesson().card as DaiCard.Lesson).topic)
    }

    @Test
    fun `las preguntas de los botones las entiende el chat`() {
        assertEquals(DaiIntent.Patterns, DaiBrain.detectIntent(DaiInsightsViewModel.explainPrompt("Comida")))
        assertEquals(DaiIntent.Advice, DaiBrain.detectIntent(DaiInsightsViewModel.ALTERNATIVES_PROMPT))
        assertEquals(DaiIntent.Advice, DaiBrain.detectIntent(DaiInsightsViewModel.SAVE_FASTER_PROMPT))
        assertEquals(DaiIntent.Advice, DaiBrain.detectIntent(DaiInsightsViewModel.RECOMMENDATION_PROMPT))
    }

    @Test
    fun `sin internet muestra error y reintentar lo recupera`() = runTest(dispatcher) {
        repository.failNextWithNetwork = true
        val vm = newVm()
        vm.load()
        advanceUntilIdle()
        assertEquals(DaiInsightsViewModel.UiState.Error(ErrorType.NoNetwork), vm.state.value)
        vm.load()
        advanceUntilIdle()
        assertFalse(vm.ui().hasData)
    }
}
