package com.pocketpal.app.ui.profile

import androidx.annotation.StringRes
import com.pocketpal.app.R

/** Una pregunta frecuente: título y respuesta como recursos (RF-A3: las FAQ viven en strings). */
data class FaqItem(@param:StringRes val question: Int, @param:StringRes val answer: Int)

/**
 * Preguntas frecuentes de "Ayuda y soporte" (§8.13).
 *
 * La pregunta 1 con su respuesta son las del Figma (pág. 57; corregida la
 * ortografía "editarel" → "editar el", §11.1). Las respuestas 2 a 5 no existen
 * en el diseño (hallazgo 5) y fueron propuestas por la IA y aprobadas por el
 * usuario en la entrega de la Fase 8. `faq_5_a` recibe el correo de soporte como
 * argumento (D-07: eliminar la cuenta se solicita por correo).
 */
object FaqCatalog {

    val items: List<FaqItem> = listOf(
        FaqItem(R.string.faq_1_q, R.string.faq_1_a),
        FaqItem(R.string.faq_2_q, R.string.faq_2_a),
        FaqItem(R.string.faq_3_q, R.string.faq_3_a),
        FaqItem(R.string.faq_4_q, R.string.faq_4_a),
        FaqItem(R.string.faq_5_q, R.string.faq_5_a),
    )

    /**
     * Filtra por el texto de búsqueda en el idioma actual (§8.13: "filtra en el
     * idioma actual"), sin distinguir mayúsculas. [textOf] resuelve el texto de
     * cada elemento para poder probar la función sin recursos de Android.
     */
    fun <T> filter(items: List<T>, query: String, textOf: (T) -> String): List<T> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items.filter { textOf(it).lowercase().contains(q) }
    }
}
