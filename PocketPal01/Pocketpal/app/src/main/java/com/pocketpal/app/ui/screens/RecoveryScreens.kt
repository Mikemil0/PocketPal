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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.AuthState
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.auth.isPasswordProblem
import com.pocketpal.app.ui.components.*
import com.pocketpal.app.ui.errorMessage
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        Modifier.size(64.dp).background(pocketColors.Card, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = pocketColors.Coral, modifier = Modifier.size(28.dp)) }
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
        ScreenHeader(stringResource(R.string.forgot_title), stringResource(R.string.forgot_subtitle), centered = true)
        PocketOutlinedTextField(
            email, { email = it }, stringResource(R.string.field_email),
            trailingIcon = Icons.Outlined.Email, keyboardType = KeyboardType.Email,
            // §14 fila 18: no se informa si el correo tiene cuenta (evita enumeración de cuentas);
            // solo se valida el formato.
            isError = bad, errorText = if (bad) error?.errorMessage() else null,
            imeAction = ImeAction.Done, onImeAction = { onSend(email) },
        )
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(stringResource(R.string.forgot_send), { onSend(email) }, loading = state is AuthState.Loading)
        Spacer(Modifier.height(32.dp))
        BottomLink(stringResource(R.string.forgot_remember), stringResource(R.string.login_signin_link), onLogin)
    }
}

/** Paso 3 */
@Composable
fun NewPasswordScreen(state: AuthState, onSave: (String, String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val error = state as? AuthState.Error
    val badPassword = error?.type?.isPasswordProblem == true

    AuthScaffold {
        Box(Modifier.align(Alignment.CenterHorizontally)) { IconBadge(Icons.Outlined.Lock) }
        Spacer(Modifier.height(16.dp))
        ScreenHeader(stringResource(R.string.new_password_title), "", centered = true)
        PocketOutlinedTextField(
            password, { password = it }, stringResource(R.string.field_new_password), isPassword = true,
            isError = badPassword,
            errorText = if (badPassword) error?.errorMessage() else null,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            confirm, { confirm = it }, stringResource(R.string.field_new_password_confirm), isPassword = true,
            isError = error?.type == ErrorType.PasswordMismatch,
            errorText = if (error?.type == ErrorType.PasswordMismatch) error.errorMessage() else null,
        )
        Spacer(Modifier.height(10.dp))
        PasswordStrengthMeter(password, confirm)
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(stringResource(R.string.new_password_save), { onSave(password, confirm) }, loading = state is AuthState.Loading)
    }
}

/** Paso 4 */
@Composable
fun PasswordUpdatedScreen(onLogin: () -> Unit) {
    AuthScaffold {
        Spacer(Modifier.height(80.dp))
        Icon(Icons.Outlined.CheckCircle, null, tint = pocketColors.Teal,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.password_updated_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.password_updated_body),
            fontSize = 12.sp, color = pocketColors.Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().border(1.dp, pocketColors.Gray, RoundedCornerShape(10.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lock, null, tint = pocketColors.Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.password_updated_note),
                fontSize = 11.sp, color = pocketColors.Muted)
        }
        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(stringResource(R.string.common_sign_in), onLogin)
    }
}
