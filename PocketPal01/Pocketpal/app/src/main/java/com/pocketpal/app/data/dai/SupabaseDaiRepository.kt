package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.BudgetRepository
import com.pocketpal.app.data.goal.GoalRepository
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.MovementRules
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * Dai sobre Supabase:
 * - la conversación en `public.dai_messages` (RLS: cada quien ve la suya);
 * - los datos para responder salen de los repositorios que ya existían
 *   (movimientos, presupuestos y metas), así no se repiten consultas;
 * - la factura aprobada se inserta en `public.movements` con `source = 'dai'`.
 */
class SupabaseDaiRepository(
    private val supabase: SupabaseClient,
    private val movements: MovementRepository,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
) : DaiRepository {

    override suspend fun history(): List<DaiMessage> =
        supabase.postgrest.from(MESSAGES)
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<MessageRow>()
            .map { it.toDomain() }

    override suspend fun save(role: DaiRole, text: String, card: DaiCard?): DaiMessage {
        val userId = currentUserId() ?: throw IllegalStateException("no session")
        val id = UUID.randomUUID().toString()
        supabase.postgrest.from(MESSAGES).insert(
            buildJsonObject {
                put("id", id)
                put("user_id", userId)
                put("role", role.wire)
                put("text", text.take(MAX_TEXT))
                if (card != null) put("card", DaiCardJson.encode(card))
            }
        )
        return DaiMessage(id, role, text, card, DaiBrain.chipsFor(card), java.time.LocalDateTime.now())
    }

    override suspend fun clearHistory() {
        // Sin filtro de usuario: la política RLS solo deja borrar las filas propias.
        // (Postgrest exige algún filtro en un delete, por eso "role in (user, dai)".)
        supabase.postgrest.from(MESSAGES).delete {
            filter { isIn("role", listOf(DaiRole.User.wire, DaiRole.Dai.wire)) }
        }
    }

    override suspend fun snapshot(today: LocalDate, zone: ZoneId, weekStart: Int, userName: String): FinanceSnapshot {
        // UNA consulta de movimientos: del 1 del mes pasado al último día de este mes.
        val month = YearMonth.from(today)
        val range = MovementRules.monthRange(month.minusMonths(1)).copy(end = month.atEndOfMonth())
        val (from, to) = MovementRules.toInstants(range, zone)
        return DaiSnapshots.build(
            today = today,
            zone = zone,
            weekStart = weekStart,
            userName = userName,
            movements = movements.list(from, to),
            categories = movements.categories(),
            budgets = budgets.list(),
            goals = goals.list(),
        )
    }

    override suspend fun saveReceipt(draft: ReceiptDraft, zone: ZoneId): Movement {
        val userId = currentUserId() ?: throw IllegalStateException("no session")
        val id = UUID.randomUUID().toString()
        // Si la factura es de hoy se usa la hora actual; si es de otro día, el mediodía.
        val time = if (draft.date == LocalDate.now(zone)) LocalTime.now(zone) else LocalTime.NOON
        val occurredAt = draft.date.atTime(time).atZone(zone).toOffsetDateTime().toString()
        supabase.postgrest.from(MOVEMENTS).insert(
            buildJsonObject {
                put("id", id)
                put("user_id", userId)
                put("kind", MovementKind.Expense.wire)
                put("title", draft.merchant.trim().take(ReceiptParser.MERCHANT_MAX))
                put("amount", draft.amount ?: 0.0)
                if (draft.categoryId != null) put("category_id", draft.categoryId)
                if (draft.reason.isNotBlank()) put("description", draft.reason.trim().take(MAX_DESCRIPTION))
                put("occurred_at", occurredAt)
                put("source", SOURCE_DAI)
            }
        )
        return movements.get(id) ?: throw IllegalStateException("movement not found: $id")
    }

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Fila de `dai_messages` tal como llega de Supabase. */
    @Serializable
    private data class MessageRow(
        @SerialName("id") val id: String,
        @SerialName("role") val role: String,
        @SerialName("text") val text: String,
        @SerialName("card") val card: JsonElement? = null,
        @SerialName("created_at") val createdAt: String? = null,
    ) {
        fun toDomain(): DaiMessage {
            val decoded = card?.let(DaiCardJson::decode)
            return DaiMessage(
                id = id,
                role = DaiRole.fromWire(role),
                text = text,
                card = decoded,
                chips = DaiBrain.chipsFor(decoded),
                createdAt = createdAt?.let {
                    runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()
                },
            )
        }
    }

    companion object {
        const val MESSAGES = "dai_messages"
        const val MOVEMENTS = "movements"
        const val SOURCE_DAI = "dai"
        const val MAX_TEXT = 2000
        const val MAX_DESCRIPTION = 200
    }
}

/**
 * Convierte la tarjeta de Dai a JSON y de vuelta (columna `card` jsonb). El campo
 * "type" dice qué tarjeta es ("spending", "goals", "alert"…).
 */
object DaiCardJson {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(card: DaiCard): JsonElement = json.encodeToJsonElement(DaiCard.serializer(), card)

    /** Si la tarjeta viene dañada o es de una versión vieja, se muestra solo el texto. */
    fun decode(element: JsonElement): DaiCard? =
        runCatching { json.decodeFromJsonElement(DaiCard.serializer(), element) }.getOrNull()
}
