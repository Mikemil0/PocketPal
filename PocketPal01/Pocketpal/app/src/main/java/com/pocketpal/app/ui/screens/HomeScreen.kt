package com.pocketpal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.Avatar
import com.pocketpal.app.ui.components.ScrollableScreen
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * RF-A1: ícono circular de 40 dp arriba a la derecha — la foto del perfil si existe;
 * si no, un círculo con la inicial del nombre (o la del correo). Al tocarlo abre
 * "Mi perfil". El resto del Home no cambia (D-14: el enlace de cerrar sesión se
 * mantiene hasta retirarlo al final del módulo).
 */
@Composable
fun HomeScreen(
    name: String,
    email: String,
    avatarUrl: String?,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
) {
    val firstName = name.trim().substringBefore(" ")

    Box(Modifier.fillMaxSize()) {
        ScrollableScreen(verticalArrangement = Arrangement.Center) {
            Text(
                if (firstName.isBlank()) stringResource(R.string.home_welcome)
                else stringResource(R.string.home_welcome_name, firstName),
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_subtitle), fontSize = 14.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.home_sign_out), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pocketColors.Coral,
                modifier = Modifier.clickable(onClick = onSignOut))
        }

        // RNF-07: área táctil de 48 dp alrededor del ícono de 40 dp.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 4.dp)
                .size(48.dp)
                .clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                size = 40.dp,
                contentDescription = stringResource(R.string.home_open_profile),
            )
        }
    }
}
