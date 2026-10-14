package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * "Idioma" (pág. 69, §8.10b). La maqueta pone "Automático" como subtítulo ambiguo
 * de "Español" (hallazgo 7): por decisión A de la Fase 5 es una **opción aparte**
 * arriba ("Automático" = idioma del sistema), igual que la hoja del Welcome. El
 * cambio se aplica al tocar "Guardar" (RF-A3).
 */
@Composable
fun LanguageScreen(
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
            stringResource(R.string.lang_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.lang_description),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(pocketColors.ChipPink)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectRow(
                title = stringResource(R.string.lang_auto),
                subtitle = stringResource(R.string.lang_auto_sub),
                selected = selected.isBlank(),
                onClick = { selected = "" },
            )
            SelectRow(
                title = stringResource(R.string.lang_name_es),
                subtitle = nativeLanguageName("es"),
                selected = selected == "es",
                onClick = { selected = "es" },
            )
            SelectRow(
                title = stringResource(R.string.lang_name_en),
                subtitle = nativeLanguageName("en"),
                selected = selected == "en",
                onClick = { selected = "en" },
            )
            SelectRow(
                title = stringResource(R.string.lang_name_pt),
                subtitle = nativeLanguageName("pt"),
                selected = selected == "pt",
                onClick = { selected = "pt" },
            )
            SelectRow(
                title = stringResource(R.string.lang_name_fr),
                subtitle = nativeLanguageName("fr"),
                selected = selected == "fr",
                onClick = { selected = "fr" },
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
