package com.pocketpal.app.data.category

/**
 * Contrato de categorías (RNF-02): los ViewModels dependen de esta interfaz,
 * **no** de Supabase directamente, para poder probarse con datos simulados.
 */
interface CategoryRepository {

    /** Categorías del tipo dado, en su orden (`position` ascendente). */
    suspend fun list(kind: CategoryKind): List<Category>

    /** Categoría por id, o `null` si no existe (o no es propia: RLS). */
    suspend fun get(id: String): Category?

    /** Crea la categoría al final de la lista ([position] = último orden + 1). */
    suspend fun create(draft: CategoryDraft, position: Int): Category

    /** Actualiza nombre, ícono, color y límite (no el tipo ni la posición). */
    suspend fun update(id: String, draft: CategoryDraft): Category

    /** Elimina la categoría. */
    suspend fun delete(id: String)

    /**
     * Reordena: [orderedIds] recibe los ids en el nuevo orden y se persisten
     * las posiciones 0..n-1 (TC-22: el orden se conserva al reabrir la app).
     */
    suspend fun reorder(kind: CategoryKind, orderedIds: List<String>)

    /**
     * Consulta previa de RN-02. La **autoridad** sigue siendo el índice único
     * `categories_unique_name` de la base; si este método devuelve `false` y la
     * base rechaza la escritura, el error se traduce igualmente (ver
     * [DuplicateCategoryNameException]).
     */
    suspend fun nameExists(kind: CategoryKind, name: String, excludeId: String? = null): Boolean

    /**
     * Movimientos y total de cada categoría (§6: "dependencia con módulos
     * futuros"). La tabla `movements` no existe hasta el Módulo 3, así que esta
     * interfaz devuelve **vacío / 0** y la UI muestra "0 movimientos" y "$0".
     */
    suspend fun getCategoryTotals(): Map<String, CategoryStats>
}

/** Contador de movimientos y suma de una categoría; 0 hasta que exista `movements`. */
data class CategoryStats(
    val movementCount: Int = 0,
    val total: Double = 0.0,
)

/**
 * La base rechazó el nombre por duplicado (índice único `categories_unique_name`).
 * El repositorio traduce aquí el error de PostgREST para que el ViewModel no
 * dependa de Supabase (RNF-02) y la UI muestre el banner de RN-02.
 */
class DuplicateCategoryNameException : Exception("duplicate category name")
