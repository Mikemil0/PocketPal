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
