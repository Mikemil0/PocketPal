package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.Budget
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Repositorio de Dai **en memoria** para las pruebas. Arma el resumen financiero con el
 * mismo [DaiSnapshots] que usa la app, así las pruebas miden las reglas reales.
 */
class FakeDaiRepository(private val zone: ZoneId = ZONE) : DaiRepository {

    val messages = mutableListOf<DaiMessage>()
    val movements = mutableListOf<Movement>()
    val categories = mutableListOf<MovementCategory>()
    val budgets = mutableListOf<Budget>()
    val goals = mutableListOf<Goal>()
    private var nextId = 0

    /** Si es `true`, la próxima llamada falla como si no hubiera internet. */
    var failNextWithNetwork = false

    /** Si es `true`, solo la próxima lectura de datos falla (el mensaje sí se guarda). */
    var failNextOnSnapshot = false

    /** Cuántas veces se pidió el resumen financiero (para ver que se reutiliza). */
    var snapshotCalls = 0

    fun addCategory(id: String, name: String, kind: MovementKind = MovementKind.Expense): MovementCategory =
        MovementCategory(id, kind.wire, name, "restaurant", "#FF9B9B").also(categories::add)

    fun addExpense(title: String, amount: Double, date: LocalDate, categoryId: String?, hour: Int = 10): Movement =
        addMovement(title, amount, date, categoryId, MovementKind.Expense, hour)

    fun addIncome(title: String, amount: Double, date: LocalDate): Movement =
        addMovement(title, amount, date, null, MovementKind.Income)

    private fun addMovement(title: String, amount: Double, date: LocalDate, categoryId: String?, kind: MovementKind, hour: Int = 10): Movement {
        val m = Movement(
            id = "m${nextId++}",
            kind = kind.wire,
            title = title,
            amount = amount,
            categoryId = categoryId,
            occurredAt = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toOffsetDateTime().toString(),
        )
        movements.add(m)
        return m
    }

    fun addBudget(categoryId: String, name: String, limit: Double): Budget =
        Budget("b${nextId++}", categoryId, name, limit, BudgetPeriod.Monthly.wire).also(budgets::add)

    fun addGoal(name: String, saved: Double, target: Double, completed: Boolean = false): Goal =
        Goal("g${nextId++}", name, "savings", "#55A5EF", target, saved, completedAt = if (completed) "2026-10-01T00:00:00Z" else null)
            .also(goals::add)

    override suspend fun history(): List<DaiMessage> {
        checkNetwork()
        return messages.toList()
    }

    override suspend fun save(role: DaiRole, text: String, card: DaiCard?): DaiMessage {
        checkNetwork()
        val message = DaiMessage("d${nextId++}", role, text, card, DaiBrain.chipsFor(card), LocalDateTime.of(2026, 10, 27, 9, 41))
        messages.add(message)
        return message
    }

    override suspend fun clearHistory() {
        checkNetwork()
        messages.clear()
    }

    override suspend fun snapshot(today: LocalDate, zone: ZoneId, weekStart: Int, userName: String): FinanceSnapshot {
        checkNetwork()
        if (failNextOnSnapshot) {
            failNextOnSnapshot = false
            throw java.io.IOException("sin red")
        }
        snapshotCalls++
        return DaiSnapshots.build(today, zone, weekStart, userName, movements, categories, budgets, goals)
    }

    override suspend fun saveReceipt(draft: ReceiptDraft, zone: ZoneId): Movement {
        checkNetwork()
        val m = addMovement(draft.merchant, draft.amount ?: 0.0, draft.date, draft.categoryId, MovementKind.Expense, hour = 12)
            .copy(description = draft.reason)
        movements[movements.lastIndex] = m
        return m
    }

    private fun checkNetwork() {
        if (failNextWithNetwork) {
            failNextWithNetwork = false
            throw java.io.IOException("sin red")
        }
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/Bogota")
    }
}
