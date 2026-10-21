package com.pocketpal.app.data.goal

import com.pocketpal.app.auth.ErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Reglas de negocio de Metas (RN-M1 a RN-M7) sin Android ni red. */
class GoalRulesTest {

    private val today = LocalDate.of(2026, 9, 13)

    // ---------------- Progreso ----------------

    @Test
    fun `porcentaje del Figma 300 mil de 2 millones es 15`() {
        assertEquals(15, GoalRules.percent(300_000.0, 2_000_000.0))
        assertEquals(0.15f, GoalRules.progress(300_000.0, 2_000_000.0), 0.0001f)
    }

    @Test
    fun `el porcentaje se redondea hacia abajo y nunca pasa de 100`() {
        assertEquals(99, GoalRules.percent(1_999_999.0, 2_000_000.0))
        assertEquals(100, GoalRules.percent(2_500_000.0, 2_000_000.0))
        assertEquals(1f, GoalRules.progress(2_500_000.0, 2_000_000.0), 0.0001f)
    }

    @Test
    fun `lo que falta nunca es negativo`() {
        assertEquals(1_700_000.0, GoalRules.remaining(300_000.0, 2_000_000.0), 0.0)
        assertEquals(0.0, GoalRules.remaining(2_500_000.0, 2_000_000.0), 0.0)
    }

    @Test
    fun `una meta se completa al alcanzar o superar el monto (RN-M7)`() {
        assertFalse(GoalRules.isCompleted(1_999_999.0, 2_000_000.0))
        assertTrue(GoalRules.isCompleted(2_000_000.0, 2_000_000.0))
        assertTrue(GoalRules.isCompleted(2_100_000.0, 2_000_000.0))
    }

    // ---------------- Fechas y sugerencia ----------------

    @Test
    fun `del 13 de septiembre al 15 de diciembre faltan 93 dias y 4 meses`() {
        val deadline = LocalDate.of(2026, 12, 15)
        assertEquals(93L, GoalRules.daysLeft(today, deadline))
        assertEquals(4, GoalRules.monthsLeft(today, deadline))
    }

    @Test
    fun `ahorro sugerido divide lo que falta entre los meses y redondea hacia arriba`() {
        val suggested = GoalRules.suggestedMonthly(300_000.0, 2_000_000.0, today, LocalDate.of(2026, 12, 15))
        assertEquals(425_000.0, suggested!!, 0.0)
        val odd = GoalRules.suggestedMonthly(0.0, 100.0, today, LocalDate.of(2026, 12, 15))
        assertEquals(25.0, odd!!, 0.0)
        val rounded = GoalRules.suggestedMonthly(0.0, 101.0, today, LocalDate.of(2026, 12, 15))
        assertEquals(26.0, rounded!!, 0.0)
    }

    @Test
    fun `sin fecha limite o completada no hay sugerencia`() {
        assertNull(GoalRules.suggestedMonthly(0.0, 100.0, today, null))
        assertNull(GoalRules.suggestedMonthly(100.0, 100.0, today, LocalDate.of(2026, 12, 15)))
    }

    @Test
    fun `si la fecha limite ya paso se sugiere todo lo que falta en un mes`() {
        val suggested = GoalRules.suggestedMonthly(0.0, 500.0, today, LocalDate.of(2026, 9, 1))
        assertEquals(500.0, suggested!!, 0.0)
    }

    @Test
    fun `parseDate lee fechas ISO y tolera valores vacios`() {
        assertEquals(LocalDate.of(2026, 12, 15), GoalRules.parseDate("2026-12-15"))
        assertEquals(LocalDate.of(2026, 12, 15), GoalRules.parseDate("2026-12-15T00:00:00Z"))
        assertNull(GoalRules.parseDate(null))
        assertNull(GoalRules.parseDate("no es fecha"))
    }

    // ---------------- Validación del formulario ----------------

    private fun draft(
        name: String = "Viaje",
        target: Double = 2_000_000.0,
        initial: Double = 0.0,
        deadline: LocalDate? = null,
    ) = GoalDraft(name = name, icon = "restaurant", color = "#FF9B9B", targetAmount = target, initialSaved = initial, deadline = deadline)

    private val existing = listOf(
        Goal(id = "g1", name = "Viaje", icon = "restaurant", color = "#FF9B9B", targetAmount = 2_000_000.0),
    )

