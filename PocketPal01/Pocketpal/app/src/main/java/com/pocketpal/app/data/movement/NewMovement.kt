package com.pocketpal.app.data.movement

import java.time.LocalDate

/**
 * Módulo 6 · Flujo 5: un movimiento NUEVO que el usuario registró desde el botón **+** del
 * inicio (escáner, voz o manual), antes de guardarlo en `public.movements`.
 *
 * @property title lo que se ve en la lista de movimientos (el motivo o la categoría).
 * @property source quién lo creó: [SOURCE_MANUAL] (formulario) o [SOURCE_DAI] (voz o
 * escáner, porque los datos los sacó Dai). Son los valores que permite el `check` de la
 * columna `source` (ver `20261026000000_module4_movements.sql`).
 */
data class NewMovement(
    val kind: MovementKind,
    val title: String,
    val amount: Double,
    val date: LocalDate,
    val categoryId: String?,
    val note: String,
    val source: String,
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_DAI = "dai"
        const val TITLE_MAX = 60
        const val NOTE_MAX = 200
    }
}
