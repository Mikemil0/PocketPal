package com.pocketpal.app.ui.movements

import com.pocketpal.app.data.movement.DateRange
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.ui.MoneyFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatos de las pantallas de Movimientos (fechas, horas y montos con signo).
 * Funciones puras: reciben el idioma ([Locale]) y no dependen de Compose.
 */
object MovementFormat {

    /** "- $86.450" para gastos y "+ $1.200.000" para ingresos. */
    fun signedAmount(movement: Movement, currency: String): String =
        (if (movement.isExpense) "- " else "+ ") + MoneyFormat.format(movement.amount, currency)

    /** "12 abr 2025" (sin el punto del mes abreviado). */
    fun date(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date).replace(".", "")

    /** "14 sep" (para las filas de búsqueda). */
    fun shortDate(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM", locale).format(date).replace(".", "")

    /** "10:24 a. m." / "10:24 AM" según el idioma. */
    fun time(dateTime: ZonedDateTime, locale: Locale): String =
        DateTimeFormatter.ofPattern("h:mm a", locale).format(dateTime)

    /** "12 abr 2025 · 10:24 a. m." (detalle del movimiento). */
    fun dateTime(dateTime: ZonedDateTime, locale: Locale): String =
        "${date(dateTime.toLocalDate(), locale)} · ${time(dateTime, locale)}"

    /** "abril 2025" con mayúscula inicial → "Abril 2025" (título del calendario). */
    fun monthTitle(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(date)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    /** "Abr 1 - Abr 30, 2025" (encabezado del filtro por fechas). */
    fun rangeLabel(range: DateRange, locale: Locale): String {
        val day = DateTimeFormatter.ofPattern("MMM d", locale)
        fun cap(s: String) = s.replace(".", "").replaceFirstChar { it.titlecase(locale) }
        val start = cap(day.format(range.start))
        val end = cap(day.format(range.end))
        return if (range.start.year == range.end.year) {
            "$start - $end, ${range.end.year}"
        } else {
            "$start, ${range.start.year} - $end, ${range.end.year}"
        }
    }
}
