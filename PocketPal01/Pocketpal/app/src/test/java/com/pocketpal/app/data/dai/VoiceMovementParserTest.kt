package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Módulo 6 · Flujo 5 · Lo que Dai entiende de una frase dicha por voz. */
class VoiceMovementParserTest {

    private val today = LocalDate.of(2026, 11, 4)
    private val categories = listOf(
        MovementCategory("comida", "expense", "Comida", "restaurant", "#FF9B9B"),
        MovementCategory("transporte", "expense", "Transporte", "car", "#9BB8FF"),
        MovementCategory("salario", "income", "Salario", "work", "#2EC4A6"),
    )

    private fun parse(text: String) = VoiceMovementParser.parse(text, today, categories)

    @Test
    fun `la frase del Figma`() {
        val d = parse("Gasté 45 mil pesos en mercado, mercado de la semana")
        assertEquals(MovementKind.Expense, d.kind)
        assertEquals(45_000.0, d.amount!!, 0.0)
        assertEquals(today, d.date)
        assertEquals("comida", d.categoryId)
        assertEquals("Mercado de la semana", d.reason)
    }

    @Test
    fun `monto en palabras y fecha de ayer`() {
        assertEquals(45_000.0, parse("Gasté cuarenta y cinco mil en mercado").amount!!, 0.0)
        val taxi = parse("pagué 20 mil de taxi ayer")
        assertEquals(today.minusDays(1), taxi.date)
        assertEquals("transporte", taxi.categoryId)
        assertEquals("Taxi", taxi.reason)
    }

    @Test
    fun `numeros con puntos, millones y abreviados`() {
        assertEquals(1_200_000.0, parse("gasté \$1.200.000 en arriendo").amount!!, 0.0)
        assertEquals(2_500_000.0, parse("me pagaron 2 millones 500 mil").amount!!, 0.0)
        assertEquals(1_500_000.0, parse("recibí 1,5 millones").amount!!, 0.0)
        assertEquals(45_000.0, parse("gaste 45mil en uber").amount!!, 0.0)
        assertEquals(1_500.0, parse("gasté mil quinientos en pan").amount!!, 0.0)
    }

    @Test
    fun `si dice varios numeros toma el monto mas grande`() {
        assertEquals(5_000.0, parse("compré dos cafés de 5.000").amount!!, 0.0)
    }

    @Test
    fun `un no es un monto`() {
        assertNull(parse("un mercado").amount)
    }

    @Test
    fun `ingreso con su categoria`() {
        val d = parse("me pagaron el salario 2 millones")
        assertEquals(MovementKind.Income, d.kind)
        assertEquals("salario", d.categoryId)
    }

    @Test
    fun `antier y hace dias`() {
        assertEquals(today.minusDays(2), parse("gasté 10 mil antier").date)
        assertEquals(today.minusDays(3), parse("gaste 45mil en uber hace 3 dias").date)
        assertEquals("Uber", parse("gaste 45mil en uber hace 3 dias").reason)
    }

    @Test
    fun `la frase del video, con categoria y motivo dichos`() {
        val d = parse("Me gasté el día de hoy \$50.000 de la categoría comida por motivo y que quería darme un gustico")
        assertEquals(MovementKind.Expense, d.kind)
        assertEquals(50_000.0, d.amount!!, 0.0)
        assertEquals(today, d.date)
        assertEquals("comida", d.categoryId)
        assertEquals("Quería darme un gustico", d.reason)
    }

    @Test
    fun `el dia de hoy no queda en el motivo`() {
        assertEquals("Almuerzo", parse("gasté 30 mil el día de hoy en almuerzo").reason)
        assertEquals("Me antojé", parse("gasté 8 mil en un helado porque me antojé").reason)
    }
}