    @Test
    fun `un formulario correcto no tiene errores`() {
        assertNull(GoalRules.validateGoal(draft(name = "Laptop", deadline = LocalDate.of(2026, 12, 15)), existing, today))
    }

    @Test
    fun `nombre vacio o largo (RN-M1)`() {
        assertEquals(ErrorType.GoalNameEmpty, GoalRules.validateGoal(draft(name = "   "), existing, today))
        assertEquals(ErrorType.GoalNameTooLong, GoalRules.validateGoal(draft(name = "x".repeat(31)), existing, today))
    }

    @Test
    fun `nombre repetido ignora mayusculas y espacios (RN-M1)`() {
        assertEquals(ErrorType.DuplicateGoalName, GoalRules.validateGoal(draft(name = "  viaje "), existing, today))
    }

    @Test
    fun `al editar la meta no choca con su propio nombre`() {
        assertNull(GoalRules.validateGoal(draft(name = "Viaje"), existing, today, excludeId = "g1", currentSaved = 0.0))
    }

    @Test
    fun `monto en cero no es valido (RN-M2)`() {
        assertEquals(ErrorType.InvalidGoalAmount, GoalRules.validateGoal(draft(name = "Laptop", target = 0.0), existing, today))
    }

    @Test
    fun `lo ya ahorrado debe ser menor que el monto (RN-M3)`() {
        assertEquals(
            ErrorType.InitialSavedTooHigh,
            GoalRules.validateGoal(draft(name = "Laptop", target = 100.0, initial = 100.0), existing, today),
        )
        assertNull(GoalRules.validateGoal(draft(name = "Laptop", target = 100.0, initial = 99.0), existing, today))
    }

    @Test
    fun `la fecha limite debe ser posterior a hoy (RN-M4)`() {
        assertEquals(ErrorType.DeadlineInPast, GoalRules.validateGoal(draft(name = "Laptop", deadline = today), existing, today))
        assertEquals(
            ErrorType.DeadlineInPast,
            GoalRules.validateGoal(draft(name = "Laptop", deadline = today.minusDays(1)), existing, today),
        )
    }

    @Test
    fun `al editar el monto no baja de lo ahorrado (RN-M5)`() {
        assertEquals(
            ErrorType.GoalAmountBelowSaved,
            GoalRules.validateGoal(draft(target = 200_000.0), existing, today, excludeId = "g1", currentSaved = 300_000.0),
        )
        assertNull(GoalRules.validateGoal(draft(target = 300_000.0), existing, today, excludeId = "g1", currentSaved = 300_000.0))
    }

    // ---------------- Validación del aporte ----------------

    @Test
    fun `aporte en cero, con fecha futura o motivo largo no es valido (RN-M6)`() {
        assertEquals(ErrorType.InvalidContributionAmount, GoalRules.validateContribution(0.0, today, today, null))
        assertEquals(ErrorType.ContributionDateInFuture, GoalRules.validateContribution(10.0, today.plusDays(1), today, null))
        assertEquals(ErrorType.ContributionNoteTooLong, GoalRules.validateContribution(10.0, today, today, "x".repeat(81)))
        assertNull(GoalRules.validateContribution(10.0, today, today, "Prima de diciembre"))
    }

    // ---------------- Orden ----------------

    private val toSort = listOf(
        Goal(id = "a", name = "A", icon = "", color = "", targetAmount = 100.0, savedAmount = 10.0, deadline = null, position = 2, createdAt = "2026-01-01"),
        Goal(id = "b", name = "B", icon = "", color = "", targetAmount = 100.0, savedAmount = 90.0, deadline = "2027-01-01", position = 0, createdAt = "2026-03-01"),
        Goal(id = "c", name = "C", icon = "", color = "", targetAmount = 100.0, savedAmount = 50.0, deadline = "2026-11-01", position = 1, createdAt = "2026-02-01"),
    )

    @Test
    fun `ordenar por posicion, cercania, fecha limite y recientes`() {
        assertEquals(listOf("b", "c", "a"), GoalRules.sort(toSort, GoalSort.Manual).map { it.id })
        assertEquals(listOf("b", "c", "a"), GoalRules.sort(toSort, GoalSort.Closest).map { it.id })
        assertEquals(listOf("c", "b", "a"), GoalRules.sort(toSort, GoalSort.Deadline).map { it.id })
        assertEquals(listOf("b", "c", "a"), GoalRules.sort(toSort, GoalSort.Recent).map { it.id })
    }
}
