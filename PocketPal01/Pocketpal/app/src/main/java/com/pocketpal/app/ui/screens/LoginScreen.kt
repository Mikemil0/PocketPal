package com.pocketpal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

@Composable
fun LoginScreen(
    state: AuthState,
    onLogin: (email: String, password: String, remember: Boolean) -> Unit,
    onBack: () -> Unit,
    onForgot: () -> Unit,
    onRegister: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var remember by rememberSaveable { mutableStateOf(false) }

    val error = state as? AuthState.Error
    val badCreds = error?.type == ErrorType.InvalidCredentials
    val badEmail = error?.type == ErrorType.InvalidEmail

    AuthScaffold(onBack = onBack) {
        ScreenHeader("Bienvenido de nuevo", "Ingresa a tu cuenta para seguir controlando tu dinero.")

        if (badCreds) {
            ErrorBanner(error!!.message)
            Spacer(Modifier.height(16.dp))
        }
        PocketOutlinedTextField(
            email, { email = it }, "Correo electrónico",
            trailingIcon = Icons.Outlined.Email,
            isError = badCreds || badEmail,
            errorText = if (badEmail) error?.message else null,
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(16.dp))
        PocketOutlinedTextField(
            password, { password = it }, "Contraseña",
            isPassword = true,
            isError = badCreds,
            errorText = if (badCreds) "Esta combinación no coincide con ninguna cuenta." else null,
            imeAction = ImeAction.Done,
            onImeAction = { onLogin(email, password, remember) },
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                remember, { remember = it },
                colors = CheckboxDefaults.colors(checkedColor = PocketColors.Teal),
            )
            Text("Recordarme", fontSize = 12.sp, color = PocketColors.Muted)
            Spacer(Modifier.weight(1f))
            Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = PocketColors.Coral,
                modifier = Modifier.clickable(onClick = onForgot))
        }
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(
            if (badCreds) "Intentar de nuevo" else "Iniciar sesión",
            { onLogin(email, password, remember) },
            loading = state is AuthState.Loading,
        )
        Spacer(Modifier.height(32.dp))
        BottomLink("¿No tienes cuenta?", "Regístrate", onRegister)
    }
}
