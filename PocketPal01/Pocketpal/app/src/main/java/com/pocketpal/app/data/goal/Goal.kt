package com.pocketpal.app.data.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Identidad mínima para la regla de nombre único (RN-M1): sirve para la fila de
 * la base ([Goal]) y para cualquier fila de la UI.
 */
interface GoalNamed {
    val id: String
    val name: String
}

/**
 * Fila de `public.goals` (migración `20261021000000_module3_goals.sql`).
 *
 * - [savedAmount] lo calcula la base sumando los aportes (trigger `recalc_goal_saved`):
 *   la app nunca lo escribe.
 * - [completedAt] lo pone la base cuando lo ahorrado alcanza el monto (trigger
 *   `set_goal_completed`): `null` = meta activa.
 * - [deadline] es una fecha ISO (`2026-12-15`) o `null` si la meta no tiene fecha límite.
 */
@Serializable
data class Goal(
    @SerialName("id") override val id: String,
    @SerialName("name") override val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("saved_amount") val savedAmount: Double = 0.0,
    @SerialName("deadline") val deadline: String? = null,
    @SerialName("position") val position: Int = 0,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) : GoalNamed {
    val isCompleted: Boolean get() = completedAt != null
    val deadlineDate: LocalDate? get() = GoalRules.parseDate(deadline)
}

/**
 * Fila de `public.goal_contributions`: un aporte a una meta.
 * [kind] = `initial` para el "Ya tengo ahorrado" con el que se creó la meta,
 * `manual` para los aportes que se registran después.
 */
@Serializable
data class GoalContribution(
    @SerialName("id") val id: String,
    @SerialName("goal_id") val goalId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("contributed_on") val contributedOn: String,
    @SerialName("note") val note: String? = null,
    @SerialName("kind") val kind: String = KIND_MANUAL,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val date: LocalDate? get() = GoalRules.parseDate(contributedOn)
    val isInitial: Boolean get() = kind == KIND_INITIAL

    companion object {
        const val KIND_INITIAL = "initial"
        const val KIND_MANUAL = "manual"
    }
}

/**
 * Datos del formulario crear/editar meta.
 * [initialSaved] solo se usa al **crear** ("Ya tengo ahorrado (opcional)"); al editar
 * se ignora porque lo ahorrado solo cambia con aportes.
 */
data class GoalDraft(
    val name: String,
    val icon: String,
    val color: String,
    val targetAmount: Double,
    val initialSaved: Double = 0.0,
    /** `null` = sin fecha límite (interruptor "Poner fecha límite" apagado). */
    val deadline: LocalDate? = null,
)
