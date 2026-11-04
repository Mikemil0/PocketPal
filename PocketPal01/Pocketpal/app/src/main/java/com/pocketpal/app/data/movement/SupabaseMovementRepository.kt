package com.pocketpal.app.data.movement

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Movimientos en Supabase sobre `public.movements` (con RLS: cada usuario solo ve lo
 * suyo, por eso aquí no hace falta filtrar por `user_id`).
 *
 * Las fechas se mandan como instantes ISO en UTC (`2026-10-25T05:00:00Z`): la base
 * compara `timestamptz` correctamente sin importar la zona.
 */
class SupabaseMovementRepository(private val supabase: SupabaseClient) : MovementRepository {

    override suspend fun list(from: Instant, toExclusive: Instant): List<Movement> =
        supabase.postgrest.from(MOVEMENTS)
            .select {
                filter {
                    gte("occurred_at", from.toString())
                    lt("occurred_at", toExclusive.toString())
                }
                order("occurred_at", Order.DESCENDING)
            }
            .decodeList<Movement>()

    override suspend fun recent(limit: Int): List<Movement> =
        supabase.postgrest.from(MOVEMENTS)
            .select {
                order("occurred_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<Movement>()

    override suspend fun search(query: String, kind: MovementKind?, limit: Int): List<Movement> {
        val q = MovementRules.sanitizeQuery(query)
        if (q.length < MovementRules.MIN_QUERY_LENGTH) return emptyList()
        return supabase.postgrest.from(MOVEMENTS)
            .select {
                filter {
                    // title ILIKE '%q%' OR description ILIKE '%q%'
                    or {
                        ilike("title", "%$q%")
                        ilike("description", "%$q%")
                    }
                    if (kind != null) eq("kind", kind.wire)
                }
                order("occurred_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<Movement>()
    }

    override suspend fun get(id: String): Movement? =
        supabase.postgrest.from(MOVEMENTS)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    override suspend fun categories(): List<MovementCategory> =
        supabase.postgrest.from(CATEGORIES)
            .select { order("position", Order.ASCENDING) }
            .decodeList<MovementCategory>()

    /**
     * Módulo 6 · Flujo 5: `insert into movements`. El id se genera aquí para leer el
     * movimiento recién creado. Si es de hoy se guarda con la hora actual; si es de otro
     * día, al mediodía (así no cambia de día por la zona horaria).
     */
    override suspend fun create(movement: NewMovement, zone: ZoneId): Movement {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("no session")
        val id = UUID.randomUUID().toString()
        val time = if (movement.date == LocalDate.now(zone)) LocalTime.now(zone) else LocalTime.NOON
        supabase.postgrest.from(MOVEMENTS).insert(
            buildJsonObject {
                put("id", id)
                put("user_id", userId)
                put("kind", movement.kind.wire)
                put("title", movement.title.trim().take(NewMovement.TITLE_MAX))
                put("amount", movement.amount)
                if (movement.categoryId != null) put("category_id", movement.categoryId)
                if (movement.note.isNotBlank()) put("description", movement.note.trim().take(NewMovement.NOTE_MAX))
                put("occurred_at", movement.date.atTime(time).atZone(zone).toOffsetDateTime().toString())
                put("source", movement.source)
            }
        )
        return get(id) ?: throw IllegalStateException("movement not found: $id")
    }

    companion object {
        const val MOVEMENTS = "movements"
        const val CATEGORIES = "categories"
    }
}
