package com.pocketpal.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PocketColors {
    val Coral = Color(0xFFFF6B6B)
    val Navy = Color(0xFF1B3050)
    val NavyLight = Color(0xFF2C4263)
    val Cream = Color(0xFFFFF8F5)
    val Ink = Color(0xFF1F2A44)
    val Muted = Color(0xFF6B7690)
    val Gray = Color(0xFFC4C9D4)
    val Teal = Color(0xFF2EC4A6)
    val Amber = Color(0xFFF2B84B)
    val ErrorRed = Color(0xFFD32020)
    val ErrorBg = Color(0xFFFFE5E5)

    // Fase 3: tonos rosados y pastel del perfil (valores aproximados del Figma, §2)
    val AvatarPink = Color(0xFFF79B97)   // círculo del avatar con la inicial
    val Rose = Color(0xFFFBDBD7)         // chip rosa de las filas y la hoja de foto
    val BlueSoft = Color(0xFFDFE6F7)     // chip azul de las filas
    val TealSoft = Color(0xFFD3EFE6)     // chip verde de las filas
    val BlueIcon = Color(0xFF7B95D6)     // tinte de ícono sobre chip azul
    val RoseBorder = Color(0xFFF4AAAA)   // borde de campos, tarjetas y contadores

    // Fase 6: categorías (valores medidos del Figma, págs. 37 y 54)
    val ChipPink = Color(0xFFFFE4E4)     // fondo del ícono de categoría y del botón atrás
    val TabInactive = Color(0xFFCBD8EC)  // pestaña "Ingresos" inactiva
    val ToggleOff = Color(0xFF718096)    // interruptor apagado (gris azulado, pág. 54)
}

@Composable
fun PocketTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = PocketColors.Coral,
        onPrimary = Color.White,
        background = PocketColors.Cream,
        surface = PocketColors.Cream,
        error = PocketColors.ErrorRed,
    ),
    content = content,
)
