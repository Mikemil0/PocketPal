package com.pocketpal.app.data.budget

import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import java.time.Instant

/**
 * Lo que la app puede pedir y guardar sobre presupuestos. Es una **interfaz**: los
 * ViewModels dependen de ella y no de Supabase. En la app se usa
 * [SupabaseBudgetRepository]; en las pruebas, un repositorio en memoria
 * (FakeBudgetRepository).
 */
interface BudgetRepository {

    /** Todos los presupuestos del usuario, del más antiguo al más nuevo. */
    suspend fun list(): List<Budget>

    /** Un presupuesto por id, o `null` si ya no existe. */
    suspend fun get(id: String): Budget?

    /** Crea el presupuesto. Si la categoría ya tiene uno, lanza [DuplicateBudgetException]. */
    suspend fun create(draft: BudgetDraft): Budget

    /** Cambia categoría, nombre, límite, período y aviso. */
    suspend fun update(id: String, draft: BudgetDraft): Budget

    /** Borra el presupuesto (los gastos de `movements` NO se tocan). */
    suspend fun delete(id: String)

    /** Categorías de gastos del usuario (Módulo 2), en su orden. */
    suspend fun categories(): List<MovementCategory>

    /**
     * Gastos con fecha en [from, toExclusive), opcionalmente de una sola categoría.
     * Con ellos se calcula lo gastado de cada presupuesto (tabla `movements` del Módulo 4).
     */
    suspend fun expenses(from: Instant, toExclusive: Instant, categoryId: String? = null): List<Movement>

    /** Umbral del aviso (columna `budget_threshold` de Notificaciones, 80 % por defecto). */
    suspend fun alertThreshold(): Int
}

/**
 * La base rechazó el presupuesto porque esa categoría ya tiene uno (índice único
 * `budgets_one_per_category`). El repositorio traduce el error de Supabase a esta
 * excepción para que el ViewModel no dependa de Supabase.
 */
class DuplicateBudgetException : Exception("duplicate budget")
