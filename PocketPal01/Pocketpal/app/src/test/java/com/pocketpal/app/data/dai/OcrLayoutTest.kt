package com.pocketpal.app.data.dai

import org.junit.Assert.assertEquals
import org.junit.Test

/** Rearmar las filas de la factura con la posición de cada línea del OCR. */
class OcrLayoutTest {

    /** Una línea a la altura [y] (alto 20 px), desde [left] hasta [right]. */
    private fun line(text: String, left: Int, right: Int, y: Int) = OcrLine(text, left, y, right, y + 20)

    @Test
    fun `une la etiqueta y el valor que ML Kit entrega en bloques distintos`() {
        // Como llega de ML Kit: primero el bloque de la izquierda y luego el de la derecha.
        val lines = listOf(
            line("TIENDA EXPRESS", 40, 300, 10),
            line("Subtotal", 40, 140, 200),
            line("IVA", 40, 90, 230),
            line("Total", 40, 110, 262),
            line("$16.000", 400, 500, 202),
            line("$1.900", 410, 500, 231),
            line("$17.900", 400, 500, 258),
        )
        assertEquals(
            listOf("TIENDA EXPRESS", "Subtotal  $16.000", "IVA  $1.900", "Total  $17.900"),
            OcrLayout.rows(lines),
        )
    }

    @Test
    fun `lineas de alturas distintas no se mezclan`() {
        val lines = listOf(line("Pan", 40, 100, 100), line("$2.500", 400, 480, 160))
        assertEquals(listOf("Pan", "$2.500"), OcrLayout.rows(lines))
    }

    @Test
    fun `dos lineas que se pisan no son de la misma fila`() {
        val lines = listOf(line("NIT 900123", 40, 300, 100), line("Calle 10", 60, 280, 106))
        assertEquals(listOf("NIT 900123", "Calle 10"), OcrLayout.rows(lines))
    }
}
