package com.pocketpal.app.data.goal

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

/**
 * Metas en Supabase sobre `public.goals` y `public.goal_contributions` (ambas con RLS:
 * cada usuario solo ve y modifica lo suyo).
 *
 * Lo ahorrado (`saved_amount`) y la marca de completada (`completed_at`) los calculan
 * los triggers de la base; por eso, después de escribir, se vuelve a leer la meta.
 */
class SupabaseGoalRepository(private val supabase: SupabaseClient) : GoalRepository {

    override suspend fun list(): List<Goal> =
        supabase.postgrest.from(GOALS)
            .select { order("position", Order.ASCENDING) }
            .decodeList<Goal>()
            .sortedBy { it.position }

    override suspend fun get(id: String): Goal? =
        supabase.postgrest.from(GOALS)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    override suspend fun create(draft: GoalDraft, position: Int): Goal {
        val userId = currentUserId() ?: throw IllegalStateException("no session")
        // El id se genera en el cliente para poder registrar el aporte inicial y abrir
        // la pantalla "Meta creada" sin otra consulta de búsqueda.
        val id = UUID.randomUUID().toString()
        try {
            supabase.postgrest.from(GOALS).insert(
                buildJsonObject {
                    put("id", id)
                    put("user_id", userId)
                    put("name", draft.name)
                    put("icon", draft.icon)
                    put("color", draft.color)
                    put("target_amount", draft.targetAmount)
                    if (draft.deadline != null) put("deadline", draft.deadline.toString())
                    put("position", position)
                }
            )
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        if (draft.initialSaved > 0.0) {
            insertContribution(
                goalId = id,
                userId = userId,
                amount = draft.initialSaved,
                date = LocalDate.now(),
                note = null,
                kind = GoalContribution.KIND_INITIAL,
            )
        }
        return get(id) ?: throw IllegalStateException("goal not found: $id")
    }

    override suspend fun update(id: String, draft: GoalDraft): Goal {
        try {
            supabase.postgrest.from(GOALS).update(
                {
                    set("name", draft.name)
                    set("icon", draft.icon)
                    set("color", draft.color)
                    set("target_amount", draft.targetAmount)
                    if (draft.deadline != null) set("deadline", draft.deadline.toString())
                    else setToNull("deadline")
                }
            ) { filter { eq("id", id) } }
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        return get(id) ?: throw IllegalStateException("goal not found: $id")
    }

    override suspend fun delete(id: String) {
        supabase.postgrest.from(GOALS).delete { filter { eq("id", id) } }
    }

    override suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            supabase.postgrest.from(GOALS).update(
                { set("position", index) }
            ) { filter { eq("id", id) } }
        }
    }

    override suspend fun contributions(goalId: String): List<GoalContribution> =
        supabase.postgrest.from(CONTRIBUTIONS)
            .select {
                filter { eq("goal_id", goalId) }
                order("contributed_on", Order.DESCENDING)
            }
            .decodeList<GoalContribution>()
            // Mismo día: el registrado de último va primero.
            .sortedWith(compareByDescending<GoalContribution> { it.contributedOn }.thenByDescending { it.createdAt.orEmpty() })

    override suspend fun addContribution(goalId: String, amount: Double, date: LocalDate, note: String?): Goal {
        val userId = currentUserId() ?: throw IllegalStateException("no session")
        insertContribution(goalId, userId, amount, date, note, GoalContribution.KIND_MANUAL)
        return get(goalId) ?: throw IllegalStateException("goal not found: $goalId")
    }

    private suspend fun insertContribution(
        goalId: String,
        userId: String,
        amount: Double,
        date: LocalDate,
        note: String?,
        kind: String,
    ) {
        supabase.postgrest.from(CONTRIBUTIONS).insert(
            buildJsonObject {
                put("goal_id", goalId)
                put("user_id", userId)
                put("amount", amount)
                put("contributed_on", date.toString())
                val cleanNote = note?.trim()
                if (!cleanNote.isNullOrEmpty()) put("note", cleanNote)
                put("kind", kind)
            }
        )
    }

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Traduce el error de la base al dominio (la UI no ve Supabase). */
    private fun PostgrestRestException.asDomain(): Exception =
        if (code == UNIQUE_VIOLATION) DuplicateGoalNameException() else this

    companion object {
        const val GOALS = "goals"
        const val CONTRIBUTIONS = "goal_contributions"

        /** SQLSTATE del índice único `goals_unique_name` (RN-M1). */
        const val UNIQUE_VIOLATION = "23505"
    }
}
