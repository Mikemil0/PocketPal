package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Número de teléfono" paso 1 (págs. 17 y 8, §8.4 / RF-B3): campo compuesto con el
 * prefijo del país (D-04 fila 5: +57, +51, +52 y +591) y el número, texto de ayuda,
 * error "Número inválido, inténtalo de nuevo" y botón Siguiente.
 */
@Composable
fun PhoneScreen(
    prefix: String,
    number: String,
    sending: Boolean,
    formError: ErrorType?,
    error: ErrorType?,
    onBack: () -> Unit,
    onPrefixChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onNext: () -> Unit,
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

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.phone_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.phone_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = pocketColors.Ink,
        )
        Spacer(Modifier.height(6.dp))
        PhoneField(
            prefix = prefix,
            number = number,
            onPrefixChange = onPrefixChange,
            onNumberChange = { onNumberChange(it); if (shownFormError != null) shownFormError = null },
        )

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.phone_helper),
            fontSize = 12.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (shownFormError == ErrorType.InvalidPhone) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.error_invalid_phone),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (shownError != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(shownError!!.messageRes()))
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.phone_next),
            onClick = onNext,
            enabled = !sending,
            loading = sending,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Campo compuesto (pág. 17): chip rosa con el prefijo (abre la lista de la pág. 9) + número. */
@Composable
private fun PhoneField(
    prefix: String,
    number: String,
    onPrefixChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Text(
                prefix,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Coral,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(pocketColors.ChipPink)
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            // Lista de prefijos (pág. 9): panel flotante rosa.
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = pocketColors.ChipPink,
                shape = RoundedCornerShape(16.dp),
            ) {
                PREFIXES.forEach { (code, _) ->
                    DropdownMenuItem(
                        text = {
                            Text(code, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pocketColors.Coral)
                        },
                        onClick = {
                            onPrefixChange(code)
                            expanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = number,
            onValueChange = onNumberChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                color = pocketColors.Ink,
            ),
            cursorBrush = SolidColor(pocketColors.Coral),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            decorationBox = { inner ->
                if (number.isEmpty()) {
                    Text(stringResource(R.string.phone_placeholder), fontSize = 15.sp, color = pocketColors.Coral)
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** D-04 / §14 fila 5: prefijos corregidos (+591 para Bolivia, no +54 del Figma). */
val PREFIXES = listOf(
    "+57" to "CO",
    "+51" to "PE",
    "+52" to "MX",
    "+591" to "BO",
)
