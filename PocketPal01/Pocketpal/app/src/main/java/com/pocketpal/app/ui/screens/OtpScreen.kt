package com.pocketpal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.auth.OtpMode
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

/** Pantalla OTP compartida: "Verifica tu correo" (registro) e "Ingresa el código" (recuperación). */
@Composable
fun OtpScreen(
    mode: OtpMode,
    email: String,
    state: AuthState,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val error = state as? AuthState.Error
    val invalid = error?.type == ErrorType.InvalidOtp

    AuthScaffold(onBack = if (mode == OtpMode.Recovery) onBack else null) {
        if (mode == OtpMode.Signup) { StepProgressBar(current = 2); Spacer(Modifier.height(32.dp)) }

        Box(
            Modifier.align(Alignment.CenterHorizontally).size(64.dp)
                .background(Color.White, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (mode == OtpMode.Signup) Icons.Outlined.Email else Icons.Outlined.Lock,
                null, tint = PocketColors.Coral, modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenHeader(
            if (mode == OtpMode.Signup) "Verifica tu correo" else "Ingresa el código",
            "Enviamos un código de 6 dígitos a $email",
            centered = true,
        )
        Spacer(Modifier.height(16.dp))
        OtpInputRow(code, { code = it }, isError = invalid)
        if (invalid) {
            Spacer(Modifier.height(8.dp))
            Text(error!!.message, fontSize = 11.sp, color = PocketColors.ErrorRed, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else if (mode == OtpMode.Recovery) {
            Spacer(Modifier.height(8.dp))
            Text("El código expira en 10 minutos", fontSize = 11.sp, color = PocketColors.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
        PocketPrimaryButton("Verificar", { onVerify(code) }, loading = state is AuthState.Loading)
        Spacer(Modifier.height(12.dp))
        BottomLink("¿No recibiste el código?", "Reenviar", onResend)
        if (mode == OtpMode.Signup) {
            Spacer(Modifier.height(48.dp))
            Text("¿Ese no es tu correo?", fontSize = 12.sp, color = PocketColors.Coral, fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onBack), textAlign = TextAlign.Center)
        }
    }
}
