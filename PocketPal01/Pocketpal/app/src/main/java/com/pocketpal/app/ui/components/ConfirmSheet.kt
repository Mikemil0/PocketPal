package com.pocketpal.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Hoja inferior de confirmación (pág. 52: "¿Cerrar sesión?"): ícono en caja rosada,
 * título, texto explicativo, botón de acción coral y botón "Cancelar" de contorno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    icon: ImageVector,
    title: String,
    body: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = pocketColors.Cream,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(pocketColors.Rose, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = pocketColors.Coral, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                fontSize = 13.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            PocketPrimaryButton(confirmText, onConfirm)
            Spacer(Modifier.height(12.dp))
            PocketPrimaryButton(
                cancelText,
                onDismiss,
                containerColor = pocketColors.Card,
                contentColor = pocketColors.Muted,
                border = BorderStroke(1.dp, pocketColors.Gray),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
