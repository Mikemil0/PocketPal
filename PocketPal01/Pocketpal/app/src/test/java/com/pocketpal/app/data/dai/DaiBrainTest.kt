package com.pocketpal.app.data.dai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Base del módulo de Dai · cómo entiende y cómo responde (reglas puras). */
class DaiBrainTest {

    private val zone = FakeDaiRepository.ZONE
    private val today = LocalDate.of(2026, 10, 27) // martes

    /** Un usuario con datos parecidos a los del Figma. */
    private fun repoWithData() = FakeDaiRepository().apply {
        addCategory("comida", "Comida")
        addCategory("transporte", "Transporte")
        addCategory("ocio", "Entretenimiento")
        addBudget("comida", "Comida", 500_000.0)
        addBudget("transporte", "Transporte", 250_000.0)
        addIncome("Salario", 2_000_000.0, LocalDate.of(2026, 10, 1))
        addExpense("Mercado", 300_000.0, LocalDate.of(2026, 10, 10), "comida")
        addExpense("Restaurante", 120_000.0, LocalDate.of(2026, 10, 26), "comida")
        addExpense("Uber", 95_000.0, LocalDate.of(2026, 10, 5), "transporte")
        addExpense("Cine", 30_000.0, LocalDate.of(2026, 10, 3), "ocio")
        addExpense("Septiembre", 200_000.0, LocalDate.of(2026, 9, 20), "comida")
    }

    private fun snapshot(repo: FakeDaiRepository = repoWithData()) =
        runBlocking { repo.snapshot(today, zone, weekStart = 1, userName = "Juan Felipe") }

    @Test
    fun `normaliza tildes, mayusculas y signos`() {
        assertEquals("como voy con mi presupuesto", DaiBrain.normalize("¿Cómo VOY con mi presupuesto?"))
    }

    @Test
    fun `entiende lo que pide el usuario`() {
        assertEquals(DaiIntent.Spending, DaiBrain.detectIntent("Quiero saber cómo voy con mi presupuesto este mes."))
        assertEquals(DaiIntent.Spending, DaiBrain.detectIntent("Durante este mes cómo se fue moviendo mi dinero?"))
        assertEquals(DaiIntent.Patterns, DaiBrain.detectIntent("¿En qué estoy gastando más?"))
        assertEquals(DaiIntent.Patterns, DaiBrain.detectIntent("¿Por qué este mes no me alcanza?"))
        assertEquals(DaiIntent.Goals, DaiBrain.detectIntent("Revisar mis metas"))
        assertEquals(DaiIntent.Advice, DaiBrain.detectIntent("Dame un consejo para ahorrar"))
        assertEquals(DaiIntent.Alerts, DaiBrain.detectIntent("¿Hay algo que debería saber?"))
        assertEquals(DaiIntent.Learn, DaiBrain.detectIntent("¿Me puedes explicar qué es un interés compuesto?"))
        assertEquals(DaiIntent.Greeting, DaiBrain.detectIntent("Hola Dai"))
        assertEquals(DaiIntent.Unknown, DaiBrain.detectIntent("asdf qwerty"))
    }

    @Test
    fun `el orden importa - un consejo para ahorrar no es una meta`() {
        assertEquals(DaiIntent.Advice, DaiBrain.detectIntent("consejo para ahorrar"))
        assertEquals(DaiIntent.Learn, DaiBrain.detectIntent("explícame qué es un presupuesto"))
    }

    @Test
    fun `resumen del mes con gasto acumulado, presupuesto y categorias`() {
        val reply = DaiBrain.spending(snapshot())
        val card = reply.card as DaiCard.Spending
        assertEquals(545_000.0, card.spent, 0.001)       // solo octubre, solo gastos
        assertEquals(750_000.0, card.budget, 0.001)
        assertEquals(73, card.percent)
        assertEquals("Comida", card.categories.first().name)
        assertEquals(77, card.categories.first().percent)  // 420.000 de 545.000
        // Comida va en 84 % de su presupuesto: Dai lo señala.
        assertTrue(reply.text.contains("73 %"))
        assertTrue(reply.text.contains("Comida (84 %)"))
        assertEquals(listOf(DaiBrain.CHIP_PATTERNS, DaiBrain.CHIP_ADVICE), reply.chips)
    }

    @Test
    fun `sin gastos dice que le faltan datos`() {
        val repo = FakeDaiRepository().apply { addCategory("comida", "Comida") }
        val reply = DaiBrain.reply("¿cómo voy?", snapshot(repo), zone)
        val card = reply.card as DaiCard.MissingData
        assertTrue(card.needsIncome && card.needsExpenses && card.needsGoals)
        assertTrue(reply.text.startsWith("Todavía no tengo suficientes datos"))
    }

    @Test
    fun `patrones - mayor categoria, dia de la semana y comparacion con el mes pasado`() {
        val data = snapshot()
        assertEquals(DayOfWeek.SATURDAY, DaiBrain.topWeekday(data, zone)) // el 10 de octubre fue sábado
        // Hasta el 27: octubre 545.000 vs septiembre 200.000 → +173 %
        assertEquals(173, DaiBrain.compareWithLastMonth(data, zone))
        val text = DaiBrain.patterns(data, zone).text
        assertTrue(text.contains("Comida es tu mayor categoría"))
        assertTrue(text.contains("sábados"))
    }

    @Test
    fun `alerta de gasto de los ultimos 3 dias con impacto estimado`() {
        val card = DaiBrain.alertCard(snapshot(), zone)!!
        assertEquals("Comida", card.category)
        assertEquals(120_000.0, card.amount, 0.001)
        assertEquals(24, card.percentOfBudget)            // 120.000 de 500.000
        // Comida lleva 420.000 en 27 días → 482.222 al mes: no se pasa.
        assertNull(card.projectedOver)
    }

    @Test
    fun `consejo usa primero el presupuesto mas apretado`() {
        val text = DaiBrain.advice(snapshot()).text
        assertTrue(text.contains("Tu presupuesto de Comida va en 84 %"))
        assertTrue(text.contains("5 días"))   // del 27 al 31 de octubre
    }

    @Test
    fun `metas - la que mas avanza`() {
        val repo = repoWithData().apply {
            addGoal("Viaje", 350_000.0, 1_500_000.0)
            addGoal("Universidad", 620_000.0, 1_000_000.0)
            addGoal("Celular", 800_000.0, 800_000.0, completed = true)
        }
        val reply = DaiBrain.goals(snapshot(repo))
        val card = reply.card as DaiCard.Goals
        assertEquals(listOf("Viaje", "Universidad"), card.goals.map { it.name }) // sin la completada
        assertTrue(reply.text.contains("Universidad ya va en 62 %"))
    }

    @Test
    fun `lecciones por tema`() {
        assertEquals(DaiTopic.CompoundInterest, DaiBrain.topicOf("interés compuesto"))
        assertEquals(DaiTopic.Rule503020, DaiBrain.topicOf("la regla 50/30/20"))
        val reply = DaiBrain.lesson(DaiTopic.CompoundInterest)
        assertEquals("El poder del interés compuesto", (reply.card as DaiCard.Lesson).videoTitle)
        assertNotNull(DaiBrain.lesson(null).chips.firstOrNull())
    }

    @Test
    fun `saludo con el primer nombre y no entendi`() {
        assertTrue(DaiBrain.greeting("Juan Felipe González").text.startsWith("¡Hola, Juan!"))
        assertTrue(DaiBrain.reply("xyz", snapshot(), zone).text.startsWith("Disculpa"))
    }
}
