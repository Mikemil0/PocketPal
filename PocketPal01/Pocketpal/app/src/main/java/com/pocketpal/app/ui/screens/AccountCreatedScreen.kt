package com.pocketpal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.components.StepProgressBar
import com.pocketpal.app.ui.theme.PocketColors

/** "VER - COD" paso final: correo verificado. */
@Composable
fun AccountCreatedScreen(onContinue: () -> Unit) {
    ScrollableScreen {
        StepProgressBar(current = 3)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Outlined.CheckCircle, null, tint = PocketColors.Teal, modifier = Modifier.size(110.dp))
            Spacer(Modifier.height(24.dp))
            Text("Tu cuenta ha sido creada", fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                color = PocketColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Ya puedes iniciar sesión con tus nuevos datos.", fontSize = 12.sp,
                color = PocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }

        PocketPrimaryButton("Iniciar sesión", onContinue)
    }
}
