package com.pocketpal.app.data.movement

import com.pocketpal.app.auth.ErrorType
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Filtros rápidos de la lista principal (Figma: "Hoy", "7 días", "Este mes"). */
enum class QuickFilter { Today, Last7Days, ThisMonth }

/** Totales de un período: lo que entró, lo que salió y el balance. */
data class PeriodSummary(val income: Double = 0.0, val expense: Double = 0.0) {
    /** "Total del mes" = ingresos - gastos. */
    val balance: Double get() = income - expense
}

/** Movimientos de un mismo día, para los encabezados "Hoy, 12 abr 2025". */
data class DayGroup(val date: LocalDate, val movements: List<Movement>)

/**
 * Total de una categoría en un período (pantalla "Movimientos por categorías").
 * [categoryId] `null` = movimientos sin categoría ("Otros").
 * [fraction] es el largo de la barra (0..1) comparado con la categoría más alta.
 */
data class CategoryTotal(
    val categoryId: String?,
    val total: Double,
    val count: Int,
    val fraction: Float,
)

/** Mensaje de "Dai te ayuda" en el detalle del movimiento. */
sealed interface DaiInsight {
    val percent: Int

    /** "Este gasto representa el 12 % de tu presupuesto en comida" (la categoría tiene límite). */
    data class OfBudget(override val percent: Int, val categoryName: String) : DaiInsight

    /** "…el 12 % de lo que gastaste en comida este mes" (la categoría no tiene límite). */
    data class OfCategorySpending(override val percent: Int, val categoryName: String) : DaiInsight

    /** "…el 12 % de tus gastos del mes" (gasto sin categoría). */
    data class OfMonthSpending(override val percent: Int) : DaiInsight

    /** "Este ingreso representa el 12 % de lo que recibiste este mes". */
    data class OfMonthIncome(override val percent: Int) : DaiInsight
}

/**
 * Reglas del Módulo 4 **sin Android ni Supabase**: rangos de fechas, totales, agrupar
 * por día, búsqueda, totales por categoría, próximo cobro y el mensaje de Dai.
 * Por ser funciones puras se prueban con JUnit sin emulador (MovementRulesTest).
 */
object MovementRules {

    const val MIN_QUERY_LENGTH = 2
    const val MAX_QUERY_LENGTH = 40
    const val MAX_RANGE_DAYS = 366L

    // ---------------- Fechas ----------------

