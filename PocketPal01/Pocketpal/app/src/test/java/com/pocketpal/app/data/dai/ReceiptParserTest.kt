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
        // El comercio ya no se pide en "Confirmar": sin comercio también se puede guardar.
        assertNull(ReceiptParser.validate(ok.copy(merchant = " "), today))
    }

    @Test
    fun `titulo del gasto sin comercio`() {
        val draft = ReceiptDraft("", 17_900.0, today, "comida", "Mercado")
        // El título es el motivo (aunque Dai haya leído un comercio).
        assertEquals("Mercado", ReceiptParser.titleFor(draft.copy(merchant = "Tienda"), "Comida"))
        assertEquals("Mercado", ReceiptParser.titleFor(draft, "Comida"))
        assertEquals("Tienda", ReceiptParser.titleFor(draft.copy(merchant = "Tienda", reason = ""), "Comida"))
        assertEquals("Comida", ReceiptParser.titleFor(draft.copy(reason = ""), "Comida"))
        assertEquals("Dai", ReceiptParser.titleFor(draft.copy(reason = ""), null))
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

    // ---------------- Total: casos reales que leían mal ----------------

    @Test
    fun `si la etiqueta Total quedo sin numero toma el de la fila de abajo, no el NIT`() {
        val lines = listOf("SUPERMERCADO LA 14", "NIT 900.123.456-7", "Tel 6012345678", "Total", "$48.350", "Efectivo $50.000", "Cambio $1.650")
        assertEquals(48_350.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `sin etiqueta no toma el NIT, el telefono ni el anio`() {
        val lines = listOf("DROGUERIA ALEMANA", "NIT 900123456-7", "Cel 3001234567", "12/10/2026 08:15", "Acetaminofen $8.500", "Suero $4.200", "$12.700")
        assertEquals(12_700.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `efectivo menos cambio cuando no hay total`() {
        val lines = listOf("PANADERIA", "Pan $3.000", "Cafe $2.500", "Efectivo $10.000", "Cambio $4.500")
        assertEquals(5_500.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `el efectivo no gana aunque sea mas grande que el total`() {
        val lines = listOf("TOTAL $17.900", "EFECTIVO $50.000", "CAMBIO $32.100")
        assertEquals(17_900.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `total de items o de IVA no es el total`() {
        val lines = listOf("TOTAL ITEMS 4", "TOTAL IVA $2.850", "TOTAL $17.850")
        assertEquals(17_850.0, ReceiptParser.findTotal(lines)!!, 0.001)
    }

    @Test
    fun `corrige letras que el OCR confunde con numeros`() {
        assertEquals(17_900.0, ReceiptParser.findTotal(listOf("TOTAL $17.9OO"))!!, 0.001)
        assertEquals(17_900.0, ReceiptParser.findTotal(listOf("TOTAL S17.900"))!!, 0.001)
        assertEquals(17_900.0, ReceiptParser.findTotal(listOf("TOTAL17.900"))!!, 0.001)
        assertEquals(17_900.0, ReceiptParser.findTotal(listOf("TOTAL $17. 900"))!!, 0.001)
    }

    @Test
    fun `la factura del Figma tal como la entrega ML Kit, por bloques`() {
        val mlKitBlocks = listOf(
            OcrLine("TIENDA EXPRESS", 40, 10, 300, 30),
            OcrLine("NIT 123456789-0", 40, 40, 300, 60),
            OcrLine("12/04/2026 16:24", 40, 70, 300, 90),
            OcrLine("Subtotal", 40, 200, 140, 220),
            OcrLine("IVA", 40, 230, 90, 250),
            OcrLine("Total", 40, 262, 110, 282),
            OcrLine("$16.000", 400, 202, 500, 222),
            OcrLine("$1.900", 410, 231, 500, 251),
            OcrLine("$17.900", 400, 258, 500, 278),
        )
        val text = OcrLayout.rows(mlKitBlocks).joinToString("\n")
        val draft = ReceiptParser.parse(text, today, categories)
        assertEquals(17_900.0, draft.amount!!, 0.001)
        assertEquals("Tienda Express", draft.merchant)
        assertEquals(LocalDate.of(2026, 4, 12), draft.date)
    }

    // ---------------- Fecha: la factura del video (Universidad de Los Andes) ----------------

    @Test
    fun `toma la fecha de la compra, no la vigencia ni la resolucion de la DIAN`() {
        val hoy = LocalDate.of(2026, 11, 8)
        val text = """
            Universidad de Los Andes
            Fecha 2O26-10-2b  Hora 17:19:32
            LCPZ COLOMBIA SAS
            NIT 901320488-1
            Resolucion 012220 de 26/12/2022
            FECHA CREACION: 26/10/2026 05:19:32 PM
            Subtotal + Impuestos  $ 20.900
            Total  $ 20.900
            Tarjeta Debito  $ 20.900
            Res DIAN 18764 desde 2026-08-28 hasta 2027-08-28
            VIGENCIA: 2026-08-28
            FECHA VALIDACION: 2026-10-26 05:19 PM
        """.trimIndent()
        val draft = ReceiptParser.parse(text, hoy, categories)
        assertEquals(LocalDate.of(2026, 10, 26), draft.date)
        assertEquals(20_900.0, draft.amount!!, 0.001)
    }

    @Test
    fun `sin etiqueta toma la fecha mas reciente que no sea futura`() {
        val hoy = LocalDate.of(2026, 11, 8)
        assertEquals(LocalDate.of(2026, 11, 2), ReceiptParser.findDate("02/11/2026\nPlazo 01/09/2026", hoy))
        assertEquals(LocalDate.of(2026, 10, 26), ReceiptParser.findDate("Vigencia 2026-08-28\n26/10/2026", hoy))
    }

    /** Recibo de Claro (servicios), como lo arma OcrLayout: la fecha límite va en la fila de abajo. */
    private val claro = """
        Claro  COMCEL S.A.  NIT 800.153.993-7  DISTRICONFECCIONES F Y G SAS -
        SERVICIOS MÓVILES  FECHA LÍMITE DE PAGO:  REFERENCIA DE PAGO:  TOTAL A PAGAR:
        Código Interno: 8.224466416  26-Ago-2026  6224664165  ${'$'}242,574.31
        Cargos anteriores  ${'$'}242,575.15
        FECHA DE CARGA DE MINUTOS:  12-Ago-2026  TOTAL A PAGAR  ${'$'}242,574.31
        PERIODO DE FACTURACIÓN:  12-Jul-2026 a 11-Ago-2026
        FECHA DE FACTURACIÓN:  12-Ago-2026
        SUSPENSIÓN A PARTIR DE:  28-Ago-2026
    """.trimIndent()

    @Test
    fun `recibo de servicios - usa la fecha limite de pago aunque este en la fila de abajo`() {
        assertEquals(LocalDate.of(2026, 8, 26), ReceiptParser.findDate(claro, LocalDate.of(2026, 11, 10)))
    }

    @Test
    fun `recibo de servicios - lee el total a pagar`() {
        val draft = ReceiptParser.parse(claro, LocalDate.of(2026, 11, 10), emptyList())
        assertEquals(242_574.31, draft.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 8, 26), draft.date)
    }

    @Test
    fun `recibo de servicios - si la fecha limite es futura usa la de facturacion`() {
        assertEquals(LocalDate.of(2026, 8, 12), ReceiptParser.findDate(claro, LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `fechas con el mes en letras`() {
        val day = LocalDate.of(2026, 11, 10)
        assertEquals(LocalDate.of(2026, 10, 8), ReceiptParser.findDate("Fecha: jue, 8 oct, 2026", day))
        assertEquals(LocalDate.of(2026, 10, 2), ReceiptParser.findDate("FECHA OCT/02/2026", day))
        assertEquals(LocalDate.of(2026, 9, 18), ReceiptParser.findDate("Fecha de emisión 18 de septiembre de 2026", day))
        assertEquals(LocalDate.of(2026, 7, 12), ReceiptParser.findDate("Fecha 12-Jul-2026", day))
    }

    /** Las categorías por defecto de PocketPal más Salud. */
    private val userCategories = listOf(
        MovementCategory("comida", MovementKind.Expense.wire, "Comida", "restaurant", "#FF9B9B"),
        MovementCategory("transporte", MovementKind.Expense.wire, "Transporte", "dashboard", "#55A5EF"),
        MovementCategory("entretenimiento", MovementKind.Expense.wire, "Entretenimiento", "star", "#EAB760"),
        MovementCategory("hogar", MovementKind.Expense.wire, "Hogar", "home", "#D194E5"),
        MovementCategory("salud", MovementKind.Expense.wire, "Salud", "favorite", "#C90E0E"),
    )

    @Test
    fun `recibo del acueducto - no queda en Salud ni en Comida`() {
        val agua = """
            EMPRESA DE ACUEDUCTO Y ALCANTARILLADO DE BOGOTA
            Barrio Floralia  Medidor 12345  Lectura y medición
            Consumo de agua 18 m3
            Total a pagar  ${'$'}85.300
        """.trimIndent()
        val draft = ReceiptParser.parse(agua, LocalDate.of(2026, 11, 10), userCategories)
        assertEquals("hogar", draft.categoryId)
        assertEquals("Pago de agua", draft.reason)
    }

    @Test
    fun `recibo de Claro - comercio, motivo y categoria`() {
        val draft = ReceiptParser.parse(claro, LocalDate.of(2026, 11, 10), userCategories)
        assertEquals("Claro", draft.merchant)
        assertEquals("Pago de celular", draft.reason)
        assertEquals("hogar", draft.categoryId)
    }

    @Test
    fun `una drogueria sigue siendo Salud`() {
        val draft = ReceiptParser.parse("DROGUERIA LA REBAJA\nAcetaminofen\nTotal ${'$'}12.500", LocalDate.of(2026, 11, 10), userCategories)
        assertEquals("salud", draft.categoryId)
        assertEquals("Compra en Drogueria La Rebaja", draft.reason)
    }
}
