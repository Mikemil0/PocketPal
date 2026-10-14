package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.BottomLink
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.OtpInputRow
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Verifica tus mensajes" (pág. 18, §8.4): código de 6 dígitos, Verificar, Reenviar
 * y "¿Ese no es tu número?". Estados del código según RF-A5 (incorrecto/expirado)
 * con temporizador real de 10 minutos (D-15). El ícono es de mensaje, no de sobre
 * (§8.4: es un SMS).
 */
@Composable
fun PhoneCodeScreen(
    prefix: String,
    number: String,
    code: String,
    codeExpiresAt: Long,
    sending: Boolean,
    verifying: Boolean,
    formError: ErrorType?,
    error: ErrorType?,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onNotYourNumber: () -> Unit,
    onFormErrorShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    var shownFormError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(formError) {
        if (formError != null) {
            shownFormError = formError
            onFormErrorShown()
        }
    }
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
        }
    }

    // D-15 / RF-A5: el temporizador del cliente distingue el código expirado.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(codeExpiresAt) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val minutesLeft = ((codeExpiresAt - now) / 60_000).coerceAtLeast(0).toInt()

    val codeInvalid = shownFormError == ErrorType.InvalidOtp ||
        shownFormError == ErrorType.ExpiredOtp ||
        shownFormError == ErrorType.OtpIncomplete

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.phone_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = pocketColors.Card,
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Sms,
                        null,
                        tint = pocketColors.Coral,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.phone_code_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.phone_code_sent_to))
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = pocketColors.Ink)) {
                        append("$prefix $number")
                    }
                },
                fontSize = 13.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        OtpInputRow(
            isError = codeInvalid,
            value = code,
            onValueChange = {
                onCodeChange(it)
                if (shownFormError != null) shownFormError = null
            },
        )

        if (codeExpiresAt > now) {
            Spacer(Modifier.height(10.dp))
            Text(
                pluralStringResource(R.plurals.otp_expires_in, minutesLeft, minutesLeft),
                fontSize = 11.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (shownFormError != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(shownFormError!!.messageRes()))
        }
        if (shownError != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(shownError!!.messageRes()))
        }

        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.phone_verify),
            onClick = onVerify,
            enabled = !verifying && !sending,
            loading = verifying,
        )
        Spacer(Modifier.height(16.dp))
        BottomLink(
            prefix = stringResource(R.string.otp_no_received),
            action = stringResource(R.string.otp_resend),
            onClick = onResend,
        )

        Spacer(Modifier.height(120.dp))
        Text(
            stringResource(R.string.phone_not_your_number),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = pocketColors.Coral,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNotYourNumber),
        )
        Spacer(Modifier.height(24.dp))
    }
}
