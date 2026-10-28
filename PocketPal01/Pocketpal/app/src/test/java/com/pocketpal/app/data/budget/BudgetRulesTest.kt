package com.pocketpal.app.data.budget

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.MovementKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Base del Módulo 5 · reglas puras de presupuestos. */
class BudgetRulesTest {

    private val zone = FakeBudgetRepository.ZONE
    private val today = LocalDate.of(2026, 10, 27) // martes

    private fun repoWithData(): FakeBudgetRepository = FakeBudgetRepository().apply {
        addCategory("comida", "Comida")
        addCategory("transporte", "Transporte")
        addCategory("hogar", "Hogar")
        addCategory("salario", "Salario", kind = MovementKind.Income)
    }

    @Test
    fun `periodo mensual va del 1 al ultimo dia del mes`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            BudgetRules.periodRange(BudgetPeriod.Monthly, today, weekStart = 1),
        )
    }

    @Test
    fun `periodo semanal respeta el inicio de semana`() {
        // Semana desde el lunes: lunes 26 a domingo 1.
        assertEquals(
            DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)),
            BudgetRules.periodRange(BudgetPeriod.Weekly, today, weekStart = 1),
        )
        // Semana desde el domingo: domingo 25 a sábado 31.
        assertEquals(
            DateRange(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31)),
            BudgetRules.periodRange(BudgetPeriod.Weekly, today, weekStart = 0),
        )
    }

    @Test
    fun `se reinicia en cuenta los dias que faltan para el siguiente periodo`() {
        assertEquals(5, BudgetRules.daysUntilReset(BudgetPeriod.Monthly, today, 1)) // 1 de noviembre
        assertEquals(6, BudgetRules.daysUntilReset(BudgetPeriod.Weekly, today, 1))  // lunes 2 de noviembre
        assertEquals(1, BudgetRules.daysUntilReset(BudgetPeriod.Monthly, LocalDate.of(2026, 10, 31), 1))
    }

    @Test
    fun `lo gastado suma solo gastos de la categoria dentro del periodo`() {
        val repo = repoWithData()
        repo.addMovement("Carulla", 86_450.0, today, "comida")
        repo.addMovement("Café", 8_500.0, LocalDate.of(2026, 10, 1), "comida")
        repo.addMovement("Mes pasado", 210_300.0, LocalDate.of(2026, 9, 30), "comida")   // fuera del mes
        repo.addMovement("Taxi", 12_000.0, today, "transporte")                           // otra categoría
        repo.addMovement("Reembolso", 50_000.0, today, "comida", kind = MovementKind.Income) // ingreso
        val range = BudgetRules.periodRange(BudgetPeriod.Monthly, today, 1)
        assertEquals(94_950.0, BudgetRules.spentIn("comida", repo.movements, range, zone), 0.001)
    }

    @Test
    fun `un gasto de las 11 pm del ultimo dia cuenta en ese mes en hora de Colombia`() {
        val repo = repoWithData()
        repo.addMovement("Cena", 40_000.0, LocalDate.of(2026, 9, 30), "comida", hour = 23) // 04:00 UTC del 1 oct
        val october = BudgetRules.periodRange(BudgetPeriod.Monthly, today, 1)
        assertEquals(0.0, BudgetRules.spentIn("comida", repo.movements, october, zone), 0.001)
    }

    @Test
    fun `porcentaje y barra`() {
        assertEquals(84, BudgetRules.percentUsed(420_000.0, 500_000.0))
        assertEquals(123, BudgetRules.percentUsed(98_500.0, 80_000.0))
        assertEquals(0, BudgetRules.percentUsed(10.0, 0.0))
        assertEquals(1f, BudgetRules.fraction(98_500.0, 80_000.0))
        assertEquals(0.5f, BudgetRules.fraction(50.0, 100.0))
    }

    @Test
    fun `estado calcula quedan y cuanto se paso`() {
        val repo = repoWithData()
        val budget = repo.addBudget("comida", "Comida", 500_000.0)
        repo.addMovement("Mercado", 420_000.0, today, "comida")
        val status = BudgetRules.status(budget, repo.categoryList[0], repo.movements, today, 1, zone)
        assertEquals(420_000.0, status.spent, 0.001)
        assertEquals(84, status.percent)
        assertEquals(80_000.0, status.remaining, 0.001)
        assertFalse(status.exceeded)

        repo.addMovement("Otro", 100_000.0, today, "comida")
        val over = BudgetRules.status(budget, null, repo.movements, today, 1, zone)
        assertTrue(over.exceeded)
        assertEquals(20_000.0, over.over, 0.001)
        assertEquals(0.0, over.remaining, 0.001)
    }

    @Test
    fun `resumen suma limites y gastado`() {
        val repo = repoWithData()
        repo.addBudget("comida", "Comida", 500_000.0)
        repo.addBudget("transporte", "Transporte", 250_000.0)
        repo.addMovement("Mercado", 420_000.0, today, "comida")
        repo.addMovement("Taxi", 95_000.0, today, "transporte")
        val statuses = BudgetRules.statuses(repo.budgets, repo.categoryList, repo.movements, today, 1, zone)
        assertEquals(BudgetsSummary(limit = 750_000.0, spent = 515_000.0, active = 2), BudgetRules.summary(statuses))
    }

    @Test
    fun `rango de carga cubre mensual y semanal`() {
        val repo = repoWithData()
        repo.addBudget("comida", "Comida", 500_000.0)
        repo.addBudget("transporte", "Transporte", 50_000.0, period = BudgetPeriod.Weekly)
        assertEquals(
            DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)),
            BudgetRules.loadRange(repo.budgets, today, weekStart = 1),
        )
        assertNull(BudgetRules.loadRange(emptyList(), today, 1))
    }

    @Test
    fun `el aviso se activa solo si esta encendido y se llego al umbral`() {
        val repo = repoWithData()
        val withAlert = repo.addBudget("comida", "Comida", 100.0, alert = true)
        val noAlert = repo.addBudget("transporte", "Transporte", 100.0, alert = false)
        repo.addMovement("A", 80.0, today, "comida")
        repo.addMovement("B", 95.0, today, "transporte")
        val s1 = BudgetRules.status(withAlert, null, repo.movements, today, 1, zone)
        val s2 = BudgetRules.status(noAlert, null, repo.movements, today, 1, zone)
        assertTrue(BudgetRules.isAlertReached(s1, 80))
        assertFalse(BudgetRules.isAlertReached(s1, 90))
        assertFalse(BudgetRules.isAlertReached(s2, 80))
    }

    @Test
    fun `categorias disponibles son gastos sin presupuesto`() {
        val repo = repoWithData()
        repo.addBudget("comida", "Comida", 500_000.0)
        val free = BudgetRules.availableCategories(repo.categoryList, repo.budgets)
        assertEquals(listOf("transporte", "hogar"), free.map { it.id }) // sin "salario" (ingreso)
        // Al editar el de comida, comida sigue apareciendo.
        val editing = BudgetRules.availableCategories(repo.categoryList, repo.budgets, keepCategoryId = "comida")
        assertEquals(listOf("comida", "transporte", "hogar"), editing.map { it.id })
    }

    @Test
    fun `sin categorias libres la lista queda vacia`() {
        val repo = repoWithData()
        repo.addBudget("comida", "Comida", 1.0)
        repo.addBudget("transporte", "Transporte", 1.0)
        repo.addBudget("hogar", "Hogar", 1.0)
        assertTrue(BudgetRules.availableCategories(repo.categoryList, repo.budgets).isEmpty())
    }

    @Test
    fun `validacion del formulario`() {
        val repo = repoWithData()
        val comida = repo.addBudget("comida", "Comida", 500_000.0)
        fun draft(category: String? = "hogar", name: String = "Hogar", amount: Double = 100.0) =
            BudgetDraft(category, name, amount, BudgetPeriod.Monthly, alertEnabled = false)

        assertEquals(ErrorType.BudgetCategoryRequired, BudgetRules.validate(draft(category = null), repo.budgets))
        assertEquals(ErrorType.BudgetNameEmpty, BudgetRules.validate(draft(name = "   "), repo.budgets))
        assertEquals(ErrorType.BudgetNameTooLong, BudgetRules.validate(draft(name = "x".repeat(31)), repo.budgets))
        assertEquals(ErrorType.InvalidBudgetAmount, BudgetRules.validate(draft(amount = 0.0), repo.budgets))
        assertEquals(ErrorType.DuplicateBudget, BudgetRules.validate(draft(category = "comida"), repo.budgets))
        assertNull(BudgetRules.validate(draft(), repo.budgets))
        // Editar el mismo presupuesto de comida no choca consigo mismo.
        assertNull(BudgetRules.validate(draft(category = "comida", name = "Alimentos"), repo.budgets, excludeBudgetId = comida.id))
    }

    @Test
    fun `historial de 6 meses del mas antiguo al actual`() {
        val repo = repoWithData()
        val budget = repo.addBudget("comida", "Comida", 500_000.0)
        repo.addMovement("Sep", 420_000.0, LocalDate.of(2026, 9, 10), "comida")
        repo.addMovement("Oct", 100_000.0, LocalDate.of(2026, 10, 2), "comida")
        repo.addMovement("Muy viejo", 999.0, LocalDate.of(2026, 4, 30), "comida") // fuera de los 6 meses
        val history = BudgetRules.history(budget, repo.movements, today, zone)
        assertEquals((5..10).map { YearMonth.of(2026, it) }, history.map { it.month })
        assertEquals(420_000.0, history[4].spent, 0.001)
        assertEquals(84, history[4].percent)
        assertEquals(100_000.0, history[5].spent, 0.001)
        assertEquals(0.0, history[0].spent, 0.001)
        assertEquals(DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 31)), BudgetRules.historyRange(today))
    }

    @Test
    fun `limite mensual equivalente de un presupuesto semanal`() {
        val weekly = Budget("x", "comida", "Comida", 70_000.0, BudgetPeriod.Weekly.wire)
        assertEquals(310_000.0, BudgetRules.monthlyLimit(weekly, YearMonth.of(2026, 10)), 0.001) // 31 días
        val monthly = weekly.copy(period = BudgetPeriod.Monthly.wire)
        assertEquals(70_000.0, BudgetRules.monthlyLimit(monthly, YearMonth.of(2026, 10)), 0.001)
    }

    @Test
    fun `el repositorio de prueba respeta un presupuesto por categoria`() = runBlocking {
        val repo = repoWithData()
        repo.create(BudgetDraft("comida", "Comida", 1.0, BudgetPeriod.Monthly, false))
        var duplicated = false
        try {
            repo.create(BudgetDraft("comida", "Otra", 1.0, BudgetPeriod.Monthly, false))
        } catch (e: DuplicateBudgetException) {
            duplicated = true
        }
        assertTrue(duplicated)
    }
}
