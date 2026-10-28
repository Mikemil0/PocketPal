package com.pocketpal.app.data.budget

import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.YearMonth

/** Cada cuánto se reinicia el presupuesto (botones "Mensual" / "Semanal" del Figma). */
enum class BudgetPeriod(val wire: String) {
    Monthly("monthly"),
    Weekly("weekly");

    companion object {
        fun fromWire(value: String?): BudgetPeriod = entries.firstOrNull { it.wire == value } ?: Monthly
    }
}

/**
 * Fila de `public.budgets` (migración `20261028000000_module5_budgets.sql`).
 *
 * Un presupuesto es un **límite** ([amountLimit]) para **una** categoría de gastos
 * ([categoryId]), por mes o por semana. Lo gastado no está en la tabla: se calcula con
 * los gastos de esa categoría en `movements` (ver [BudgetRules.status]).
 */
@Serializable
data class Budget(
    @SerialName("id") val id: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("name") val name: String,
    @SerialName("amount_limit") val amountLimit: Double,
    @SerialName("period") val period: String = BudgetPeriod.Monthly.wire,
    @SerialName("alert_enabled") val alertEnabled: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val budgetPeriod: BudgetPeriod get() = BudgetPeriod.fromWire(period)
}

/** Lo que llena el formulario "Nuevo presupuesto" / "Editar presupuesto". */
data class BudgetDraft(
    val categoryId: String?,
    val name: String,
    val amountLimit: Double,
    val period: BudgetPeriod,
    val alertEnabled: Boolean,
)

/**
 * Un presupuesto con lo gastado en su período actual (tarjetas de la lista, del inicio
 * y del detalle).
 */
data class BudgetStatus(
    val budget: Budget,
    val category: MovementCategory?,
    val spent: Double,
    val range: DateRange,
) {
    /** "84%" (puede pasar de 100 si ya se superó el límite). */
    val percent: Int get() = BudgetRules.percentUsed(spent, budget.amountLimit)

    /** Largo de la barra, de 0 a 1 (llena si ya se pasó). */
    val fraction: Float get() = BudgetRules.fraction(spent, budget.amountLimit)

    /** "Quedan $80.000" (0 si ya se pasó). */
    val remaining: Double get() = (budget.amountLimit - spent).coerceAtLeast(0.0)

    /** Cuánto se pasó del límite (0 si no se ha pasado). */
    val over: Double get() = (spent - budget.amountLimit).coerceAtLeast(0.0)

    val exceeded: Boolean get() = spent > budget.amountLimit
}

/** Tarjeta azul de la lista: "Presupuestado" y "Gastado". */
data class BudgetsSummary(val limit: Double, val spent: Double, val active: Int)

/** Una barra del historial: lo gastado en un mes frente al límite de ese mes. */
data class MonthUsage(val month: YearMonth, val spent: Double, val limit: Double) {
    val percent: Int get() = BudgetRules.percentUsed(spent, limit)
    val fraction: Float get() = BudgetRules.fraction(spent, limit)
}

/** Detalle de un presupuesto: su estado y los gastos del período (más reciente primero). */
data class BudgetDetail(
    val status: BudgetStatus,
    val movements: List<Movement>,
    val daysUntilReset: Int,
    /** `true` si el aviso está encendido y ya se llegó al umbral (80 % por defecto). */
    val alertReached: Boolean,
)
