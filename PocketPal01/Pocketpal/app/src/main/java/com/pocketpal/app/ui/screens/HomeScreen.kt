package com.pocketpal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun HomeScreen(name: String, onSignOut: () -> Unit) {
    ScrollableScreen(verticalArrangement = Arrangement.Center) {
        Text(
            if (name.isBlank()) "¡Bienvenido!" else "¡Bienvenido, $name!",
            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PocketColors.Ink, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text("Pronto habrá más zonas por descubrir.", fontSize = 14.sp, color = PocketColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        Text("Cerrar sesión", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PocketColors.Coral,
            modifier = Modifier.clickable(onClick = onSignOut))
    }
}