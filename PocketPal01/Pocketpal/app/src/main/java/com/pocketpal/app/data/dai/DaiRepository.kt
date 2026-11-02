package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.Movement
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lo que Dai puede leer y guardar. Es una **interfaz**: los ViewModels dependen de ella
 * y no de Supabase. En la app se usa [SupabaseDaiRepository]; en las pruebas, un
 * repositorio en memoria (FakeDaiRepository).
 */
interface DaiRepository {

    /** La conversación guardada, del mensaje más viejo al más nuevo. */
    suspend fun history(): List<DaiMessage>

    /** Guarda un mensaje (del usuario o de Dai) y lo devuelve con su id y hora. */
    suspend fun save(role: DaiRole, text: String, card: DaiCard? = null): DaiMessage

    /** "Borrar historial": borra SOLO la conversación (no movimientos, metas ni presupuestos). */
    suspend fun clearHistory()

    /**
     * Los datos financieros del usuario para que Dai responda: movimientos de este mes
     * y del anterior, categorías, presupuestos con lo gastado y metas.
     */
    suspend fun snapshot(today: LocalDate, zone: ZoneId, weekStart: Int, userName: String): FinanceSnapshot

    /**
     * Guarda la factura APROBADA por el usuario como un gasto en `movements`
     * (`source = 'dai'`), a la hora [zone] del teléfono.
     */
    suspend fun saveReceipt(draft: ReceiptDraft, zone: ZoneId): Movement
}
