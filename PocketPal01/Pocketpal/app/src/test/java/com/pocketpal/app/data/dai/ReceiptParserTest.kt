package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Base del módulo de Dai · lectura de facturas (lo que viene del OCR). */
class ReceiptParserTest {

    private val today = LocalDate.of(2026, 10, 27)

    /** El texto que ML Kit lee de la factura del Figma ("TIENDA EXPRESS"). */
    private val figmaReceipt = """
        TIENDA EXPRESS
        NIT 123456789-0
        12/04/2026 16:24
        Producto   Cant   Valor
        Agua       1.000  $3.000
        Pan        1.000  $2.500
        Galletas   1.000  $4.500
        Leche      1.000  $6.000
        Subtotal          $16.000
        IVA                $1.900
        Total             $17.900
        ¡Gracias por tu compra!
    """.trimIndent()

    private val categories = listOf(
        MovementCategory("comida", MovementKind.Expense.wire, "Comida", "restaurant", "#FF9B9B"),
        MovementCategory("transporte", MovementKind.Expense.wire, "Transporte", "dashboard", "#55A5EF"),
        MovementCategory("salario", MovementKind.Income.wire, "Salario", "savings", "#55A5EF"),
    )

    @Test
    fun `lee la factura del Figma`() {
        val draft = ReceiptParser.parse(figmaReceipt, today, categories)
        assertEquals("Tienda Express", draft.merchant)
        assertEquals(17_900.0, draft.amount!!, 0.001)     // el TOTAL, no el subtotal
        assertEquals(LocalDate.of(2026, 4, 12), draft.date) // día/mes/año
        assertEquals("comida", draft.categoryId)            // "tienda", "pan", "leche" → Comida
        assertEquals("Compra en Tienda Express", draft.reason)
    }

    @Test
    fun `montos en formato colombiano y gringo`() {
        assertEquals(17_900.0, ReceiptParser.parseAmount("$17.900")!!, 0.001)
        assertEquals(17_900.5, ReceiptParser.parseAmount("17.900,50")!!, 0.001)
        assertEquals(17_900.5, ReceiptParser.parseAmount("17,900.50")!!, 0.001)
        assertEquals(1_250_000.0, ReceiptParser.parseAmount("1.250.000")!!, 0.001)
        assertEquals(45.5, ReceiptParser.parseAmount("45,50")!!, 0.001)
        assertEquals(17_900.0, ReceiptParser.parseAmount("17900")!!, 0.001)
        assertNull(ReceiptParser.parseAmount("$"))
    }

    @Test
    fun `sin la palabra total toma el monto mas grande`() {
        val lines = listOf("Uber", "Viaje 12 km", "Tarifa $8.500", "Peaje $2.000", "Cobro $10.500")
        assertEquals(10_500.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `total a pagar gana sobre total`() {
        val lines = listOf("Total productos $50.000", "Descuento $5.000", "TOTAL A PAGAR $45.000")
        assertEquals(45_000.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `fechas - formatos y nunca futura`() {
        assertEquals(LocalDate.of(2026, 10, 3), ReceiptParser.findDate("Fecha: 2026-10-03", today))
        assertEquals(LocalDate.of(2026, 10, 3), ReceiptParser.findDate("03-10-26", today))
        assertNull(ReceiptParser.findDate("Vence 15/12/2026", today))   // futura: no
        assertNull(ReceiptParser.findDate("sin fecha", today))
        // Si no hay fecha, el borrador usa hoy (el usuario la puede cambiar).
        assertEquals(today, ReceiptParser.parse("TIENDA\nTotal 5.000", today, categories).date)
    }

    @Test
    fun `el comercio salta los encabezados`() {
        assertEquals("Drogueria Alemana", ReceiptParser.findMerchant(listOf("FACTURA DE VENTA", "NIT 900.123", "DROGUERIA ALEMANA", "Total 20.000")))
        assertEquals("", ReceiptParser.findMerchant(listOf("$12.000", "12/10/2026")))
    }

    @Test
    fun `sugiere categoria por nombre o por palabras clave`() {
        assertEquals("transporte", ReceiptParser.suggestCategory("UBER BV viaje", categories)?.id)
        assertEquals("transporte", ReceiptParser.suggestCategory("Pago transporte escolar", categories)?.id)
        assertNull(ReceiptParser.suggestCategory("Ferreteria El Tornillo", categories))
        // Nunca sugiere una categoría de ingresos.
        assertNull(ReceiptParser.suggestCategory("Salario octubre", categories))
    }

    @Test
    fun `validacion antes de guardar`() {
        val ok = ReceiptDraft("Tienda", 17_900.0, today, "comida", "")
        assertNull(ReceiptParser.validate(ok, today))
        assertEquals(ReceiptProblem.Amount, ReceiptParser.validate(ok.copy(amount = null), today))
        assertEquals(ReceiptProblem.Amount, ReceiptParser.validate(ok.copy(amount = 0.0), today))
        assertEquals(ReceiptProblem.FutureDate, ReceiptParser.validate(ok.copy(date = today.plusDays(1)), today))
        assertEquals(ReceiptProblem.Merchant, ReceiptParser.validate(ok.copy(merchant = " "), today))
    }

    @Test
    fun `donde se guardara - el presupuesto antes y despues`() {
        val repo = FakeDaiRepository().apply {
            addCategory("comida", "Comida")
            addBudget("comida", "Comida", 500_000.0)
            addExpense("Mercado", 420_000.0, today, "comida")
        }
        val data = runBlocking { repo.snapshot(today, FakeDaiRepository.ZONE, 1, "") }
        val draft = ReceiptDraft("Tienda Express", 17_900.0, today, "comida", "")
        val impact = ReceiptParser.impact(draft, data, weekStart = 1)
        assertEquals("Comida", impact.categoryName)
        assertEquals(84, impact.budgetPercentBefore)
        assertEquals(88, impact.budgetPercentAfter)
        assertFalse(impact.exceedsBudget)

        // Una factura de otro mes no mueve el presupuesto de este mes.
        val old = ReceiptParser.impact(draft.copy(date = LocalDate.of(2026, 9, 2)), data, 1)
        assertEquals(84, old.budgetPercentAfter)

        // Sin categoría: no hay presupuesto que mostrar.
        val none = ReceiptParser.impact(draft.copy(categoryId = null), data, 1)
        assertNull(none.categoryName)
        assertNull(none.budgetName)

        val reply = ReceiptParser.savedReply(draft, impact)
        assertTrue(reply.text.contains("Registré $17.900 de Tienda Express en Comida"))
        assertTrue(reply.text.contains("queda en el 88 %"))
    }
}
