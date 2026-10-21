package com.pocketpal.app.ui.goals

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatos que comparten las pantallas de Metas.
 *
 * - Fecha "bonita" del Figma: "15 dic 2026" (en el idioma de la app).
 * - Fecha de los campos: según la preferencia "Formato de fecha" del Módulo 2
 *   (`DD/MM/YYYY`, `MM/DD/YYYY` o `YYYY-MM-DD`).
 * - Montos cortos de las tarjetas del Home: "$300k", "$1,7M".
 */
object GoalFormat {

    /** "15 dic 2026" (sin el punto que algunos idiomas ponen en el mes abreviado). */
    fun prettyDate(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(date).replace(".", "")

    /** Fecha con el formato elegido por el usuario en Preferencias generales. */
    fun inputDate(date: LocalDate, preference: String): String =
        DateTimeFormatter.ofPattern(patternOf(preference)).format(date)

    /** Texto guía del campo de fecha según la preferencia ("DD/MM/AAAA" en español). */
    fun placeholder(preference: String, locale: Locale): String {
        val spanishLike = locale.language in setOf("es", "pt", "fr")
        val year = if (spanishLike) "AAAA" else "YYYY"
        return when (preference) {
            "MM/DD/YYYY" -> "MM/DD/$year"
            "YYYY-MM-DD" -> "$year-MM-DD"
            else -> "DD/MM/$year"
        }
    }

    /**
     * Monto corto: "$950", "$300k", "$1,7M". Se usa en las tarjetas pequeñas del Home,
     * donde el monto completo no cabe.
     */
    fun compactMoney(amount: Double): String {
        val value = amount.coerceAtLeast(0.0)
        return when {
            value >= 1_000_000 -> {
                val millions = Math.floor(value / 100_000) / 10.0
                val text = if (millions % 1.0 == 0.0) millions.toLong().toString()
                else millions.toString().replace('.', ',')
                "$${text}M"
            }
            value >= 1_000 -> "$${(value / 1_000).toLong()}k"
            else -> "$${value.toLong()}"
        }
    }

    /** Milisegundos UTC que usa el selector de fecha de Material ↔ [LocalDate]. */
    fun toPickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun patternOf(preference: String): String = when (preference) {
        "MM/DD/YYYY" -> "MM/dd/yyyy"
        "YYYY-MM-DD" -> "yyyy-MM-dd"
        else -> "dd/MM/yyyy"
    }
}
