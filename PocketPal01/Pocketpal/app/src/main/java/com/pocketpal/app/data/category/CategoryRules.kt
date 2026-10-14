package com.pocketpal.app.data.category

/**
 * Reglas de negocio de categorías (RN-02 y RN-03, SDD_Modulo2.md §4.4).
 *
 * Funciones puras para poder probarlas en unitarias (§10) sin Android ni red.
 */
object CategoryRules {

    /** RN-02: nombre de 1 a 30 caracteres. */
    const val MAX_NAME_LENGTH = 30

    /** RN-02: la unicidad ignora mayúsculas y espacios al inicio o final. */
    fun normalize(name: String): String = name.trim().lowercase()

    fun isValidName(name: String): Boolean {
        val length = name.trim().length
        return length in 1..MAX_NAME_LENGTH
    }

    /** RN-03: el límite es opcional; si está activo debe ser un número mayor que 0. */
    fun isValidLimit(monthlyLimit: Double?): Boolean =
        monthlyLimit == null || monthlyLimit > 0.0

    /**
     * RN-02: único por usuario y por tipo (gasto o ingreso). [existing] es la
     * lista ya cargada del tipo; [excludeId] evita que la categoría al editar
     * se duplique consigo misma.
     */
    fun isDuplicateName(
        name: String,
        existing: List<CategoryNamed>,
        excludeId: String? = null,
    ): Boolean {
        val normalized = normalize(name)
        return existing.any { it.id != excludeId && normalize(it.name) == normalized }
    }

    /** El formulario es válido cuando RN-02 y RN-03 se cumplen (Guardar habilitado, §8.9). */
    fun isFormValid(
        name: String,
        monthlyLimit: Double?,
        existing: List<CategoryNamed>,
        excludeId: String? = null,
    ): Boolean =
        isValidName(name) &&
            isValidLimit(monthlyLimit) &&
            !isDuplicateName(name, existing, excludeId)
}
