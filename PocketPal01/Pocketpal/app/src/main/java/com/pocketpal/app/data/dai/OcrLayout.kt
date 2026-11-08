package com.pocketpal.app.data.dai

import kotlin.math.abs
import kotlin.math.min

/**
 * Una línea de texto que leyó el OCR, con su rectángulo en la foto (en píxeles).
 * Es una copia simple de `com.google.mlkit.vision.text.Text.Line` para poder probar
 * [OcrLayout] con JUnit, sin Android.
 */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Double get() = (top + bottom) / 2.0
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

/**
 * Rearma las **filas** de la factura a partir de la posición de cada línea.
 *
 * ML Kit agrupa el texto por **bloques** (columnas), no por filas. En una factura la
 * palabra "TOTAL" queda en el bloque de la izquierda y "$17.900" en el de la derecha, y
 * llegan separadas:
 * ```
 * TIENDA EXPRESS          ← bloque 1
 * Subtotal
 * IVA
 * Total
 * $16.000                 ← bloque 2
 * $1.900
 * $17.900
 * ```
 * Sin la fila, el lector no sabe qué número es el total y termina tomando el número más
 * grande (que muchas veces es el NIT o el teléfono). Aquí se juntan las líneas que están
 * **a la misma altura** y se ordenan de izquierda a derecha:
 * ```
 * Total  $17.900
 * ```
 */
object OcrLayout {

    /** Qué tanto pueden diferir los centros de dos líneas (en alturas de línea) para ir en la misma fila. */
    private const val SAME_ROW = 0.55

    fun rows(lines: List<OcrLine>): List<String> {
        val sorted = lines.filter { it.text.isNotBlank() }.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<OcrLine>>()
        for (line in sorted) {
            val row = rows.lastOrNull()
            if (row != null && sameRow(row, line)) row.add(line) else rows.add(mutableListOf(line))
        }
        return rows.map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }
    }

    private fun sameRow(row: List<OcrLine>, line: OcrLine): Boolean {
        val rowCenter = row.map { it.centerY }.average()
        val rowHeight = row.map { it.height }.average()
        val tolerance = min(rowHeight, line.height.toDouble()) * SAME_ROW
        // Dos líneas que se pisan horizontalmente no pueden ser de la misma fila.
        val overlaps = row.any { it.left < line.right && line.left < it.right }
        return !overlaps && abs(line.centerY - rowCenter) <= tolerance
    }
}
