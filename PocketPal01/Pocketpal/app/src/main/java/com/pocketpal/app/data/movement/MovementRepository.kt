package com.pocketpal.app.data.movement

import java.time.Instant

/**
 * Lo que la app puede pedir sobre movimientos. Es una **interfaz**: los ViewModels
 * dependen de ella y no de Supabase. En la app se usa [SupabaseMovementRepository];
 * en las pruebas, un repositorio en memoria (FakeMovementRepository).
 *
 * En este módulo todo es de **lectura**: registrar movimientos lo hará Dai más adelante.
 */
interface MovementRepository {

    /** Movimientos con fecha en [from, toExclusive), el más reciente primero. */
    suspend fun list(from: Instant, toExclusive: Instant): List<Movement>

    /** Los últimos [limit] movimientos (sección "Movimientos recientes" del inicio). */
    suspend fun recent(limit: Int): List<Movement>

    /**
     * Busca [query] en el título o la descripción, sin importar mayúsculas.
     * [kind] `null` = todos; si no, solo gastos o solo ingresos.
     */
    suspend fun search(query: String, kind: MovementKind?, limit: Int = SEARCH_LIMIT): List<Movement>

    /** Un movimiento por id, o `null` si ya no existe. */
    suspend fun get(id: String): Movement?

    /** Las categorías del usuario (gastos e ingresos) para pintar íconos, colores y nombres. */
    suspend fun categories(): List<MovementCategory>

    companion object {
        const val SEARCH_LIMIT = 50
        const val RECENT_LIMIT = 3
    }
}
