package com.pocketpal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.AuthStep
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun NoWifiScreen(onRetry: () -> Unit) {
    AuthScaffold {
        Spacer(Modifier.height(80.dp))
        Icon(Icons.Outlined.WifiOff, null, tint = PocketColors.Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(96.dp))
        Spacer(Modifier.height(24.dp))
        ScreenHeader("Sin conexión a internet", "No pudimos conectarnos. Revisa tu wifi o datos móviles e inténtalo de nuevo.", centered = true)
        Row(
            Modifier.fillMaxWidth().border(1.dp, PocketColors.Gray, RoundedCornerShape(10.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = PocketColors.Teal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Tranquilo: puedes continuar con el formulario una vez te conectes a una red.",
                fontSize = 12.sp, color = PocketColors.Ink)
        }
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton("Reintentar", onRetry)
    }
}

@Composable
fun SessionExpiredScreen(email: String, onLogin: () -> Unit, onOtherAccount: () -> Unit) {
    AuthScaffold(background = PocketColors.Navy) {
        Spacer(Modifier.height(100.dp))
        Icon(Icons.Outlined.AccessTime, null, tint = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Tu sesión expiró", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Por seguridad, cerramos tu sesión tras un tiempo de inactividad. Vuelve a ingresar para continuar.",
            fontSize = 12.sp, color = PocketColors.Coral, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (email.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().background(PocketColors.NavyLight, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).background(Color.LightGray, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(email, fontSize = 12.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton("Iniciar sesión de nuevo", onLogin)
        Spacer(Modifier.height(16.dp))
        Text("Usar otra cuenta", fontSize = 12.sp, color = PocketColors.Muted,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOtherAccount), textAlign = TextAlign.Center)
    }
}

/** Loading / Éxito / Error del flujo de Google. */
@Composable
fun GoogleAuthScreen(state: AuthState, onContinue: () -> Unit, onRetry: () -> Unit, onUseEmail: () -> Unit) {
    val success = state is AuthState.Success<*> && state.step == AuthStep.GoogleConnected
    val failed = state is AuthState.Error && state.type == ErrorType.GoogleFailed

    AuthScaffold {
        Spacer(Modifier.height(120.dp))
        when {
            success -> {
                Icon(Icons.Outlined.CheckCircle, null, tint = PocketColors.Teal,
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(110.dp))
                Spacer(Modifier.height(24.dp))
                ScreenHeader("Cuenta conectada correctamente", "", centered = true)
                Spacer(Modifier.height(120.dp))
                PocketPrimaryButton("Continuar", onContinue)
            }
            failed -> {
                Icon(Icons.Outlined.Cancel, null, tint = PocketColors.ErrorRed,
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(110.dp))
                Spacer(Modifier.height(24.dp))
                ScreenHeader("No pudimos conectar tu cuenta.",
                    "No pudimos conectar con tu cuenta de Google. Revisa tu conexión e intenta de nuevo.", centered = true)
                Spacer(Modifier.height(80.dp))
                PocketPrimaryButton("Intentar de nuevo", onRetry)
                Spacer(Modifier.height(16.dp))
                Text("Usar correo y contraseña", fontSize = 12.sp, color = PocketColors.Muted,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onUseEmail), textAlign = TextAlign.Center)
            }
            else -> {
                CircularProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally).size(72.dp),
                    color = PocketColors.Muted, strokeWidth = 5.dp,
                )
                Spacer(Modifier.height(24.dp))
                ScreenHeader("Conectando con Google", "Esto tomará un momento, no cierres la aplicación.", centered = true)
            }
        }
    }
}
