package com.pocketpal.app.data.category

import com.pocketpal.app.data.category.CategoryRules.normalize
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementRules
import kotlinx.coroutines.CancellationException
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Categorías en Supabase sobre la tabla `public.categories` (ya creada en la
 * Fase 2, con RLS "own categories" y el índice único `categories_unique_name`).
 *
 * Solo puede tocar las filas del usuario con sesión porque la política RLS
 * filtra por `auth.uid() = user_id`. La restricción única de la base es la
 * **autoridad** de RN-02: si se inserta un nombre duplicado, Postgres responde
 * `23505` y aquí se traduce a [DuplicateCategoryNameException].
 */
class SupabaseCategoryRepository(private val supabase: SupabaseClient) : CategoryRepository {

    override suspend fun list(kind: CategoryKind): List<Category> =
        supabase.postgrest.from(CATEGORIES)
            .select {
                filter { eq("kind", kind.wire) }
                order("position", Order.ASCENDING)
            }
            .decodeList<Category>()
            .sortedBy { it.position }

    override suspend fun get(id: String): Category? =
        supabase.postgrest.from(CATEGORIES)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    override suspend fun create(draft: CategoryDraft, position: Int): Category {
        val userId = currentUserId() ?: throw IllegalStateException("no session")
        // El id se genera en el cliente para poder volver a la lista sin otra consulta.
        val id = UUID.randomUUID().toString()
        try {
            supabase.postgrest.from(CATEGORIES).insert(
                buildJsonObject {
                    put("id", id)
                    put("user_id", userId)
                    put("kind", draft.kind.wire)
                    put("name", draft.name)
                    put("icon", draft.icon)
                    put("color", draft.color)
                    if (draft.monthlyLimit != null) put("monthly_limit", draft.monthlyLimit)
                    put("position", position)
                }
            )
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        return Category(
            id = id,
            kind = draft.kind.wire,
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            monthlyLimit = draft.monthlyLimit,
            position = position,
        )
    }

    override suspend fun update(id: String, draft: CategoryDraft): Category {
        try {
            supabase.postgrest.from(CATEGORIES).update(
                {
                    set("name", draft.name)
                    set("icon", draft.icon)
                    set("color", draft.color)
                    if (draft.monthlyLimit != null) set("monthly_limit", draft.monthlyLimit)
                    else setToNull("monthly_limit")
                }
            ) { filter { eq("id", id) } }
        } catch (e: PostgrestRestException) {
            throw e.asDomain()
        }
        return get(id) ?: throw IllegalStateException("category not found: $id")
    }

    override suspend fun delete(id: String) {
        supabase.postgrest.from(CATEGORIES).delete { filter { eq("id", id) } }
    }

    override suspend fun reorder(kind: CategoryKind, orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            supabase.postgrest.from(CATEGORIES).update(
                { set("position", index) }
            ) { filter { eq("id", id) } }
        }
    }

    override suspend fun nameExists(kind: CategoryKind, name: String, excludeId: String?): Boolean =
        list(kind).any { it.id != excludeId && normalize(it.name) == normalize(name) }

    /**
     * Módulo 4: cuántos movimientos y cuánto suma cada categoría **en el mes actual**
     * (el mismo período del límite mensual). Si la consulta falla, la lista de
     * categorías igual se muestra, con 0 / $0.
     */
    override suspend fun getCategoryTotals(): Map<String, CategoryStats> {
        val zone = ZoneId.systemDefault()
        val (from, to) = MovementRules.toInstants(MovementRules.monthRange(YearMonth.now(zone)), zone)
        val movements = try {
            supabase.postgrest.from("movements")
                .select {
                    filter {
                        gte("occurred_at", from.toString())
                        lt("occurred_at", to.toString())
                    }
                }
                .decodeList<Movement>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyMap()
        }
        return movements
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> CategoryStats(movementCount = list.size, total = list.sumOf { it.amount }) }
    }

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Traduce el error de la base al dominio (RNF-02: la UI no ve Supabase). */
    private fun PostgrestRestException.asDomain(): Exception =
        if (code == UNIQUE_VIOLATION) DuplicateCategoryNameException() else this

    companion object {
        const val CATEGORIES = "categories"

        /** SQLSTATE del índice único `categories_unique_name` (RN-02). */
        const val UNIQUE_VIOLATION = "23505"
    }
}
