package com.pocketpal.app.data.movement

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Repositorio de movimientos **en memoria** para las pruebas de los ViewModels.
 * Aplica las mismas reglas que la base (rango de fechas, búsqueda en título o
 * descripción, más reciente primero). Lo usan las pruebas de todos los flujos del Módulo 4.
 */
class FakeMovementRepository(private val zone: ZoneId = ZONE) : MovementRepository {

    val movements = mutableListOf<Movement>()
    val categoryList = mutableListOf<MovementCategory>()
    private var nextId = 0

    /** Si es `true`, la próxima consulta falla como si no hubiera internet. */
    var failNextWithNetwork = false

    /** Cuántas veces se llamó a [search] (para probar la espera antes de buscar). */
    var searchCalls = 0

    /** Agrega un movimiento de prueba el [date] a las [hour]:00 (hora de Colombia). */
    fun add(
        title: String,
        amount: Double,
        date: LocalDate,
        kind: MovementKind = MovementKind.Expense,
        categoryId: String? = null,
        description: String? = null,
        hour: Int = 10,
        recurrence: Recurrence? = null,
        nextChargeOn: LocalDate? = null,
    ): Movement {
        val movement = Movement(
            id = "m${nextId++}",
            kind = kind.wire,
            title = title,
            amount = amount,
            categoryId = categoryId,
            description = description,
            occurredAt = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toOffsetDateTime().toString(),
            recurrence = recurrence?.wire,
            nextChargeOn = nextChargeOn?.toString(),
        )
        movements.add(movement)
        return movement
    }

    fun addCategory(
        id: String,
        name: String,
        kind: MovementKind = MovementKind.Expense,
        monthlyLimit: Double? = null,
    ): MovementCategory {
        val category = MovementCategory(id, kind.wire, name, "restaurant", "#FF9B9B", monthlyLimit)
        categoryList.add(category)
        return category
    }

    override suspend fun list(from: Instant, toExclusive: Instant): List<Movement> {
        checkNetwork()
        return MovementRules.newestFirst(
            movements.filter { m -> m.instant?.let { !it.isBefore(from) && it.isBefore(toExclusive) } ?: false }
        )
    }

    override suspend fun recent(limit: Int): List<Movement> {
        checkNetwork()
        return MovementRules.newestFirst(movements).take(limit)
    }

    override suspend fun search(query: String, kind: MovementKind?, limit: Int): List<Movement> {
        checkNetwork()
        searchCalls++
        if (!MovementRules.canSearch(query)) return emptyList()
        return MovementRules.newestFirst(
            MovementRules.filterByKind(movements, kind).filter { MovementRules.matches(it, query) }
        ).take(limit)
    }

    override suspend fun get(id: String): Movement? {
        checkNetwork()
        return movements.firstOrNull { it.id == id }
    }

    override suspend fun categories(): List<MovementCategory> {
        checkNetwork()
        return categoryList.toList()
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
