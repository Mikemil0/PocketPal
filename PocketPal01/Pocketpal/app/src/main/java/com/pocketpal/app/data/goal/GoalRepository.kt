package com.pocketpal.app.data.goal

import java.time.LocalDate

/**
 * Contrato de Metas (RNF-02): los ViewModels dependen de esta interfaz, **no** de
 * Supabase, para poder probarse con un repositorio en memoria (`FakeGoalRepository`).
 */
interface GoalRepository {

    /** Todas las metas del usuario (activas y completadas), en su orden (`position`). */
    suspend fun list(): List<Goal>

    /** Meta por id, o `null` si no existe o no es del usuario (RLS). */
    suspend fun get(id: String): Goal?

    /**
     * Crea la meta al final de la lista. Si [GoalDraft.initialSaved] es mayor que 0,
     * registra también el aporte inicial (`kind = initial`) y la base recalcula lo ahorrado.
     * Devuelve la meta ya con lo ahorrado calculado.
     */
    suspend fun create(draft: GoalDraft, position: Int): Goal

    /** Actualiza nombre, ícono, color, monto y fecha límite (no lo ahorrado ni la posición). */
    suspend fun update(id: String, draft: GoalDraft): Goal

    /** Elimina la meta y, en cascada, todos sus aportes. */
    suspend fun delete(id: String)

    /** Guarda el nuevo orden manual: [orderedIds] queda con posiciones 0..n-1. */
    suspend fun reorder(orderedIds: List<String>)

    /** Aportes de la meta, del más reciente al más antiguo. */
    suspend fun contributions(goalId: String): List<GoalContribution>

    /**
     * Registra un aporte y devuelve la meta actualizada (lo ahorrado y, si se completó,
     * `completed_at` los calcula la base).
     */
    suspend fun addContribution(goalId: String, amount: Double, date: LocalDate, note: String?): Goal
}

/**
 * La base rechazó el nombre por duplicado (índice único `goals_unique_name`). El
 * repositorio traduce el error de Supabase a esta excepción para que el ViewModel no
 * dependa de Supabase.
 */
class DuplicateGoalNameException : Exception("duplicate goal name")
