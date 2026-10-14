package com.pocketpal.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun RegisterScreen(
    state: AuthState,
    onSubmit: (name: String, email: String, password: String, confirm: String, terms: Boolean) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var terms by rememberSaveable { mutableStateOf(false) }

    val error = state as? AuthState.Error
    val type = error?.type

    AuthScaffold(onBack = onBack) {
        ScreenHeader("Crea tu cuenta", "Empieza a llevar el control real de tus gastos en minutos.")
        StepProgressBar(current = 1)
        Spacer(Modifier.height(20.dp))

        PocketOutlinedTextField(
            name, { name = it }, "Nombre completo",
            trailingIcon = Icons.Outlined.Person,
            isError = type == ErrorType.InvalidName, errorText = error?.message,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            email, { email = it }, "Correo electrónico",
            trailingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            isError = type == ErrorType.InvalidEmail || type == ErrorType.EmailTaken,
            errorText = if (type == ErrorType.InvalidEmail) error?.message else null,
        )
        if (type == ErrorType.EmailTaken) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(error!!.message, actionText = "Inicia sesión o usa otro correo.", onAction = onLogin)
        }
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            password, { password = it }, "Contraseña", isPassword = true,
            isError = type == ErrorType.WeakPassword,
            errorText = if (type == ErrorType.WeakPassword) error?.message else null,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            confirm, { confirm = it }, "Verifica la contraseña", isPassword = true,
            isError = type == ErrorType.PasswordMismatch,
            errorText = if (type == ErrorType.PasswordMismatch) error?.message else null,
        )
        Spacer(Modifier.height(10.dp))
        PasswordStrengthMeter(password, confirm)

        Spacer(Modifier.height(16.dp))
        PocketPrimaryButton(
            "Crear cuenta",
            { onSubmit(name, email, password, confirm, terms) },
            loading = state is AuthState.Loading,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                terms, { terms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = PocketColors.Teal,
                    uncheckedColor = if (type == ErrorType.TermsNotAccepted) PocketColors.ErrorRed else PocketColors.Teal,
                ),
            )
            Text(
                buildAnnotatedString {
                    append("Acepto los ")
                    withStyle(SpanStyle(color = PocketColors.Coral)) { append("Términos y condiciones") }
                    append(" y la ")
                    withStyle(SpanStyle(color = PocketColors.Coral)) { append("Política de privacidad") }
                },
                fontSize = 11.sp, color = PocketColors.Ink,
                modifier = Modifier.padding(top = 12.dp).clickable { terms = !terms },
            )
        }
        if (type == ErrorType.TermsNotAccepted) {
            Text(error!!.message, fontSize = 11.sp, color = PocketColors.ErrorRed,
                modifier = Modifier.padding(start = 12.dp))
        }
    }
}
