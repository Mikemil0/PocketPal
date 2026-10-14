package com.pocketpal.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun WelcomeScreen(onGoogle: () -> Unit, onRegister: () -> Unit, onLogin: () -> Unit) {
    ScrollableScreen(background = PocketColors.Navy) {
        // Spacer vacío arriba: con SpaceBetween deja el logo centrado en el espacio sobre los botones.
        Spacer(Modifier.height(0.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo_pocketpal),
                contentDescription = "PocketPal",
                modifier = Modifier.size(150.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Pocket") }
                    withStyle(SpanStyle(color = PocketColors.Coral)) { append("Pal") }
                },
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp)) // separación mínima cuando hay que hacer scroll
            PocketGoogleButton("Continuar con Google", onGoogle)
            Spacer(Modifier.height(12.dp))
            Text("ó", color = PocketColors.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            PocketPrimaryButton("Crear cuenta", onRegister)
            Spacer(Modifier.height(10.dp))
            PocketSecondaryButton("Ya tengo una cuenta", onLogin)
            Spacer(Modifier.height(16.dp))
            Text(
                "Al continuar aceptas nuestros Términos y Política de privacidad.",
                color = PocketColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
