package com.pocketpal.app.data.movement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Tipo de movimiento. En la base se guardan códigos neutros (`expense`, `income`);
 * los textos "Gasto"/"Ingreso" los pone la UI en el idioma elegido.
 */
enum class MovementKind(val wire: String) {
    Expense("expense"),
    Income("income");

    companion object {
        fun fromWire(value: String): MovementKind = entries.firstOrNull { it.wire == value } ?: Expense
    }
}

/** Cada cuánto se repite un movimiento recurrente (columna `recurrence`). */
enum class Recurrence(val wire: String) {
    Weekly("weekly"),
    Monthly("monthly"),
    Yearly("yearly");

    companion object {
        fun fromWire(value: String?): Recurrence? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * Fila de `public.movements` (migración `20261026000000_module4_movements.sql`).
 *
 * - [amount] siempre es positivo: el signo lo da [kind] (un gasto se pinta "- $").
 * - [occurredAt] es un instante con zona (`timestamptz`), por ejemplo
 *   `2026-10-25T15:24:00+00:00`. Para saber "qué día fue" se convierte a la zona
 *   del teléfono con [localDateTime] / [localDate].
 * - [categoryId] puede ser `null` ("sin categoría").
 * - [recurrence] `null` = movimiento normal; si tiene valor, es recurrente.
 */
@Serializable
data class Movement(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: String,
    @SerialName("title") val title: String,
    @SerialName("amount") val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("recurrence") val recurrence: String? = null,
    @SerialName("next_charge_on") val nextChargeOn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val movementKind: MovementKind get() = MovementKind.fromWire(kind)
    val isExpense: Boolean get() = movementKind == MovementKind.Expense
    val isIncome: Boolean get() = movementKind == MovementKind.Income

    /** Monto con signo: negativo para gastos, positivo para ingresos. */
    val signedAmount: Double get() = if (isExpense) -amount else amount

    val recurrenceType: Recurrence? get() = Recurrence.fromWire(recurrence)
    val isRecurring: Boolean get() = recurrenceType != null

    /** El instante del movimiento, o `null` si la fecha viniera dañada. */
    val instant: Instant? get() = MovementRules.parseInstant(occurredAt)

    fun localDateTime(zone: ZoneId): ZonedDateTime? = instant?.atZone(zone)

    fun localDate(zone: ZoneId): LocalDate? = localDateTime(zone)?.toLocalDate()
}

/**
 * Categoría reducida que necesita este módulo (nombre, ícono, color y límite
 * mensual). Se lee de la misma tabla `categories` del Módulo 2.
 */
@Serializable
data class MovementCategory(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: String,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("monthly_limit") val monthlyLimit: Double? = null,
)

/** Rango de fechas con ambos extremos incluidos (del 1 al 30 de abril, por ejemplo). */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}
