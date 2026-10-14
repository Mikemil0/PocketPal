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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
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
    val badPassword = type?.isPasswordProblem == true

    val termsPrefix = stringResource(R.string.register_terms_prefix)
    val termsLink = stringResource(R.string.register_terms_terms)
    val termsMiddle = stringResource(R.string.register_terms_middle)
    val privacyLink = stringResource(R.string.register_terms_privacy)

    AuthScaffold(onBack = onBack) {
        ScreenHeader(stringResource(R.string.register_title), stringResource(R.string.register_subtitle))
        StepProgressBar(current = 1)
        Spacer(Modifier.height(20.dp))

        PocketOutlinedTextField(
            name, { name = it }, stringResource(R.string.field_full_name),
            trailingIcon = Icons.Outlined.Person,
            isError = type == ErrorType.InvalidName,
            errorText = if (type == ErrorType.InvalidName) error?.errorMessage() else null,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            email, { email = it }, stringResource(R.string.field_email),
            trailingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            isError = type == ErrorType.InvalidEmail || type == ErrorType.EmailTaken,
            errorText = if (type == ErrorType.InvalidEmail) error?.errorMessage() else null,
        )
        if (type == ErrorType.EmailTaken) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(
                error!!.errorMessage(),
                actionText = stringResource(R.string.email_taken_action),
                onAction = onLogin,
            )
        }
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            password, { password = it }, stringResource(R.string.field_password), isPassword = true,
            isError = badPassword,
            errorText = if (badPassword) error?.errorMessage() else null,
        )
        Spacer(Modifier.height(14.dp))
        PocketOutlinedTextField(
            confirm, { confirm = it }, stringResource(R.string.field_password_confirm), isPassword = true,
            isError = type == ErrorType.PasswordMismatch,
            errorText = if (type == ErrorType.PasswordMismatch) error?.errorMessage() else null,
        )
        Spacer(Modifier.height(10.dp))
        PasswordStrengthMeter(password, confirm)

        Spacer(Modifier.height(16.dp))
        PocketPrimaryButton(
            stringResource(R.string.register_button),
            { onSubmit(name, email, password, confirm, terms) },
            loading = state is AuthState.Loading,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                terms, { terms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = pocketColors.Teal,
                    uncheckedColor = if (type == ErrorType.TermsNotAccepted) pocketColors.ErrorRed else pocketColors.Teal,
                ),
            )
            Text(
                buildAnnotatedString {
                    append(termsPrefix)
                    withStyle(SpanStyle(color = pocketColors.Coral)) { append(termsLink) }
                    append(termsMiddle)
                    withStyle(SpanStyle(color = pocketColors.Coral)) { append(privacyLink) }
                },
                fontSize = 11.sp, color = pocketColors.Ink,
                modifier = Modifier.padding(top = 12.dp).clickable { terms = !terms },
            )
        }
        if (type == ErrorType.TermsNotAccepted) {
            Text(error!!.errorMessage(), fontSize = 11.sp, color = pocketColors.ErrorRed,
                modifier = Modifier.padding(start = 12.dp))
        }
    }
}
