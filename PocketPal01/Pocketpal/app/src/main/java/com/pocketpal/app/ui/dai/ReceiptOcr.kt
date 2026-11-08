package com.pocketpal.app.ui.dai

import com.google.mlkit.vision.text.Text
import com.pocketpal.app.data.dai.OcrLayout
import com.pocketpal.app.data.dai.OcrLine

/**
 * Puente entre ML Kit y el lector de facturas.
 *
 * ML Kit devuelve el texto por **bloques** (columnas). Aquí se toma cada línea con su
 * rectángulo en la foto y [OcrLayout.rows] la vuelve a armar **por filas**, para que la
 * etiqueta "Total" y su valor "$17.900" queden juntos antes de llegar a `ReceiptParser`.
 */
object ReceiptOcr {

    fun toText(result: Text): String {
        val lines = result.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLine(line.text, box.left, box.top, box.right, box.bottom)
            }
        // Sin posiciones (no debería pasar) se usa el texto tal como lo entregó ML Kit.
        if (lines.isEmpty()) return result.text
        return OcrLayout.rows(lines).joinToString("\n")
    }
}
