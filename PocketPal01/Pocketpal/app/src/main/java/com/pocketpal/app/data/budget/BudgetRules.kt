package com.pocketpal.app.data.budget

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Reglas del módulo de Presupuestos **sin Android ni Supabase**: período actual, lo
 * gastado, porcentajes, validación del formulario, categorías disponibles e historial.
 * Por ser funciones puras se prueban con JUnit sin emulador (BudgetRulesTest).
 */
object BudgetRules {

    const val NAME_MAX = 30
    const val MAX_AMOUNT = 999_999_999_999.0

    /** Umbral del aviso si no se puede leer el de "Notificaciones" (Módulo 2). */
    const val DEFAULT_THRESHOLD = 80

    /** Barras de la pantalla "Historial". */
    const val HISTORY_MONTHS = 6

    // ---------------- Período ----------------

    /**
     * Días del período que va hoy:
     * - Mensual: del 1 al último día del mes.
     * - Semanal: 7 días que empiezan el día elegido en "Inicio de semana" (Módulo 2;
     *   0 = domingo, 1 = lunes…).
     */
    fun periodRange(period: BudgetPeriod, today: LocalDate, weekStart: Int): DateRange = when (period) {
        BudgetPeriod.Monthly -> MovementRules.monthRange(YearMonth.from(today))
        BudgetPeriod.Weekly -> {
            val todayIndex = today.dayOfWeek.value % 7          // domingo = 0
            val back = (todayIndex - weekStart.mod(7) + 7) % 7  // días desde el inicio de semana
            val start = today.minusDays(back.toLong())
            DateRange(start, start.plusDays(6))
        }
    }

    /** "Se reinicia en 17 días": días que faltan para que empiece el siguiente período. */
    fun daysUntilReset(period: BudgetPeriod, today: LocalDate, weekStart: Int): Int =
        ChronoUnit.DAYS.between(today, periodRange(period, today, weekStart).end).toInt() + 1

    /**
     * Rango que cubre los períodos de todos los presupuestos (para traer los gastos con
     * UNA sola consulta). `null` si no hay presupuestos.
     */
    fun loadRange(budgets: List<Budget>, today: LocalDate, weekStart: Int): DateRange? {
        val ranges = budgets.map { periodRange(it.budgetPeriod, today, weekStart) }
        if (ranges.isEmpty()) return null
        return DateRange(ranges.minOf { it.start }, ranges.maxOf { it.end })
    }

    // ---------------- Lo gastado ----------------

    /** Suma de los **gastos** de la categoría con fecha (en la zona del teléfono) dentro del rango. */
    fun spentIn(categoryId: String, movements: List<Movement>, range: DateRange, zone: ZoneId): Double =
        movements
            .filter { it.movementKind == MovementKind.Expense && it.categoryId == categoryId }
            .filter { m -> m.localDate(zone)?.let { it in range } ?: false }
            .sumOf { it.amount }

    fun status(
        budget: Budget,
        category: MovementCategory?,
        movements: List<Movement>,
        today: LocalDate,
        weekStart: Int,
        zone: ZoneId,
    ): BudgetStatus {
        val range = periodRange(budget.budgetPeriod, today, weekStart)
        return BudgetStatus(budget, category, spentIn(budget.categoryId, movements, range, zone), range)
    }

    fun statuses(
        budgets: List<Budget>,
        categories: List<MovementCategory>,
        movements: List<Movement>,
        today: LocalDate,
        weekStart: Int,
        zone: ZoneId,
    ): List<BudgetStatus> {
        val byId = categories.associateBy { it.id }
        return budgets.map { status(it, byId[it.categoryId], movements, today, weekStart, zone) }
    }

    /** "Presupuestado $1.850.000 · Gastado $1.210.000 · 8 categorías activas". */
    fun summary(statuses: List<BudgetStatus>): BudgetsSummary = BudgetsSummary(
        limit = statuses.sumOf { it.budget.amountLimit },
        spent = statuses.sumOf { it.spent },
        active = statuses.size,
    )

