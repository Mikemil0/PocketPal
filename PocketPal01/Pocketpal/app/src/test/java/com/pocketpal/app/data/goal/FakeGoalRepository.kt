package com.pocketpal.app.data.goal

import java.time.LocalDate

/**
 * Repositorio de metas **en memoria** para las pruebas de los ViewModels (RNF-02).
 *
 * Imita lo que hacen los triggers de la base: lo ahorrado es la suma de los aportes y
 * la meta queda completada cuando lo ahorrado alcanza el monto. Lo usan las pruebas de
 * todos los flujos del Módulo 3.
 */
class FakeGoalRepository : GoalRepository {

    private val goals = mutableListOf<Goal>()
    private val contributionsList = mutableListOf<GoalContribution>()
    private var nextId = 0
    private var clock = 0

    /** Si es `true`, la próxima creación o edición falla como si la base rechazara el nombre. */
    var failNextWithDuplicate = false

    /** Si es `true`, la próxima operación falla como si no hubiera internet. */
    var failNextWithNetwork = false

    val reorderCalls = mutableListOf<List<String>>()
    val deletedIds = mutableListOf<String>()

    override suspend fun list(): List<Goal> {
        checkNetwork()
        return goals.sortedBy { it.position }
    }

    override suspend fun get(id: String): Goal? = goals.firstOrNull { it.id == id }

    override suspend fun create(draft: GoalDraft, position: Int): Goal {
        checkNetwork()
        checkDuplicate()
        val goal = Goal(
            id = "g${nextId++}",
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            targetAmount = draft.targetAmount,
            deadline = draft.deadline?.toString(),
            position = position,
            createdAt = "2026-10-${(10 + clock++).toString().padStart(2, '0')}T10:00:00Z",
        )
        goals.add(goal)
        if (draft.initialSaved > 0.0) {
            contributionsList.add(contribution(goal.id, draft.initialSaved, LocalDate.of(2026, 10, 1), null, GoalContribution.KIND_INITIAL))
        }
        recalc(goal.id)
        return get(goal.id)!!
    }

    override suspend fun update(id: String, draft: GoalDraft): Goal {
        checkNetwork()
        checkDuplicate()
        val index = goals.indexOfFirst { it.id == id }
        goals[index] = goals[index].copy(
            name = draft.name,
            icon = draft.icon,
            color = draft.color,
            targetAmount = draft.targetAmount,
            deadline = draft.deadline?.toString(),
        )
        recalc(id)
        return get(id)!!
    }

    override suspend fun delete(id: String) {
        checkNetwork()
        goals.removeAll { it.id == id }
        contributionsList.removeAll { it.goalId == id }
        deletedIds.add(id)
    }

    override suspend fun reorder(orderedIds: List<String>) {
        checkNetwork()
        reorderCalls.add(orderedIds)
        orderedIds.forEachIndexed { index, id ->
            val i = goals.indexOfFirst { it.id == id }
            if (i >= 0) goals[i] = goals[i].copy(position = index)
        }
    }

    override suspend fun contributions(goalId: String): List<GoalContribution> {
        checkNetwork()
        return contributionsList.filter { it.goalId == goalId }
            .sortedWith(compareByDescending<GoalContribution> { it.contributedOn }.thenByDescending { it.createdAt.orEmpty() })
    }

    override suspend fun addContribution(goalId: String, amount: Double, date: LocalDate, note: String?): Goal {
        checkNetwork()
        contributionsList.add(contribution(goalId, amount, date, note, GoalContribution.KIND_MANUAL))
        recalc(goalId)
        return get(goalId)!!
    }

    /** Lo que harían los triggers `recalc_goal_saved` y `set_goal_completed`. */
    private fun recalc(goalId: String) {
        val index = goals.indexOfFirst { it.id == goalId }
        if (index < 0) return
        val saved = contributionsList.filter { it.goalId == goalId }.sumOf { it.amount }
        val goal = goals[index]
        val completed = saved >= goal.targetAmount
        goals[index] = goal.copy(
            savedAmount = saved,
            completedAt = if (completed) goal.completedAt ?: "2026-10-20T12:00:00Z" else null,
        )
    }

    private fun contribution(goalId: String, amount: Double, date: LocalDate, note: String?, kind: String) =
        GoalContribution(
            id = "c${nextId++}",
            goalId = goalId,
            amount = amount,
            contributedOn = date.toString(),
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            kind = kind,
            createdAt = "2026-10-20T10:${(clock++).toString().padStart(2, '0')}:00Z",
        )

    private fun checkNetwork() {
        if (failNextWithNetwork) {
            failNextWithNetwork = false
            throw java.io.IOException("sin red")
        }
    }

    private fun checkDuplicate() {
        if (failNextWithDuplicate) {
            failNextWithDuplicate = false
            throw DuplicateGoalNameException()
        }
    }
}
