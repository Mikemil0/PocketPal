package com.pocketpal.app.data.movement

import com.pocketpal.app.auth.ErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Reglas puras del Módulo 4 (sin Android ni Supabase). */
class MovementRulesTest {

    private val zone: ZoneId = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 10, 25)

    private fun movement(
        amount: Double,
        kind: MovementKind = MovementKind.Expense,
        at: String = "2026-10-25T15:24:00+00:00",
        categoryId: String? = null,
        title: String = "Café",
        description: String? = null,
        recurrence: Recurrence? = null,
        nextChargeOn: String? = null,
    ) = Movement(
        id = title + at, kind = kind.wire, title = title, amount = amount, categoryId = categoryId,
        description = description, occurredAt = at, recurrence = recurrence?.wire, nextChargeOn = nextChargeOn,
    )

    // ---------- Fechas ----------

    @Test
    fun `lee la fecha que manda Supabase con y sin microsegundos`() {
        assertEquals(Instant.parse("2026-10-25T15:24:00Z"), MovementRules.parseInstant("2026-10-25T15:24:00+00:00"))
        assertEquals(Instant.parse("2026-10-25T15:24:00.123456Z"), MovementRules.parseInstant("2026-10-25 15:24:00.123456+00:00"))
        assertNull(MovementRules.parseInstant("no es fecha"))
        assertNull(MovementRules.parseInstant(null))
    }

    @Test
    fun `rangos de los filtros rapidos`() {
        assertEquals(DateRange(today, today), MovementRules.rangeFor(QuickFilter.Today, today))
        assertEquals(DateRange(LocalDate.of(2026, 10, 19), today), MovementRules.rangeFor(QuickFilter.Last7Days, today))
        assertEquals(
            DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            MovementRules.rangeFor(QuickFilter.ThisMonth, today),
        )
        assertEquals(DateRange(LocalDate.of(2026, 9, 26), today), MovementRules.lastDays(30, today))
    }

    @Test
    fun `el mes anterior en enero es diciembre del ano pasado`() {
        assertEquals(
            DateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)),
            MovementRules.previousMonthRange(LocalDate.of(2026, 1, 15)),
        )
    }

    @Test
    fun `hoy en Colombia va de medianoche a medianoche hora Colombia`() {
        val (from, to) = MovementRules.toInstants(DateRange(today, today), zone)
        assertEquals(Instant.parse("2026-10-25T05:00:00Z"), from)
        assertEquals(Instant.parse("2026-10-26T05:00:00Z"), to)
    }

    @Test
    fun `un movimiento a las 11 pm de Colombia sigue siendo de ese dia aunque en UTC sea el siguiente`() {
        val late = movement(1000.0, at = "2026-10-26T04:30:00+00:00") // 25 oct 11:30 p. m. en Colombia
        assertEquals(today, late.localDate(zone))
        assertTrue(MovementRules.isInRange(late, DateRange(today, today), zone))
    }

    @Test
    fun `validacion del rango personalizado`() {
        assertNull(MovementRules.validateRange(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))))
        assertNull(MovementRules.validateRange(DateRange(today, today)))
        assertEquals(ErrorType.InvalidDateRange, MovementRules.validateRange(DateRange(today, today.minusDays(1))))
        assertEquals(ErrorType.InvalidDateRange, MovementRules.validateRange(DateRange(today.minusYears(2), today)))
    }

    // ---------- Totales ----------

    @Test
    fun `resumen separa ingresos y gastos y calcula el balance`() {
        val summary = MovementRules.summary(
            listOf(movement(1_200_000.0, MovementKind.Income), movement(86_450.0), movement(24_900.0, title = "Spotify"))
        )
        assertEquals(1_200_000.0, summary.income, 0.0)
        assertEquals(111_350.0, summary.expense, 0.0)
        assertEquals(1_088_650.0, summary.balance, 0.0)
    }

    @Test
    fun `variacion porcentual frente al periodo anterior`() {
        assertEquals(12, MovementRules.percentChange(112.0, 100.0))
        assertEquals(-8, MovementRules.percentChange(842_900.0, 916_200.0))
        assertNull(MovementRules.percentChange(500.0, 0.0))
        assertNull(MovementRules.percentChange(500.0, -100.0))
    }

    @Test
    fun `agrupa por dia con el dia mas reciente primero`() {
        val groups = MovementRules.groupByDay(
            listOf(
                movement(1.0, at = "2026-10-24T15:00:00+00:00", title = "a"),
                movement(2.0, at = "2026-10-25T13:00:00+00:00", title = "b"),
                movement(3.0, at = "2026-10-25T20:00:00+00:00", title = "c"),
            ),
            zone,
        )
        assertEquals(listOf(today, today.minusDays(1)), groups.map { it.date })
        assertEquals(listOf("c", "b"), groups.first().movements.map { it.title })
    }

    @Test
    fun `dias con movimientos para los puntos del calendario`() {
        val days = MovementRules.daysWithMovements(
            listOf(movement(1.0, at = "2026-10-02T15:00:00+00:00"), movement(2.0, at = "2026-10-02T18:00:00+00:00", title = "x")),
            zone,
        )
        assertEquals(setOf(LocalDate.of(2026, 10, 2)), days)
    }

    // ---------- Búsqueda ----------

    @Test
    fun `limpia la busqueda de caracteres especiales del filtro`() {
        assertEquals("café pan", MovementRules.sanitizeQuery("  café,   pan%  "))
        assertEquals("mercado", MovementRules.sanitizeQuery("(mercado)*"))
        assertEquals(MovementRules.MAX_QUERY_LENGTH, MovementRules.sanitizeQuery("a".repeat(100)).length)
    }

    @Test
    fun `se busca desde dos letras`() {
        assertFalse(MovementRules.canSearch(" m "))
        assertFalse(MovementRules.canSearch("%%"))
        assertTrue(MovementRules.canSearch("me"))
    }

    @Test
    fun `coincide en titulo o descripcion sin importar mayusculas`() {
        assertTrue(MovementRules.matches(movement(1.0, title = "Supermercado Norte"), "MERCADO"))
        assertTrue(MovementRules.matches(movement(1.0, title = "Carulla", description = "Mercado semanal"), "mercado"))
        assertFalse(MovementRules.matches(movement(1.0, title = "Spotify"), "mercado"))
    }

    @Test
    fun `filtra por tipo`() {
        val list = listOf(movement(1.0), movement(2.0, MovementKind.Income, title = "Salario"))
        assertEquals(1, MovementRules.filterByKind(list, MovementKind.Income).size)
        assertEquals(2, MovementRules.filterByKind(list, null).size)
    }

    // ---------- Categorías ----------

    @Test
    fun `totales por categoria de mayor a menor con barras relativas a la mayor`() {
        val totals = MovementRules.categoryTotals(
            listOf(
                movement(100.0, categoryId = "comida", title = "a"),
                movement(100.0, categoryId = "comida", title = "b"),
                movement(50.0, categoryId = "transporte", title = "c"),
                movement(30.0, title = "d"), // sin categoría
                movement(999.0, MovementKind.Income, categoryId = "salario", title = "e"),
            )
        )
        assertEquals(listOf("comida", "transporte", null), totals.map { it.categoryId })
        assertEquals(200.0, totals[0].total, 0.0)
        assertEquals(2, totals[0].count)
        assertEquals(1f, totals[0].fraction, 0.001f)
        assertEquals(0.25f, totals[1].fraction, 0.001f)
    }

    // ---------- Recurrentes ----------

    @Test
    fun `proximo cobro usa next_charge_on si aun no paso`() {
        val spotify = movement(24_900.0, recurrence = Recurrence.Monthly, nextChargeOn = "2026-11-12")
        assertEquals(LocalDate.of(2026, 11, 12), MovementRules.nextCharge(spotify, today, zone))
    }

    @Test
    fun `proximo cobro se calcula sumando la frecuencia si no hay fecha o ya paso`() {
        val monthly = movement(10.0, at = "2026-08-12T15:00:00+00:00", recurrence = Recurrence.Monthly, nextChargeOn = "2026-09-12")
        assertEquals(LocalDate.of(2026, 11, 12), MovementRules.nextCharge(monthly, today, zone))
        val weekly = movement(10.0, at = "2026-10-25T15:00:00+00:00", recurrence = Recurrence.Weekly)
        assertEquals(LocalDate.of(2026, 11, 1), MovementRules.nextCharge(weekly, today, zone))
        assertNull(MovementRules.nextCharge(movement(10.0), today, zone))
    }

    // ---------- Dai te ayuda ----------

    @Test
    fun `dai usa el presupuesto de la categoria si tiene limite`() {
        val comida = MovementCategory("comida", "expense", "Comida", "restaurant", "#FF9B9B", monthlyLimit = 700_000.0)
        val gasto = movement(86_450.0, categoryId = "comida")
        assertEquals(DaiInsight.OfBudget(12, "Comida"), MovementRules.insight(gasto, comida, listOf(gasto)))
    }

    @Test
    fun `dai usa lo gastado en la categoria si no tiene limite`() {
        val ocio = MovementCategory("ocio", "expense", "Entretenimiento", "star", "#EAB760")
        val spotify = movement(25.0, categoryId = "ocio", title = "Spotify")
        val cine = movement(75.0, categoryId = "ocio", title = "Cine")
        assertEquals(DaiInsight.OfCategorySpending(25, "Entretenimiento"), MovementRules.insight(spotify, ocio, listOf(spotify, cine)))
    }

    @Test
    fun `dai sin categoria compara con todos los gastos y en ingresos con los ingresos`() {
        val gasto = movement(10.0, title = "Farmacia")
        val otro = movement(30.0, title = "Taxi")
        assertEquals(DaiInsight.OfMonthSpending(25), MovementRules.insight(gasto, null, listOf(gasto, otro)))
        val salario = movement(1_200_000.0, MovementKind.Income, title = "Salario")
        val venta = movement(300_000.0, MovementKind.Income, title = "Venta")
        assertEquals(DaiInsight.OfMonthIncome(80), MovementRules.insight(salario, null, listOf(salario, venta, gasto)))
    }

    @Test
    fun `dai nunca muestra 0 por ciento en un gasto muy pequeno`() {
        val tiny = movement(1.0, title = "Chicle")
        val big = movement(1_000_000.0, title = "Arriendo")
        assertEquals(DaiInsight.OfMonthSpending(1), MovementRules.insight(tiny, null, listOf(tiny, big)))
    }

    @Test
    fun `monto con signo segun el tipo`() {
        assertEquals(-5.0, movement(5.0).signedAmount, 0.0)
        assertEquals(5.0, movement(5.0, MovementKind.Income).signedAmount, 0.0)
        assertEquals(YearMonth.of(2026, 10), YearMonth.from(movement(1.0).localDate(zone)!!))
    }
}
