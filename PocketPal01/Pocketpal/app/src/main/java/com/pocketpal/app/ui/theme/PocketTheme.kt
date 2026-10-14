package com.pocketpal.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Tema de la app (D-03 / RF-B8): aplica la paleta clara u oscura según la
 * preferencia de apariencia ("light", "dark" o "system" = Automático).
 *
 * La preferencia vive en DataStore (RNF-06) y llega como parámetro desde
 * `MainActivity`, así que cambiarla en Preferencias generales se aplica al
 * instante en toda la app. Los roles de `MaterialTheme` se mapean de la paleta
 * para que los componentes de Material 3 sigan el tema.
 */
@Composable
fun PocketTheme(
    theme: String = "system",
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPocketPalette else LightPocketPalette
    CompositionLocalProvider(LocalPocketColors provides palette) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = palette.Coral,
                    onPrimary = Color.White,
                    background = palette.Cream,
                    surface = palette.Card,
                    onSurface = palette.Ink,
                    error = palette.ErrorRed,
                )
            } else {
                lightColorScheme(
                    primary = palette.Coral,
                    onPrimary = Color.White,
                    background = palette.Cream,
                    surface = palette.Cream,
                    error = palette.ErrorRed,
                )
            },
            content = content,
        )
    }
}
