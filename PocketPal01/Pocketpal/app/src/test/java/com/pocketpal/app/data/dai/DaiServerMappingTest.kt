package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.MovementCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Cómo se combina la respuesta del servidor de Dai con lo que leyó el teléfono. */
class DaiServerMappingTest {

    private val today = LocalDate.of(2026, 11, 11)
    private val categories = listOf(
        MovementCategory("c1", "expense", "Comida", "restaurant", "#FF9B9B"),
        MovementCategory("c2", "expense", "Transporte", "dashboard", "#55A5EF"),
        MovementCategory("c3", "expense", "Compras", "favorite", "#C90E0E"),
        MovementCategory("c4", "expense", "Hogar", "home", "#D194E5"),
    )
    private val local = ReceiptDraft(
        merchant = "Tienda Express",
        amount = 17_900.0,
        date = today,
        categoryId = null,
        reason = "Compra en Tienda Express",
    )

    @Test
    fun `usa monto, fecha, categoria y motivo del servidor`() {
        val server = ServerReceipt(48_710.0, LocalDate.of(2026, 11, 2), "Supermercado", "Mercado", "EXITO")
        val draft = DaiServerMapping.merge(local, server, categories, today)
        assertEquals(48_710.0, draft.amount!!, 0.001)
        assertEquals(LocalDate.of(2026, 11, 2), draft.date)
        assertEquals("c1", draft.categoryId)
        assertEquals("Mercado", draft.reason)
        assertEquals("Tienda Express", draft.merchant)
    }

    @Test
    fun `una fecha futura del servidor no se usa`() {
        val server = ServerReceipt(50_000.0, LocalDate.of(2026, 11, 23), "Servicios", "Gas", "VANTI")
        val draft = DaiServerMapping.merge(local, server, categories, today)
        assertEquals(today, draft.date)
        assertEquals("c4", draft.categoryId)
    }

    @Test
    fun `sin monto ni comercio en el servidor se conserva lo del telefono`() {
        val server = ServerReceipt(null, null, null, "", "")
        val draft = DaiServerMapping.merge(local.copy(merchant = ""), server.copy(merchant = "TERPEL"), categories, today)
        assertEquals(17_900.0, draft.amount!!, 0.001)
        assertEquals("TERPEL", draft.merchant)
        assertEquals("Compra en Tienda Express", draft.reason)
    }

    @Test
    fun `categorias equivalentes`() {
        assertEquals("c2", DaiServerMapping.matchCategory("Transporte", categories))
        assertEquals("c1", DaiServerMapping.matchCategory("Restaurantes y domicilios", categories))
        assertEquals("c3", DaiServerMapping.matchCategory("Ropa y accesorios", categories))
        assertNull(DaiServerMapping.matchCategory("Educación", categories))
    }
}
