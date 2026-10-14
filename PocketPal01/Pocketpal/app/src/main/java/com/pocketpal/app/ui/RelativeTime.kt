package com.pocketpal.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pocketpal.app.R
import java.time.OffsetDateTime

/**
 * Tiempos relativos con plurales (§5.2: "hace 2 meses", "Activa"). Devuelve el texto
 * listo para mostrar a partir de un instante ISO; "" si no se puede leer.
 */
@Composable
fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val millis = try {
        OffsetDateTime.parse(iso).toInstant().toEpochMilli()
    } catch (e: Exception) {
        return ""
    }
    val diff = System.currentTimeMillis() - millis
    if (diff < 0) return ""
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> stringResource(R.string.relative_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.relative_minutes, minutes.toInt(), minutes.toInt())
        minutes < 60 * 24 -> {
            val hours = minutes / 60
            pluralStringResource(R.plurals.relative_hours, hours.toInt(), hours.toInt())
        }
        minutes < 60 * 24 * 30 -> {
            val days = minutes / (60 * 24)
            pluralStringResource(R.plurals.relative_days, days.toInt(), days.toInt())
        }
        else -> {
            val months = minutes / (60 * 24 * 30)
            pluralStringResource(R.plurals.relative_months, months.toInt(), months.toInt())
        }
    }
}
