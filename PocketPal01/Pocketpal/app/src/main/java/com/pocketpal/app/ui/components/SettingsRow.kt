package com.pocketpal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Tarjeta agrupada de filas de Mi perfil (pág. 33): fondo blanco, borde rosado
 * redondeado y separadores finos entre filas.
 */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp)),
        content = content,
    )
}

/**
 * Fila de ajustes: ícono en caja de color, título, subtítulo y chevrón
 * (RF-B1: "cada fila: ícono de color en caja, título, subtítulo y chevrón").
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = Color.Unspecified,
    titleColor: Color = Color.Unspecified,
    showChevron: Boolean = true,
    dividerAbove: Boolean = false,
    onClick: () -> Unit,
) {
    // Sentinel Unspecified = color del tema (no se puede leer la paleta en los
    // valores por defecto de los parámetros).
    val resolvedTitleColor = if (titleColor == Color.Unspecified) pocketColors.Ink else titleColor
    Column(modifier.fillMaxWidth()) {
        if (dividerAbove) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(pocketColors.RoseBorder.copy(alpha = 0.6f)))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = resolvedTitleColor)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = pocketColors.Muted)
                }
            }
            if (value != null) {
                // Valor actual a la derecha, sin chevrón (filas de Preferencias, pág. 42)
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (valueColor == Color.Unspecified) pocketColors.Muted else valueColor,
                )
            }
            if (showChevron && value == null) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    null,
                    tint = pocketColors.Gray,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
