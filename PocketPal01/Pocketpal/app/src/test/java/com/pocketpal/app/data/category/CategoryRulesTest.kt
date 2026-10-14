package com.pocketpal.app.data.category

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RN-02 y RN-03 (SDD_Modulo2.md §4.4) sobre [CategoryRules]:
 * nombre obligatorio de 1 a 30 caracteres, único por usuario y tipo sin
 * distinguir mayúsculas ni espacios, y límite mensual opcional mayor que 0.
 */
class CategoryRulesTest {

    private fun category(id: String, name: String) = Category(
        id = id,
        kind = CategoryKind.Expense.wire,
        name = name,
        icon = "restaurant",
        color = "#FF9B9B",
    )

    @Test
    fun `normalize quita espacios y minusculas`() {
        assertTrue(CategoryRules.normalize("  Alimentos ") == "alimentos")
    }

    @Test
    fun `nombre vacio o solo espacios es invalido`() {
        assertFalse(CategoryRules.isValidName(""))
        assertFalse(CategoryRules.isValidName("   "))
    }

    @Test
    fun `nombre de 1 a 30 caracteres es valido`() {
        assertTrue(CategoryRules.isValidName("A"))
        assertTrue(CategoryRules.isValidName("Alimentos"))
        assertTrue(CategoryRules.isValidName("x".repeat(30)))
    }

    @Test
    fun `nombre de mas de 30 caracteres es invalido`() {
        assertFalse(CategoryRules.isValidName("x".repeat(31)))
    }

    @Test
    fun `limite opcional`() {
        assertTrue(CategoryRules.isValidLimit(null))
    }

    @Test
    fun `limite activo debe ser mayor que 0`() {
        assertFalse(CategoryRules.isValidLimit(0.0))
        assertFalse(CategoryRules.isValidLimit(-1.0))
        assertTrue(CategoryRules.isValidLimit(600000.0))
    }

    @Test
    fun `duplicado ignora mayusculas y espacios`() {
        val existing = listOf(category("1", "Alimentos"))
        assertTrue(CategoryRules.isDuplicateName("  alimentos", existing))
        assertTrue(CategoryRules.isDuplicateName("ALIMENTOS", existing))
        assertFalse(CategoryRules.isDuplicateName("Transporte", existing))
    }

    @Test
    fun `al editar no se duplica consigo misma`() {
        val existing = listOf(category("1", "Alimentos"))
        assertFalse(CategoryRules.isDuplicateName("Alimentos", existing, excludeId = "1"))
        assertTrue(CategoryRules.isDuplicateName("Alimentos", existing, excludeId = "2"))
    }

    @Test
    fun `el formulario es valido cuando cumple RN-02 y RN-03`() {
        val existing = listOf(category("1", "Alimentos"))
        assertTrue(CategoryRules.isFormValid("Transporte", null, existing))
        assertTrue(CategoryRules.isFormValid("Transporte", 600000.0, existing))
        assertFalse(CategoryRules.isFormValid("", null, existing))
        assertFalse(CategoryRules.isFormValid("x".repeat(31), null, existing))
        assertFalse(CategoryRules.isFormValid("Alimentos", null, existing))
        assertFalse(CategoryRules.isFormValid("Transporte", 0.0, existing))
    }
}
