package com.pocketpal.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta semántica de la app (D-03). Cada pantalla lee los colores de aquí —
 * `pocketColors` — en vez de valores fijos, para que el tema oscuro se aplique
 * en toda la app (RF-B8) sin tocar cada pantalla otra vez.
 *
 * Dos grupos de roles:
 * - **Marca fijos** (Coral, Navy, NavyLight, Teal, Amber, ErrorRed, AvatarPink,
 *   BlueIcon, ToggleOff): idénticos en claro y oscuro (botones, splash y acentos
 *   conservan la identidad de la marca).
 * - **Superficies y textos** (Cream, Card, Ink, Muted, Gray, Rose, BlueSoft,
 *   TealSoft, RoseBorder, ChipPink, TabInactive, ErrorBg): cambian con el tema.
 */
data class PocketPalette(
    val Coral: Color,
    val Navy: Color,
    val NavyLight: Color,
    val Cream: Color,
    val Ink: Color,
    val Muted: Color,
    val Gray: Color,
    val Teal: Color,
    val Amber: Color,
    val ErrorRed: Color,
    val ErrorBg: Color,
    val AvatarPink: Color,
    val Rose: Color,
    val BlueSoft: Color,
    val TealSoft: Color,
    val BlueIcon: Color,
    val RoseBorder: Color,
    val ChipPink: Color,
    val TabInactive: Color,
    val ToggleOff: Color,
    val Card: Color,
)

/**
 * Valores fijos del diseño (tema claro). Se conservan como constantes para los
 * casos que NO deben cambiar con el tema (texto sobre superficies de marca o
 * blancas, p. ej. el botón de Google).
 */
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

/** Tema claro: los valores exactos del diseño (sin cambios visuales respecto a antes). */
val LightPocketPalette = PocketPalette(
    Coral = PocketColors.Coral,
    Navy = PocketColors.Navy,
    NavyLight = PocketColors.NavyLight,
    Cream = PocketColors.Cream,
    Ink = PocketColors.Ink,
    Muted = PocketColors.Muted,
    Gray = PocketColors.Gray,
    Teal = PocketColors.Teal,
    Amber = PocketColors.Amber,
    ErrorRed = PocketColors.ErrorRed,
    ErrorBg = PocketColors.ErrorBg,
    AvatarPink = PocketColors.AvatarPink,
    Rose = PocketColors.Rose,
    BlueSoft = PocketColors.BlueSoft,
    TealSoft = PocketColors.TealSoft,
    BlueIcon = PocketColors.BlueIcon,
    RoseBorder = PocketColors.RoseBorder,
    ChipPink = PocketColors.ChipPink,
    TabInactive = PocketColors.TabInactive,
    ToggleOff = PocketColors.ToggleOff,
    Card = Color.White,
)

/**
 * Tema oscuro (D-03): **derivado** de la paleta clara con los navy medidos de la
 * pág. 44 del Figma (#243447 aparece tanto en la paleta como relleno de la ficha
 * "Oscuro" de la pág. 42; #FFF9F7 es el crema de la paleta). **Sujeto a diseño:**
 * el Figma no trae pantallas en oscuro (§3, D-03), así que los valores son la
 * aproximación de esta implementación.
 */
val DarkPocketPalette = PocketPalette(
    Coral = PocketColors.Coral,
    Navy = PocketColors.Navy,
    NavyLight = PocketColors.NavyLight,
    Cream = Color(0xFF243447),      // fondo: navy del chip "Oscuro" (pág. 44)
    Ink = Color(0xFFFFF9F7),        // texto: crema de la paleta (pág. 44)
    Muted = Color(0xFFB2B7C0),      // texto secundario (págs. 36/42)
    Gray = Color(0xFF718096),       // gris azulado de la paleta (pág. 44)
    Teal = PocketColors.Teal,
    Amber = PocketColors.Amber,
    ErrorRed = PocketColors.ErrorRed,
    ErrorBg = Color(0xFF4A2A2A),
    AvatarPink = PocketColors.AvatarPink,
    Rose = Color(0xFF5C3C40),       // chips rosados oscurecidos (derivados)
    BlueSoft = Color(0xFF2C3E5A),
    TealSoft = Color(0xFF214A45),
    BlueIcon = PocketColors.BlueIcon,
    RoseBorder = Color(0xFF6B4448),
    ChipPink = Color(0xFF55393D),
    TabInactive = Color(0xFF33455F),
    ToggleOff = PocketColors.ToggleOff,
    Card = Color(0xFF2E4057),       // tarjetas: navy un poco más claro que el fondo
)

/** Paleta activa; la provee [PocketTheme] según la preferencia de apariencia. */
val LocalPocketColors: ProvidableCompositionLocal<PocketPalette> =
    staticCompositionLocalOf { LightPocketPalette }

/** Acceso a la paleta activa desde cualquier composable (reemplaza a `PocketColors.X`). */
val pocketColors: PocketPalette
    @Composable get() = LocalPocketColors.current
