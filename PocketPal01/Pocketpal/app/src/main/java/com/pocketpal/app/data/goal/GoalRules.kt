package com.pocketpal.app.data.goal

import com.pocketpal.app.auth.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Reglas de negocio de Metas (SDD Módulo 3). Son funciones puras: no usan Android
 * ni red, así se prueban con pruebas unitarias (`GoalRulesTest`).
 *
 * - RN-M1: nombre obligatorio, de 1 a 30 caracteres y único por usuario.
 * - RN-M2: monto de la meta mayor que 0.
 * - RN-M3: "Ya tengo ahorrado" opcional y menor que el monto.
 * - RN-M4: la fecha límite, si existe, es posterior a hoy.
 * - RN-M5: al editar, el monto no puede quedar por debajo de lo ya ahorrado.
 * - RN-M6: un aporte es mayor que 0, no tiene fecha futura y su motivo tiene máximo 80 caracteres.
 * - RN-M7: aportar más de lo que falta está permitido (la meta queda completada).
 */
object GoalRules {

    const val MAX_NAME_LENGTH = 30
    const val MAX_NOTE_LENGTH = 80

    /** Días que se toman como "un mes" para calcular el ahorro sugerido. */
    private const val DAYS_PER_MONTH = 30.0

    // ---------------- Nombre (RN-M1) ----------------

    fun normalize(name: String): String = name.trim().lowercase()

    fun isDuplicateName(name: String, existing: List<GoalNamed>, excludeId: String? = null): Boolean {
        val normalized = normalize(name)
        return existing.any { it.id != excludeId && normalize(it.name) == normalized }
    }

    // ---------------- Progreso ----------------

    /** Avance entre 0.0 y 1.0 (para la barra de progreso). */
    fun progress(saved: Double, target: Double): Float =
        if (target <= 0.0) 0f else (saved / target).coerceIn(0.0, 1.0).toFloat()

    /**
     * Porcentaje entero **redondeado hacia abajo** (15 %, como el Figma), para que una meta
     * a la que le falta poco nunca muestre 100 % antes de estar completada.
     */
    fun percent(saved: Double, target: Double): Int =
        if (target <= 0.0) 0 else floor((saved / target) * 100).toInt().coerceIn(0, 100)

    /** Lo que falta para cumplir la meta (nunca negativo). */
    fun remaining(saved: Double, target: Double): Double = (target - saved).coerceAtLeast(0.0)

    fun isCompleted(saved: Double, target: Double): Boolean = target > 0.0 && saved >= target

    // ---------------- Fechas ----------------

    /** Días entre hoy y la fecha límite (negativo si ya pasó). */
    fun daysLeft(today: LocalDate, deadline: LocalDate): Long = ChronoUnit.DAYS.between(today, deadline)

    /**
     * Meses de ahorro que quedan: se redondea hacia arriba y mínimo 1
     * (93 días = 4 meses: septiembre, octubre, noviembre y diciembre).
     */
    fun monthsLeft(today: LocalDate, deadline: LocalDate): Int {
        val days = daysLeft(today, deadline)
        if (days <= 0) return 1
        return ceil(days / DAYS_PER_MONTH).toInt().coerceAtLeast(1)
    }

    /**
     * "Ahorra $X/mes para llegar a tiempo". `null` si la meta no tiene fecha límite o ya
     * está completada. Se redondea hacia arriba a pesos enteros para no quedarse corto.
     */
    fun suggestedMonthly(saved: Double, target: Double, today: LocalDate, deadline: LocalDate?): Double? {
        if (deadline == null || isCompleted(saved, target)) return null
        val months = monthsLeft(today, deadline)
        return ceil(remaining(saved, target) / months)
    }

    /** Lee una fecha ISO (`2026-12-15`) de la base; `null` si viene vacía o mal formada. */
    fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.take(10))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    // ---------------- Validaciones ----------------

    /**
     * Valida el formulario de crear/editar meta. Devuelve el **primer** error encontrado o
     * `null` si todo está bien.
     *
     * @param currentSaved lo ya ahorrado si se está **editando** (RN-M5); `null` al crear.
     */
    fun validateGoal(
        draft: GoalDraft,
        existing: List<GoalNamed>,
        today: LocalDate,
        excludeId: String? = null,
        currentSaved: Double? = null,
    ): ErrorType? {
        val name = draft.name.trim()
        return when {
            name.isEmpty() -> ErrorType.GoalNameEmpty
            name.length > MAX_NAME_LENGTH -> ErrorType.GoalNameTooLong
            isDuplicateName(name, existing, excludeId) -> ErrorType.DuplicateGoalName
            draft.targetAmount <= 0.0 -> ErrorType.InvalidGoalAmount
            currentSaved == null && draft.initialSaved < 0.0 -> ErrorType.InitialSavedTooHigh
            currentSaved == null && draft.initialSaved >= draft.targetAmount -> ErrorType.InitialSavedTooHigh
            draft.deadline != null && !draft.deadline.isAfter(today) -> ErrorType.DeadlineInPast
            currentSaved != null && draft.targetAmount < currentSaved -> ErrorType.GoalAmountBelowSaved
            else -> null
        }
    }

    /** Valida un aporte (RN-M6). Devuelve el primer error o `null`. */
    fun validateContribution(amount: Double, date: LocalDate, today: LocalDate, note: String?): ErrorType? =
        when {
            amount <= 0.0 -> ErrorType.InvalidContributionAmount
            date.isAfter(today) -> ErrorType.ContributionDateInFuture
            (note?.trim()?.length ?: 0) > MAX_NOTE_LENGTH -> ErrorType.ContributionNoteTooLong
            else -> null
        }

    // ---------------- Orden de la lista (filtro) ----------------

    /**
     * Ordena según el filtro elegido:
     * - [GoalSort.Manual]: el orden que el usuario dejó arrastrando (`position`).
     * - [GoalSort.Closest]: primero las que tienen mayor porcentaje.
     * - [GoalSort.Deadline]: primero la fecha límite más próxima; sin fecha, al final.
     * - [GoalSort.Recent]: primero las creadas más recientemente.
     */
    fun sort(goals: List<Goal>, sort: GoalSort): List<Goal> = when (sort) {
        GoalSort.Manual -> goals.sortedBy { it.position }
        GoalSort.Closest -> goals.sortedByDescending { progress(it.savedAmount, it.targetAmount) }
        GoalSort.Deadline -> goals.sortedWith(
            compareBy<Goal> { it.deadlineDate == null }.thenBy { it.deadlineDate }
        )
        GoalSort.Recent -> goals.sortedByDescending { it.createdAt.orEmpty() }
    }
}

/** Opciones del ícono de filtro de la lista de metas. */
enum class GoalSort { Manual, Closest, Deadline, Recent }
