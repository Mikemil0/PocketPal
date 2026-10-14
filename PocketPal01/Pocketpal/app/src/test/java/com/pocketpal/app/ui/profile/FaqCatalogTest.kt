package com.pocketpal.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §8.13 sobre [FaqCatalog]: el catálogo de preguntas frecuentes y el filtrado del
 * buscador en el idioma actual. El filtro se prueba con textos simulados porque la
 * resolución real vive en los recursos (RF-A3).
 */
class FaqCatalogTest {

    private data class Faq(val question: String, val answer: String)

    private val faqs = listOf(
        Faq("¿Cómo edito un movimiento?", "Desde Movimientos toca el gasto."),
        Faq("¿Mis datos están seguros?", "Sí, con reglas de seguridad en el servidor."),
        Faq("¿Cómo creo una meta?", "Llegarán en una próxima versión."),
    )

    @Test
    fun `el catalogo tiene las 5 preguntas del diseno`() {
        assertEquals(5, FaqCatalog.items.size)
    }

    @Test
    fun `consulta vacia devuelve todo`() {
        assertEquals(faqs, FaqCatalog.filter(faqs, "   ") { it.question })
    }

    @Test
    fun `filtra por la pregunta sin distinguir mayusculas`() {
        val result = FaqCatalog.filter(faqs, "DATOS") { it.question }
        assertEquals(listOf(faqs[1]), result)
    }

    @Test
    fun `filtra tambien por la respuesta`() {
        val result = FaqCatalog.filter(faqs, "próxima versión") { it.question + " " + it.answer }
        assertEquals(listOf(faqs[2]), result)
    }

    @Test
    fun `sin coincidencias devuelve vacio`() {
        assertTrue(FaqCatalog.filter(faqs, "zzz") { it.question }.isEmpty())
    }
}