    /** Lee el `timestamptz` que manda Supabase (`2026-10-25T15:24:00+00:00`). */
    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value.trim().replace(' ', 'T')).toInstant()
        } catch (e: Exception) {
            null
        }
    }

    fun parseDate(value: String?): LocalDate? =
        try {
            value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it.take(10)) }
        } catch (e: Exception) {
            null
        }

    /** Rango de cada filtro rápido. "7 días" = hoy y los 6 anteriores. */
    fun rangeFor(filter: QuickFilter, today: LocalDate): DateRange = when (filter) {
        QuickFilter.Today -> DateRange(today, today)
        QuickFilter.Last7Days -> lastDays(7, today)
        QuickFilter.ThisMonth -> monthRange(YearMonth.from(today))
    }

    /** Los últimos [days] días contando hoy (botón "Últimos 30 días" del calendario). */
    fun lastDays(days: Int, today: LocalDate): DateRange =
        DateRange(today.minusDays((days - 1).toLong()), today)

    /** Del primer al último día del mes. */
    fun monthRange(month: YearMonth): DateRange = DateRange(month.atDay(1), month.atEndOfMonth())

    /** El mes calendario anterior completo (para "↑ 12 % vs. mes anterior"). */
    fun previousMonthRange(today: LocalDate): DateRange = monthRange(YearMonth.from(today).minusMonths(1))

    /**
     * Convierte un rango de días a dos instantes [desde, hasta) en la zona del teléfono.
     * Así "hoy" en Colombia va de hoy 00:00 a mañana 00:00 hora Colombia, aunque la
     * base guarde en UTC.
     */
    fun toInstants(range: DateRange, zone: ZoneId): Pair<Instant, Instant> =
        range.start.atStartOfDay(zone).toInstant() to range.end.plusDays(1).atStartOfDay(zone).toInstant()

    /** Rango personalizado del calendario: el fin no puede ser antes del inicio ni pasar de un año. */
    fun validateRange(range: DateRange): ErrorType? = when {
        range.end.isBefore(range.start) -> ErrorType.InvalidDateRange
        ChronoUnit.DAYS.between(range.start, range.end) >= MAX_RANGE_DAYS -> ErrorType.InvalidDateRange
        else -> null
    }

    fun isInRange(movement: Movement, range: DateRange, zone: ZoneId): Boolean =
        movement.localDate(zone)?.let { it in range } ?: false

    // ---------------- Totales y agrupación ----------------

    fun summary(movements: List<Movement>): PeriodSummary = PeriodSummary(
        income = movements.filter { it.isIncome }.sumOf { it.amount },
        expense = movements.filter { it.isExpense }.sumOf { it.amount },
    )

    /**
     * Variación en % frente al período anterior, redondeada. `null` si no hay con qué
     * comparar (el anterior es 0 o negativo), para no mostrar porcentajes absurdos.
     */
    fun percentChange(current: Double, previous: Double): Int? {
        if (previous <= 0.0) return null
        return ((current - previous) / previous * 100).roundToInt()
    }

    /** Más reciente primero (por fecha y hora). */
    fun newestFirst(movements: List<Movement>): List<Movement> =
        movements.sortedByDescending { it.instant ?: Instant.EPOCH }

    /** Agrupa por día (en la zona del teléfono), el día más reciente primero. */
    fun groupByDay(movements: List<Movement>, zone: ZoneId): List<DayGroup> =
        newestFirst(movements)
            .groupBy { it.localDate(zone) }
            .filterKeys { it != null }
            .map { (date, items) -> DayGroup(date!!, items) }
            .sortedByDescending { it.date }

    /** Días que tienen al menos un movimiento (puntos del calendario). */
    fun daysWithMovements(movements: List<Movement>, zone: ZoneId): Set<LocalDate> =
        movements.mapNotNull { it.localDate(zone) }.toSet()

    fun filterByKind(movements: List<Movement>, kind: MovementKind?): List<Movement> =
        if (kind == null) movements else movements.filter { it.movementKind == kind }

    // ---------------- Búsqueda ----------------

    /**
     * Limpia lo que escribe el usuario antes de mandarlo a la base: quita espacios de
     * sobra y los caracteres que tienen significado especial en el filtro de Supabase
     * (`%`, `_`, `,`, paréntesis, `*`…). Así "café, pan" no rompe la consulta.
     */
    fun sanitizeQuery(raw: String): String =
        raw.replace(Regex("""[%_,()*\\:"'.]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_QUERY_LENGTH)

    /** Se busca desde 2 letras (con 1 sola saldría casi todo). */
    fun canSearch(raw: String): Boolean = sanitizeQuery(raw).length >= MIN_QUERY_LENGTH

    /** La misma regla que aplica la base (ilike en título o descripción), para pruebas y filtros locales. */
    fun matches(movement: Movement, query: String): Boolean {
        val q = sanitizeQuery(query)
        if (q.isEmpty()) return true
        return movement.title.contains(q, ignoreCase = true) ||
            (movement.description?.contains(q, ignoreCase = true) ?: false)
    }

    // ---------------- Categorías ----------------

    /**
     * Total por categoría de un tipo (por defecto gastos), de mayor a menor. Las barras se
     * miden contra la categoría más alta para que siempre haya una barra llena.
     */
    fun categoryTotals(movements: List<Movement>, kind: MovementKind = MovementKind.Expense): List<CategoryTotal> {
        val grouped = movements.filter { it.movementKind == kind }.groupBy { it.categoryId }
        val max = grouped.values.maxOfOrNull { list -> list.sumOf { it.amount } } ?: 0.0
        return grouped.map { (categoryId, list) ->
            val total = list.sumOf { it.amount }
            CategoryTotal(
                categoryId = categoryId,
                total = total,
                count = list.size,
                fraction = if (max > 0) (total / max).toFloat().coerceIn(0f, 1f) else 0f,
            )
        }.sortedByDescending { it.total }
    }

    // ---------------- Recurrentes ----------------

    /**
     * Próximo cobro de un movimiento recurrente. Si la base trae `next_charge_on` y aún no
     * ha pasado, se usa ese; si no, se suma la frecuencia a la fecha del movimiento hasta
     * quedar después de hoy. `null` si el movimiento no es recurrente.
     */
    fun nextCharge(movement: Movement, today: LocalDate, zone: ZoneId): LocalDate? {
        val recurrence = movement.recurrenceType ?: return null
        parseDate(movement.nextChargeOn)?.let { if (it.isAfter(today)) return it }
        var date = movement.localDate(zone) ?: return null
        while (!date.isAfter(today)) {
            date = when (recurrence) {
                Recurrence.Weekly -> date.plusWeeks(1)
                Recurrence.Monthly -> date.plusMonths(1)
                Recurrence.Yearly -> date.plusYears(1)
            }
        }
        return date
    }

    // ---------------- Dai te ayuda ----------------

    /**
     * Qué tanto pesa este movimiento dentro del mes en que ocurrió:
     * 1. Gasto con categoría que tiene límite mensual → % del presupuesto.
     * 2. Gasto con categoría sin límite → % de lo gastado en esa categoría ese mes.
     * 3. Gasto sin categoría → % de todos los gastos del mes.
     * 4. Ingreso → % de los ingresos del mes.
     * [monthMovements] son los movimientos del mismo mes (incluido este).
     */
    fun insight(
        movement: Movement,
        category: MovementCategory?,
        monthMovements: List<Movement>,
    ): DaiInsight? {
        fun pct(part: Double, whole: Double): Int? =
            if (whole <= 0.0) null else (part / whole * 100).roundToInt().coerceAtLeast(1)

        if (movement.isIncome) {
            val income = monthMovements.filter { it.isIncome }.sumOf { it.amount }
            return pct(movement.amount, income)?.let { DaiInsight.OfMonthIncome(it) }
        }
        val limit = category?.monthlyLimit
        if (category != null && limit != null && limit > 0.0) {
            return pct(movement.amount, limit)?.let { DaiInsight.OfBudget(it, category.name) }
        }
        if (category != null) {
            val spent = monthMovements.filter { it.isExpense && it.categoryId == category.id }.sumOf { it.amount }
            return pct(movement.amount, spent)?.let { DaiInsight.OfCategorySpending(it, category.name) }
        }
        val spent = monthMovements.filter { it.isExpense }.sumOf { it.amount }
        return pct(movement.amount, spent)?.let { DaiInsight.OfMonthSpending(it) }
    }
}
