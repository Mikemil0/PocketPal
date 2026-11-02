package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.BudgetStatus
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/** Quién escribió el mensaje (columna `role` de `dai_messages`). */
enum class DaiRole(val wire: String) {
    User("user"),
    Dai("dai");

    companion object {
        fun fromWire(value: String?): DaiRole = entries.firstOrNull { it.wire == value } ?: Dai
    }
}

/**
 * Un mensaje del chat ya listo para la pantalla.
 * [card] es la tarjeta que acompaña algunas respuestas de Dai (`null` = solo texto).
 * [chips] son las respuestas rápidas que aparecen debajo ("Ver resumen", "Consejos").
 */
data class DaiMessage(
    val id: String,
    val role: DaiRole,
    val text: String,
    val card: DaiCard? = null,
    val chips: List<String> = emptyList(),
    val createdAt: LocalDateTime? = null,
) {
    val isUser: Boolean get() = role == DaiRole.User
}

/** Una categoría dentro del resumen de gastos ("Comida 34 % · $420.000"). */
@Serializable
data class CategoryShare(
    @SerialName("name") val name: String,
    @SerialName("color") val color: String,
    @SerialName("amount") val amount: Double,
    @SerialName("percent") val percent: Int,
)

/** Una meta dentro de la tarjeta de metas ("Viaje · $350.000 de $1.500.000 · 23 %"). */
@Serializable
data class GoalShare(
    @SerialName("name") val name: String,
    @SerialName("saved") val saved: Double,
    @SerialName("target") val target: Double,
    @SerialName("percent") val percent: Int,
)

/**
 * Tarjetas que Dai puede poner debajo de su mensaje. Es una `sealed class`: el
 * compilador obliga a que la pantalla sepa dibujar todas. Se guardan en la columna
 * `card` (jsonb) con un campo "type" que dice cuál es.
 */
@Serializable
sealed class DaiCard {

    /** "Gasto acumulado $820.000 · Presupuesto $1.200.000 · 68 % utilizado" + categorías principales. */
    @Serializable
    @SerialName("spending")
    data class Spending(
        @SerialName("spent") val spent: Double,
        @SerialName("budget") val budget: Double,
        @SerialName("percent") val percent: Int,
        @SerialName("categories") val categories: List<CategoryShare>,
    ) : DaiCard()

    /** Progreso de las metas activas. */
    @Serializable
    @SerialName("goals")
    data class Goals(@SerialName("goals") val goals: List<GoalShare>) : DaiCard()

    /** "Alerta de gasto": en los últimos días gastaste mucho en una categoría. */
    @Serializable
    @SerialName("alert")
    data class Alert(
        @SerialName("category") val category: String,
        @SerialName("amount") val amount: Double,
        @SerialName("days") val days: Int,
        @SerialName("percent_of_budget") val percentOfBudget: Int?,
        /** Cuánto terminaría el mes por encima del presupuesto (`null` si no se pasa). */
        @SerialName("projected_over") val projectedOver: Double?,
    ) : DaiCard()

    /** Lección de "Aprende con Dai" con su video recomendado. */
    @Serializable
    @SerialName("lesson")
    data class Lesson(
        @SerialName("topic") val topic: String,
        @SerialName("video_title") val videoTitle: String,
        @SerialName("minutes") val minutes: Int,
        @SerialName("video_query") val videoQuery: String,
    ) : DaiCard()

    /** "Todavía no tengo suficientes datos": qué le falta al usuario. */
    @Serializable
    @SerialName("missing_data")
    data class MissingData(
        @SerialName("income") val needsIncome: Boolean,
        @SerialName("expenses") val needsExpenses: Boolean,
        @SerialName("goals") val needsGoals: Boolean,
    ) : DaiCard()

    /** Factura aprobada y guardada como movimiento. */
    @Serializable
    @SerialName("receipt")
    data class ReceiptSaved(
        @SerialName("merchant") val merchant: String,
        @SerialName("amount") val amount: Double,
        @SerialName("category") val category: String?,
        @SerialName("date") val date: String,
        @SerialName("budget_percent") val budgetPercent: Int?,
    ) : DaiCard()
}

/** Lo que responde Dai: el texto, la tarjeta opcional y las respuestas rápidas. */
data class DaiReply(
    val text: String,
    val card: DaiCard? = null,
    val chips: List<String> = emptyList(),
)

/**
 * Todo lo que Dai necesita saber para responder, leído de Supabase en un solo paso:
 * movimientos del mes (y del anterior), categorías, presupuestos con lo gastado y metas.
 */
data class FinanceSnapshot(
    val today: LocalDate,
    val userName: String,
    val monthMovements: List<Movement>,
    val previousMonthMovements: List<Movement>,
    val categories: List<MovementCategory>,
    val budgets: List<BudgetStatus>,
    val goals: List<Goal>,
) {
    val monthExpenses: List<Movement> get() = monthMovements.filter { it.isExpense }
    val monthIncome: Double get() = monthMovements.filter { it.isIncome }.sumOf { it.amount }
    val activeGoals: List<Goal> get() = goals.filterNot { it.isCompleted }
}

/**
 * Lo que la cámara y el OCR sacaron de una factura, para que el usuario lo revise y
 * lo APRUEBE antes de guardar. Todo es editable en la pantalla "Confirmar".
 */
data class ReceiptDraft(
    val merchant: String,
    val amount: Double?,
    val date: LocalDate,
    val categoryId: String?,
    val reason: String,
    /** El texto que leyó el OCR, por si el usuario quiere verlo. */
    val rawText: String = "",
)

/**
 * "¿Dónde se guardará?": lo que la pantalla de aprobación le muestra al usuario
 * antes de tocar Guardar.
 */
data class ReceiptImpact(
    /** Nombre de la categoría ("Comida") o `null` = Sin categoría. */
    val categoryName: String?,
    /** Presupuesto de esa categoría, si tiene. */
    val budgetName: String?,
    val budgetPercentBefore: Int?,
    val budgetPercentAfter: Int?,
    val exceedsBudget: Boolean,
)
