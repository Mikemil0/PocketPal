package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PasswordStrengthMeter
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Cambiar contraseña" (págs. 53 y 66, §8.6 / RF-B4): contraseña actual con enlace
 * de recuperación, nueva contraseña con medidor de fuerza y reglas (RN-04) y botón
 * "Actualizar contraseña". Errores: "Contraseña incorrecta" en el campo actual y
 * "La nueva contraseña no debe ser igual a la actual" (pág. 66).
 */
@Composable
fun ChangePasswordScreen(
    saving: Boolean,
    formError: ErrorType?,
    error: ErrorType?,
    onBack: () -> Unit,
    onChangePassword: (current: String, new: String, confirm: String) -> Unit,
    onForgotPassword: () -> Unit,
    onFormErrorShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    // Los errores del formulario se muestran junto al campo que los causó y se consumen.
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

    val currentWrong = shownFormError == ErrorType.CurrentPasswordIncorrect
    val rulesError = shownFormError?.takeIf {
        it != ErrorType.CurrentPasswordIncorrect && it != ErrorType.EmptyPassword
    }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.password_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.password_subtitle),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(20.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        PocketOutlinedTextField(
            value = current,
            onValueChange = {
                current = it
                if (currentWrong) shownFormError = null
            },
            label = stringResource(R.string.password_current),
            placeholder = stringResource(R.string.field_password),
            isPassword = true,
            isError = currentWrong,
            errorText = if (currentWrong) stringResource(R.string.error_current_password_incorrect) else null,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            borderColor = pocketColors.RoseBorder,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.password_forgot),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = pocketColors.Coral,
            modifier = Modifier
                .clickable(onClick = onForgotPassword)
                .padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(pocketColors.Gray))
        Spacer(Modifier.height(16.dp))

        PocketOutlinedTextField(
            value = new,
            onValueChange = { new = it },
            label = stringResource(R.string.password_new),
            placeholder = stringResource(R.string.field_password),
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            borderColor = pocketColors.RoseBorder,
        )
        Spacer(Modifier.height(16.dp))

        PocketOutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = stringResource(R.string.password_confirm),
            placeholder = stringResource(R.string.field_password_confirm),
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            borderColor = pocketColors.RoseBorder,
        )
        Spacer(Modifier.height(16.dp))

        PasswordStrengthMeter(password = new, confirm = confirm)

        if (rulesError != null) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(stringResource(rulesError.messageRes()))
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.password_submit),
            onClick = { onChangePassword(current, new, confirm) },
            enabled = !saving,
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }
}
