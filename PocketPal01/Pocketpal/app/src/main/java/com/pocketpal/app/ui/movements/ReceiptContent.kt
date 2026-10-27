package com.pocketpal.app.ui.movements

/**
 * Flujo 4 · Contenido del comprobante que se comparte o se descarga.
 *
 * Es una clase **sin Android**: recibe los textos ya traducidos (etiquetas y valores) y
 * arma las líneas. Así se puede probar con JUnit, y la parte que sí usa Android
 * ([MovementReceipt]) solo se encarga de enviarlas o dibujarlas en un PDF.
 */
data class ReceiptContent(
    /** "Comprobante de movimiento · PocketPal" */
    val heading: String,
    val title: String,
    /** "- $86.450" */
    val amount: String,
    /** Pares etiqueta → valor: ("Categoría", "Comida"), ("Fecha", "12 abr 2025 · 10:24 a. m.")… */
    val fields: List<Pair<String, String>>,
    /** Pie: "Generado con PocketPal". */
    val footer: String,
    /** Id del movimiento, para el nombre del archivo. */
    val movementId: String,
) {
    /** Texto plano para compartir por WhatsApp, correo, etc. */
    fun shareText(): String = buildString {
        appendLine(heading)
        appendLine()
        appendLine(title)
        appendLine(amount)
        fields.filter { it.second.isNotBlank() }.forEach { (label, value) -> appendLine("$label: $value") }
        appendLine()
        append(footer)
    }

    /** Líneas del PDF, en orden. */
    fun pdfLines(): List<String> =
        listOf(title, amount) + fields.filter { it.second.isNotBlank() }.map { (label, value) -> "$label: $value" }

    /** "PocketPal_comprobante_<id>.pdf" (solo letras, números, guion y guion bajo). */
    fun fileName(): String = "PocketPal_comprobante_${movementId.replace(Regex("[^A-Za-z0-9_-]"), "").take(12)}.pdf"
}