    /** Porcentaje usado, redondeado ("84%"). Puede pasar de 100. */
    fun percentUsed(spent: Double, limit: Double): Int =
        if (limit <= 0.0) 0 else (spent / limit * 100).roundToInt()

    /** Largo de la barra (0..1). */
    fun fraction(spent: Double, limit: Double): Float =
        if (limit <= 0.0) 0f else (spent / limit).toFloat().coerceIn(0f, 1f)

    /** El aviso "Avísame al acercarme al límite" está encendido y ya se llegó al umbral. */
    fun isAlertReached(status: BudgetStatus, threshold: Int): Boolean =
        status.budget.alertEnabled && status.percent >= threshold

    // ---------------- Formulario ----------------

    /**
     * Categorías de **gastos** que todavía no tienen presupuesto (pantalla "Elegir
     * categoría"). Al editar, [keepCategoryId] (la del presupuesto en edición) sigue
     * apareciendo. Lista vacía = "Ya tienes presupuesto en todas tus categorías".
     */
    fun availableCategories(
        categories: List<MovementCategory>,
        budgets: List<Budget>,
        keepCategoryId: String? = null,
    ): List<MovementCategory> {
        val taken = budgets.map { it.categoryId }.toSet() - setOfNotNull(keepCategoryId)
        return categories.filter { it.kind == MovementKind.Expense.wire && it.id !in taken }
    }

    /** ¿Otra fila ya usa esa categoría? (un presupuesto por categoría, como el índice de la base). */
    fun isCategoryTaken(categoryId: String?, budgets: List<Budget>, excludeBudgetId: String? = null): Boolean =
        categoryId != null && budgets.any { it.categoryId == categoryId && it.id != excludeBudgetId }

    /** Primer problema del formulario, o `null` si se puede guardar. */
    fun validate(draft: BudgetDraft, budgets: List<Budget>, excludeBudgetId: String? = null): ErrorType? {
        val name = draft.name.trim()
        return when {
            draft.categoryId == null -> ErrorType.BudgetCategoryRequired
            name.isEmpty() -> ErrorType.BudgetNameEmpty
            name.length > NAME_MAX -> ErrorType.BudgetNameTooLong
            draft.amountLimit <= 0.0 || draft.amountLimit > MAX_AMOUNT -> ErrorType.InvalidBudgetAmount
            isCategoryTaken(draft.categoryId, budgets, excludeBudgetId) -> ErrorType.DuplicateBudget
            else -> null
        }
    }

    // ---------------- Historial ----------------

    /** Del primer día de hace 5 meses al último día de este mes (6 barras). */
    fun historyRange(today: LocalDate): DateRange {
        val current = YearMonth.from(today)
        return DateRange(current.minusMonths((HISTORY_MONTHS - 1).toLong()).atDay(1), current.atEndOfMonth())
    }

    /**
     * Límite equivalente de un mes: el mismo si es mensual; si es semanal, el límite por
     * semana × (días del mes ÷ 7). Así las barras de un presupuesto semanal se pueden comparar.
     */
    fun monthlyLimit(budget: Budget, month: YearMonth): Double = when (budget.budgetPeriod) {
        BudgetPeriod.Monthly -> budget.amountLimit
        BudgetPeriod.Weekly -> budget.amountLimit * month.lengthOfMonth() / 7.0
    }

    /** Las 6 barras, del mes más antiguo al actual. */
    fun history(budget: Budget, movements: List<Movement>, today: LocalDate, zone: ZoneId): List<MonthUsage> {
        val current = YearMonth.from(today)
        return (HISTORY_MONTHS - 1 downTo 0).map { back ->
            val month = current.minusMonths(back.toLong())
            MonthUsage(
                month = month,
                spent = spentIn(budget.categoryId, movements, MovementRules.monthRange(month), zone),
                limit = monthlyLimit(budget, month),
            )
        }
    }
}
