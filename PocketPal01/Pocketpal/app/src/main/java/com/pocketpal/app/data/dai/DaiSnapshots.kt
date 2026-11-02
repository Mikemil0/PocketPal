package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.Budget
import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRules
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Arma el [FinanceSnapshot] a partir de lo que llega de la base. Está aparte (y sin
 * Supabase) para que el repositorio real y el de pruebas hagan exactamente lo mismo.
 */
object DaiSnapshots {

    fun build(
        today: LocalDate,
        zone: ZoneId,
        weekStart: Int,
        userName: String,
        movements: List<Movement>,
        categories: List<MovementCategory>,
        budgets: List<Budget>,
        goals: List<Goal>,
    ): FinanceSnapshot {
        val month = YearMonth.from(today)
        val thisMonth = MovementRules.monthRange(month)
        val lastMonth = MovementRules.monthRange(month.minusMonths(1))
        val expenseCategories = categories.filter { it.kind == MovementKind.Expense.wire }
        return FinanceSnapshot(
            today = today,
            userName = userName,
            monthMovements = movements.filter { MovementRules.isInRange(it, thisMonth, zone) },
            previousMonthMovements = movements.filter { MovementRules.isInRange(it, lastMonth, zone) },
            categories = categories,
            // Lo gastado de cada presupuesto se calcula igual que en el Módulo 5.
            budgets = BudgetRules.statuses(budgets, expenseCategories, movements, today, weekStart, zone),
            goals = goals,
        )
    }
}
