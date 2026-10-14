package com.pocketpal.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Íconos de categoría (§8.9: cuadrícula de 10 íconos, 2 filas × 5) mapeados a
 * Material Icons Extended. En la base se guarda solo el **identificador de
 * texto** ([id]) para que la fila no dependa de una clase de Android (RN-11).
 */
enum class CategoryIcon(val id: String, val icon: ImageVector) {
    Restaurant("restaurant", Icons.Outlined.Restaurant),
    Home("home", Icons.Outlined.Home),
    Favorite("favorite", Icons.Outlined.Favorite),
    Person("person", Icons.Outlined.Person),
    Calendar("calendar", Icons.Outlined.CalendarMonth),
    Mood("mood", Icons.Outlined.Mood),
    Dashboard("dashboard", Icons.Outlined.Dashboard),
    Star("star", Icons.Outlined.Star),
    Savings("savings", Icons.Outlined.Savings),
    Search("search", Icons.Outlined.Search);

    companion object {
        /** Orden de la cuadrícula del Figma (págs. 54 y 67). */
        val grid: List<CategoryIcon> = entries

        fun fromId(id: String): CategoryIcon = entries.firstOrNull { it.id == id } ?: Restaurant
    }
}

/**
 * Los 5 colores de categoría (§8.9: rosa/coral, mostaza, rojo oscuro, azul,
 * morado). Valores medidos del Figma (pág. 54); el hex se guarda tal cual.
 */
object CategoryColors {
    val palette: List<String> = listOf("#FF9B9B", "#EAB760", "#C90E0E", "#55A5EF", "#D194E5")

    val default: String = palette.first()

    /** Convierte el hex guardado en `categories.color` a un [Color] de Compose. */
    fun parse(hex: String): Color {
        val clean = hex.removePrefix("#").padStart(6, '0')
        val value = clean.toLongOrNull(16) ?: 0xFF9B9B
        return Color(0xFF000000 or value)
    }
}
