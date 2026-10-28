package com.pocketpal.app.data.budget

import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * Presupuestos en Supabase sobre `public.budgets` (con RLS: cada usuario solo ve y
 * modifica los suyos). Lo gastado se calcula con `public.movements` (Módulo 4).
 */
class SupabaseBudgetRepository(private val supabase: SupabaseClient) : BudgetRepository {

    override suspend fun list(): List<Budget> =
        supabase.postgrest.from(BUDGETS)
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<Budget>()

    override suspend fun get(id: String): Budget? =
        supabase.postgrest.from(BUDGETS)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    override suspend fun create(draft: BudgetDraft): Budget {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("no session")
        // El id se genera aquí para abrir "Presupuesto creado" sin buscarlo otra vez.
        val id = UUID.randomUUID().toString()
        try {
            supabase.postgrest.from(BUDGETS).insert(
                buildJsonObject {
                    put("id", id)
                    put("user_id", userId)
                    put("category_id", draft.categoryId)
                    put("name", draft.name.trim())
                    put("amount_limit", draft.amountLimit)
                    put("period", draft.period.wire)
                    put("alert_enabled", draft.alertEnabled)
                }
            )
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        return get(id) ?: throw IllegalStateException("budget not found: $id")
    }

    override suspend fun update(id: String, draft: BudgetDraft): Budget {
        try {
            supabase.postgrest.from(BUDGETS).update(
                {
                    set("category_id", draft.categoryId)
                    set("name", draft.name.trim())
                    set("amount_limit", draft.amountLimit)
                    set("period", draft.period.wire)
                    set("alert_enabled", draft.alertEnabled)
                }
            ) { filter { eq("id", id) } }
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        return get(id) ?: throw IllegalStateException("budget not found: $id")
    }

    override suspend fun delete(id: String) {
        supabase.postgrest.from(BUDGETS).delete { filter { eq("id", id) } }
    }

    override suspend fun categories(): List<MovementCategory> =
        supabase.postgrest.from(CATEGORIES)
            .select {
                filter { eq("kind", MovementKind.Expense.wire) }
                order("position", Order.ASCENDING)
            }
            .decodeList<MovementCategory>()

    override suspend fun expenses(from: Instant, toExclusive: Instant, categoryId: String?): List<Movement> =
        supabase.postgrest.from(MOVEMENTS)
            .select {
                filter {
                    eq("kind", MovementKind.Expense.wire)
                    gte("occurred_at", from.toString())
                    lt("occurred_at", toExclusive.toString())
                    if (categoryId != null) eq("category_id", categoryId)
                }
                order("occurred_at", Order.DESCENDING)
            }
            .decodeList<Movement>()

    override suspend fun alertThreshold(): Int =
        try {
            supabase.postgrest.from(NOTIFICATION_SETTINGS)
                .select()
                .decodeSingleOrNull<ThresholdRow>()
                ?.budgetThreshold
                ?: BudgetRules.DEFAULT_THRESHOLD
        } catch (e: Exception) {
            // Si no se puede leer, el aviso usa el valor por defecto (80 %).
            BudgetRules.DEFAULT_THRESHOLD
        }

    /** Solo la columna que se necesita de `notification_settings`. */
    @Serializable
    private data class ThresholdRow(@SerialName("budget_threshold") val budgetThreshold: Int = BudgetRules.DEFAULT_THRESHOLD)

    /** Traduce el error de la base al dominio (la UI no ve Supabase). */
    private fun PostgrestRestException.asDomain(): Exception =
        if (code == UNIQUE_VIOLATION) DuplicateBudgetException() else this

    companion object {
        const val BUDGETS = "budgets"
        const val CATEGORIES = "categories"
        const val MOVEMENTS = "movements"
        const val NOTIFICATION_SETTINGS = "notification_settings"

        /** SQLSTATE del índice único `budgets_one_per_category`. */
        const val UNIQUE_VIOLATION = "23505"
    }
}
