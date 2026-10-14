package com.pocketpal.app.data.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tipo de categoría. RN-11: en la base se guardan **códigos neutros**
 * (`expense`, `income`); las etiquetas ("Gastos"/"Ingresos") las traduce la UI.
 */
enum class CategoryKind(val wire: String) {
    Expense("expense"),
    Income("income");

    companion object {
        fun fromWire(value: String): CategoryKind =
            entries.firstOrNull { it.wire == value } ?: Expense
    }
}

/**
 * Identidad mínima para las reglas de nombre (RN-02): sirve tanto para la fila
 * de la base ([Category]) como para la fila de la UI.
 */
interface CategoryNamed {
    val id: String
    val name: String
}

/**
 * Fila de `public.categories` (SDD_Modulo2.md §6).
 *
 * `monthly_limit` es opcional (RN-03) y `position` es el orden de la lista
 * (D-10: se reordena arrastrando y se persiste aquí).
 */
@Serializable
data class Category(
    @SerialName("id") override val id: String,
    @SerialName("kind") val kind: String,
    @SerialName("name") override val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("monthly_limit") val monthlyLimit: Double? = null,
    @SerialName("position") val position: Int = 0,
) : CategoryNamed {
    val categoryKind: CategoryKind get() = CategoryKind.fromWire(kind)
}

/** Datos del formulario crear/editar (§8.9). */
data class CategoryDraft(
    val kind: CategoryKind,
    val name: String,
    val icon: String,
    val color: String,
    /** `null` = sin límite (el interruptor "Definir límite mensual" apagado). */
    val monthlyLimit: Double?,
)
