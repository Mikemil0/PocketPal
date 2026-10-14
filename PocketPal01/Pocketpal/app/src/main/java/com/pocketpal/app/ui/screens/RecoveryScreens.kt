package com.pocketpal.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.theme.PocketColors

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        Modifier.size(64.dp).background(Color.White, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = PocketColors.Coral, modifier = Modifier.size(28.dp)) }
}

/** Paso 1 */
@Composable
fun ForgotEmailScreen(state: AuthState, onSend: (String) -> Unit, onBack: () -> Unit, onLogin: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val error = state as? AuthState.Error
    val bad = error?.type == ErrorType.InvalidEmail

    AuthScaffold(onBack = onBack) {
        Box(Modifier.align(Alignment.CenterHorizontally)) { IconBadge(Icons.Outlined.VisibilityOff) }
        Spacer(Modifier.height(16.dp))
        ScreenHeader("¿Olvidaste tu contraseña?", "No hay problema. Ingresa tu correo y te enviaremos un código para recuperarla.", centered = true)
        PocketOutlinedTextField(
            email, { email = it }, "Correo electrónico",
            trailingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email,
            isError = bad, errorText = if (bad) "Cuenta no encontrada" else null,
            imeAction = ImeAction.Done, onImeAction = { onSend(email) },
        )
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton("Enviar código", { onSend(email) }, loading = state is AuthState.Loading)
        Spacer(Modifier.height(32.dp))
        BottomLink("¿Recordaste tu contraseña?", "Inicia sesión", onLogin)
    }
}

/** Paso 3 */
@Composable
fun NewPasswordScreen(state: AuthState, onSave: (String, String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val error = state as? AuthState.Error

    AuthScaffold {
        Box(Modifier.align(Alignment.CenterHorizontally)) { IconBadge(Icons.Outlined.Lock) }
        Spacer(Modifier.height(16.dp))
        ScreenHeader("Crea una nueva contraseña", "", centered = true)
        PocketOutlinedTextField(
            password, { password = it }, "Nueva contraseña", isPassword = true,
            isError = error?.type == ErrorType.WeakPassword,
            errorText = if (error?.type == ErrorType.WeakPassword) error.message else null,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            confirm, { confirm = it }, "Confirmar contraseña", isPassword = true,
            isError = error?.type == ErrorType.PasswordMismatch,
            errorText = if (error?.type == ErrorType.PasswordMismatch) error.message else null,
        )
        Spacer(Modifier.height(10.dp))
        PasswordStrengthMeter(password, confirm)
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton("Guardar y continuar", { onSave(password, confirm) }, loading = state is AuthState.Loading)
    }
}

/** Paso 4 */
@Composable
fun PasswordUpdatedScreen(onLogin: () -> Unit) {
    AuthScaffold {
        Spacer(Modifier.height(80.dp))
        Icon(Icons.Outlined.CheckCircle, null, tint = PocketColors.Teal,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text("Contraseña actualizada", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            color = PocketColors.Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Tu contraseña se cambió con éxito. Ya puedes iniciar sesión con tu nueva clave.",
            fontSize = 12.sp, color = PocketColors.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().border(1.dp, PocketColors.Gray, RoundedCornerShape(10.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, null, tint = PocketColors.Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Por seguridad, cerramos tu sesión en todos los dispositivos en los que estuviera activa.",
                fontSize = 11.sp, color = PocketColors.Muted)
        }
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton("Iniciar sesión", onLogin)
    }
}
