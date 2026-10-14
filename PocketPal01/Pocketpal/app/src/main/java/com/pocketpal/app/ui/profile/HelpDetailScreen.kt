package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Detalle de una pregunta frecuente (pág. 57): pregunta, respuesta y los botones
 * de texto "Anterior" / "Siguiente" para recorrer el catálogo (RF-B10). Los extremos
 * quedan deshabilitados.
 */
@Composable
fun HelpDetailScreen(
    index: Int,
    supportEmail: String,
    onBack: () -> Unit,
    onPrev: (Int) -> Unit,
    onNext: (Int) -> Unit,
) {
    val safeIndex = index.coerceIn(0, FaqCatalog.items.lastIndex)
    val item = FaqCatalog.items[safeIndex]
    val question = stringResource(item.question)
    val answer = stringResource(item.answer, supportEmail)

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.help_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionHeader(stringResource(R.string.help_section_faqs))

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 480.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    question,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = pocketColors.Ink,
                    lineHeight = 23.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    answer,
                    fontSize = 14.sp,
                    color = pocketColors.Muted,
                    lineHeight = 20.sp,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NavText(
                    text = stringResource(R.string.help_prev),
                    enabled = safeIndex > 0,
                    onClick = { onPrev(safeIndex - 1) },
                )
                NavText(
                    text = stringResource(R.string.help_next),
                    enabled = safeIndex < FaqCatalog.items.lastIndex,
                    onClick = { onNext(safeIndex + 1) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** "Anterior" / "Siguiente" (pág. 57): azul marino al habilitar, gris al estar en un extremo. */
@Composable
private fun NavText(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) pocketColors.Ink else pocketColors.Gray,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
    )
}
