package com.pocketpal.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.SelectRow
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Moneda" (`profile/currency`, **sin diseño**: D-04). Lista simple de las cuatro
 * monedas soportadas con selección única y Guardar (mismo patrón que sus pantallas
 * hermanas). El valor es único en la app (RN-09: mismo en Datos personales).
 */
@Composable
fun CurrencyScreen(
    current: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.currency_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "COP" to R.string.currency_cop,
                "USD" to R.string.currency_usd,
                "EUR" to R.string.currency_eur,
                "MXN" to R.string.currency_mxn,
            ).forEach { (code, label) ->
                SelectRow(
                    title = stringResource(label),
                    selected = selected == code,
                    onClick = { selected = code },
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.common_save),
            onClick = { onSave(selected) },
        )
        Spacer(Modifier.height(24.dp))
    }
}
