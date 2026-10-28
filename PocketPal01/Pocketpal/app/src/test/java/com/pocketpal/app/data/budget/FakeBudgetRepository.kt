package com.pocketpal.app.data.budget

import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRules
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Repositorio de presupuestos **en memoria** para las pruebas de los ViewModels.
 * Aplica las mismas reglas que la base: un presupuesto por categoría (índice único) y
 * gastos filtrados por fecha y categoría. Lo usan las pruebas de todos los flujos.
 */
class FakeBudgetRepository(private val zone: ZoneId = ZONE) : BudgetRepository {

    val budgets = mutableListOf<Budget>()
    val categoryList = mutableListOf<MovementCategory>()
    val movements = mutableListOf<Movement>()
    var threshold = BudgetRules.DEFAULT_THRESHOLD
    private var nextId = 0

    /** Si es `true`, la próxima llamada falla como si no hubiera internet. */
    var failNextWithNetwork = false

    /** Si es `true`, el próximo guardado choca con el índice único (otro teléfono ya lo creó). */
    var failNextWithDuplicate = false

    fun addCategory(id: String, name: String, kind: MovementKind = MovementKind.Expense, monthlyLimit: Double? = null): MovementCategory {
        val category = MovementCategory(id, kind.wire, name, "restaurant", "#FF9B9B", monthlyLimit)
        categoryList.add(category)
        return category
    }

    fun addBudget(
        categoryId: String,
        name: String,
        limit: Double,
        period: BudgetPeriod = BudgetPeriod.Monthly,
        alert: Boolean = false,
    ): Budget {
        val budget = Budget("b${nextId++}", categoryId, name, limit, period.wire, alert)
        budgets.add(budget)
        return budget
    }

    /** Agrega un gasto (o ingreso) el [date] a las [hour]:00, hora de Colombia. */
    fun addMovement(
        title: String,
        amount: Double,
        date: LocalDate,
        categoryId: String?,
        kind: MovementKind = MovementKind.Expense,
        hour: Int = 10,
    ): Movement {
        val movement = Movement(
            id = "m${nextId++}",
            kind = kind.wire,
            title = title,
            amount = amount,
            categoryId = categoryId,
            occurredAt = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toOffsetDateTime().toString(),
        )
        movements.add(movement)
        return movement
    }

    override suspend fun list(): List<Budget> {
        checkNetwork()
        return budgets.toList()
    }

    override suspend fun get(id: String): Budget? {
        checkNetwork()
        return budgets.firstOrNull { it.id == id }
    }

    override suspend fun create(draft: BudgetDraft): Budget {
        checkNetwork()
        checkUnique(draft.categoryId, excludeId = null)
        return addBudget(draft.categoryId!!, draft.name.trim(), draft.amountLimit, draft.period, draft.alertEnabled)
    }

    override suspend fun update(id: String, draft: BudgetDraft): Budget {
        checkNetwork()
        checkUnique(draft.categoryId, excludeId = id)
        val index = budgets.indexOfFirst { it.id == id }
        check(index >= 0) { "budget not found: $id" }
        val updated = budgets[index].copy(
            categoryId = draft.categoryId!!,
            name = draft.name.trim(),
            amountLimit = draft.amountLimit,
            period = draft.period.wire,
            alertEnabled = draft.alertEnabled,
        )
        budgets[index] = updated
        return updated
    }

    override suspend fun delete(id: String) {
        checkNetwork()
        budgets.removeAll { it.id == id }
    }

    override suspend fun categories(): List<MovementCategory> {
        checkNetwork()
        return categoryList.filter { it.kind == MovementKind.Expense.wire }
    }

    override suspend fun expenses(from: Instant, toExclusive: Instant, categoryId: String?): List<Movement> {
        checkNetwork()
        return MovementRules.newestFirst(
            movements.filter { m ->
                m.isExpense &&
                    (categoryId == null || m.categoryId == categoryId) &&
                    (m.instant?.let { !it.isBefore(from) && it.isBefore(toExclusive) } ?: false)
            }
        )
    }

    override suspend fun alertThreshold(): Int = threshold

    private fun checkUnique(categoryId: String?, excludeId: String?) {
        if (failNextWithDuplicate) {
            failNextWithDuplicate = false
            throw DuplicateBudgetException()
        }
        if (budgets.any { it.categoryId == categoryId && it.id != excludeId }) throw DuplicateBudgetException()
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
