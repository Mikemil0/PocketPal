package com.pocketpal.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.SelectRow
import com.pocketpal.app.ui.theme.pocketColors

/**
 * "Formato de fecha" (pág. 58, §8.11): tres opciones con ejemplo y botón Guardar.
 * Las etiquetas se localizan (es "AAAA" / en "YYYY", §8.11) y la errata del Figma
 * "09/07/AAAA" se corrige a "09/07/2026" (§11.1, hallazgo 11).
 */
@Composable
fun DateFormatScreen(
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
        // El Figma (pág. 58) alinea el título a la izquierda.
        Text(
            stringResource(R.string.date_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.date_description),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectRow(
                title = stringResource(R.string.date_option_dmy),
                subtitle = stringResource(R.string.date_option_dmy_example),
                selected = selected == "DD/MM/YYYY",
                onClick = { selected = "DD/MM/YYYY" },
            )
            SelectRow(
                title = stringResource(R.string.date_option_mdy),
                subtitle = stringResource(R.string.date_option_mdy_example),
                selected = selected == "MM/DD/YYYY",
                onClick = { selected = "MM/DD/YYYY" },
            )
            SelectRow(
                title = stringResource(R.string.date_option_ymd),
                subtitle = stringResource(R.string.date_option_ymd_example),
                selected = selected == "YYYY-MM-DD",
                onClick = { selected = "YYYY-MM-DD" },
            )
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.common_save),
            onClick = { onSave(selected) },
        )
        Spacer(Modifier.height(24.dp))
    }
}
