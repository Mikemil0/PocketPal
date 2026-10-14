package com.pocketpal.app.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exportar datos (§7.2: "Rango, formato y estado vacío").
 *
 * D-05: en este módulo solo se implementa la interfaz y sus estados. La tabla
 * `movements` no existe todavía (Módulo 3), así que el único estado alcanzable
 * es "No hay movimientos que exportar" y el botón queda deshabilitado (RF-B9).
 * Sin repositorio todavía: la capa de consulta llega junto con los movimientos.
 */
class ExportViewModel : ViewModel() {

    /** Códigos neutros para el rango (RN-11: las etiquetas se traducen en la UI). */
    enum class Range { THIS_MONTH, LAST_3_MONTHS, THIS_YEAR, ALL, CUSTOM }

    /** Códigos neutros para el formato del archivo (RN-11). */
    enum class Format { CSV, PDF }

    data class UiState(
        val range: Range = Range.THIS_MONTH,
        val format: Format = Format.CSV,
        val customStart: Long? = null,
        val customEnd: Long? = null,
        /** §1.3: sin movimientos hasta el Módulo 3. */
        val hasMovements: Boolean = false,
    ) {
        /** RF-B9: sin movimientos en el rango, el botón queda deshabilitado. */
        val canExport: Boolean
            get() = hasMovements && (
                range != Range.CUSTOM ||
                    (customStart != null && customEnd != null && customStart <= customEnd)
                )
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun selectRange(range: Range) {
        _state.value = _state.value.copy(range = range)
    }

    fun selectFormat(format: Format) {
        _state.value = _state.value.copy(format = format)
    }

    fun setCustomRange(start: Long, end: Long) {
        _state.value = _state.value.copy(
            range = Range.CUSTOM,
            customStart = minOf(start, end),
            customEnd = maxOf(start, end),
        )
    }
}
