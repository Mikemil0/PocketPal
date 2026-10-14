package com.pocketpal.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-05 / RF-B9 (SDD_Modulo2.md §8.12) sobre [ExportViewModel]: rango, formato y
 * estado vacío. Sin movimientos (§1.3: la tabla `movements` es del Módulo 3) el
 * botón de exportar siempre queda deshabilitado.
 */
class ExportViewModelTest {

    @Test
    fun `estado inicial mes actual, csv y sin movimientos`() {
        val state = ExportViewModel().state.value
        assertEquals(ExportViewModel.Range.THIS_MONTH, state.range)
        assertEquals(ExportViewModel.Format.CSV, state.format)
        assertFalse(state.hasMovements)
        assertFalse(state.canExport)
    }

    @Test
    fun `seleccionar rango actualiza el estado`() {
        val vm = ExportViewModel()
        vm.selectRange(ExportViewModel.Range.LAST_3_MONTHS)
        assertEquals(ExportViewModel.Range.LAST_3_MONTHS, vm.state.value.range)
    }

    @Test
    fun `seleccionar formato actualiza el estado`() {
        val vm = ExportViewModel()
        vm.selectFormat(ExportViewModel.Format.PDF)
        assertEquals(ExportViewModel.Format.PDF, vm.state.value.format)
    }

    @Test
    fun `rango personalizado guarda inicio y fin ordenados`() {
        val vm = ExportViewModel()
        vm.setCustomRange(end = 2_000L, start = 1_000L)
        val state = vm.state.value
        assertEquals(ExportViewModel.Range.CUSTOM, state.range)
        assertEquals(1_000L, state.customStart)
        assertEquals(2_000L, state.customEnd)
    }

    @Test
    fun `sin movimientos nunca se puede exportar`() {
        val vm = ExportViewModel()
        vm.setCustomRange(start = 1_000L, end = 2_000L)
        assertFalse(vm.state.value.canExport)
    }

    @Test
    fun `con movimientos se exporta salvo rango personalizado incompleto`() {
        val completo = ExportViewModel.UiState(
            range = ExportViewModel.Range.CUSTOM,
            customStart = 1_000L,
            customEnd = 2_000L,
            hasMovements = true,
        )
        val sinFechas = ExportViewModel.UiState(
            range = ExportViewModel.Range.CUSTOM,
            hasMovements = true,
        )
        val esteMes = ExportViewModel.UiState(hasMovements = true)
        assertTrue(completo.canExport)
        assertFalse(sinFechas.canExport)
        assertTrue(esteMes.canExport)
    }
}
