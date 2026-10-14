package com.pocketpal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.auth.OtpMode
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay

/**
 * Pantalla OTP compartida: "Verifica tu correo" (registro) e "Ingresa el código" (recuperación).
 *
 * RF-A5: distingue dos estados de error.
 * - **Incorrecto:** cajas en rojo y mensaje con ícono de información.
 * - **Expirado:** lo decide el temporizador del cliente ([expiresAt], D-15); se muestra el
 *   estado "El código venció" con la acción Reenviar y **sin botón Verificar**.
 */
@Composable
fun OtpScreen(
    mode: OtpMode,
    email: String,
    state: AuthState,
    expiresAt: Long?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    val error = state as? AuthState.Error
    val expiredError = error?.type == ErrorType.ExpiredOtp
    val incorrect = error?.type == ErrorType.InvalidOtp
    val incomplete = error?.type == ErrorType.OtpIncomplete

    var remaining by remember(expiresAt) {
        mutableLongStateOf(if (expiresAt == null) Long.MAX_VALUE else expiresAt - System.currentTimeMillis())
    }
    LaunchedEffect(expiresAt) {
        if (expiresAt == null) return@LaunchedEffect
        while (true) {
            remaining = expiresAt - System.currentTimeMillis()
            if (remaining <= 0L) break
            delay(1_000)
        }
    }
    val expired = expiredError || (expiresAt != null && remaining <= 0L)
    val minutesLeft = ((remaining.coerceAtLeast(1L) + 59_999L) / 60_000L).toInt()

    AuthScaffold(onBack = if (mode == OtpMode.Recovery) onBack else null) {
        if (mode == OtpMode.Signup) { StepProgressBar(current = 2); Spacer(Modifier.height(32.dp)) }

        Box(
            Modifier.align(Alignment.CenterHorizontally).size(64.dp)
                .background(pocketColors.Card, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (mode == OtpMode.Signup) Icons.Outlined.Email else Icons.Outlined.Lock,
                null, tint = pocketColors.Coral, modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        if (expired) {
            ScreenHeader(
                stringResource(R.string.otp_expired_title),
                stringResource(R.string.error_expired_otp, email),
                centered = true,
            )
        } else {
            ScreenHeader(
                if (mode == OtpMode.Signup) stringResource(R.string.otp_title_signup)
                else stringResource(R.string.otp_title_recovery),
                stringResource(R.string.otp_sent_to, email),
                centered = true,
            )
        }
        Spacer(Modifier.height(16.dp))
        OtpInputRow(code, { code = it }, isError = expired || incorrect || incomplete)
        when {
            expired -> {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(stringResource(R.string.otp_expired_message), icon = Icons.Outlined.Info)
            }
            incorrect -> {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(stringResource(R.string.otp_incorrect_message), icon = Icons.Outlined.Info)
            }
            incomplete -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.error_otp_incomplete), fontSize = 11.sp,
                    color = pocketColors.ErrorRed, modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            mode == OtpMode.Recovery && expiresAt != null -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(R.plurals.otp_expires_in, minutesLeft, minutesLeft),
                    fontSize = 11.sp, color = pocketColors.Muted, modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        // RF-A5: el estado expirado no ofrece Verificar, solo Reenviar.
        if (!expired) {
            PocketPrimaryButton(
                stringResource(R.string.otp_verify), { onVerify(code) },
                loading = state is AuthState.Loading,
            )
            Spacer(Modifier.height(12.dp))
        }
        BottomLink(
            stringResource(R.string.otp_no_received),
            stringResource(R.string.otp_resend),
            onResend,
        )
        if (mode == OtpMode.Signup) {
            Spacer(Modifier.height(48.dp))
            Text(stringResource(R.string.otp_not_your_email), fontSize = 12.sp, color = pocketColors.Coral, fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onBack), textAlign = TextAlign.Center)
        }
    }
}
