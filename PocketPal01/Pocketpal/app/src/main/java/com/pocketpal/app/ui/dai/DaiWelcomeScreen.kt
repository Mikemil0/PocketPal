package com.pocketpal.app.ui.dai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors

/** Las 4 opciones de "¿Qué te gustaría hacer hoy?" (Figma "DAI BIENVENIDA"). */
enum class DaiWelcomeOption(val label: Int, val prompt: String) {
    Spending(R.string.dai_welcome_spending, "¿Cómo voy con mis gastos este mes?"),
    Goals(R.string.dai_welcome_goals, "¿Cómo voy con mis metas?"),
    Advice(R.string.dai_welcome_advice, "Dame un consejo"),
    Voice(R.string.dai_welcome_voice, ""),
}

/**
 * Flujo 1 · "Daikoku · Tu asistente financiero" (Figma "DAI BIENVENIDA").
 *
 * Franja azul con el nombre y Dai, "¡Hola, Usuario!", la tarjeta "¿Qué te gustaría hacer
 * hoy?" con 4 opciones y el botón "Hablar con Dai". Las 3 primeras opciones abren el
 * chat con esa pregunta ya enviada. "Hablar por voz" abre el modo voz si [onVoice] no es
 * `null` (flujo 4); si no, abre el chat.
 */
@Composable
fun DaiWelcomeScreen(
    userName: String,
    onBack: () -> Unit,
    onOption: (String) -> Unit,
    onTalk: () -> Unit,
    onVoice: (() -> Unit)? = null,
) {
    val first = userName.trim().substringBefore(" ")
    Column(
        Modifier
            .fillMaxSize()
            .background(pocketColors.Cream)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- Franja azul ----
        Box(Modifier.fillMaxWidth().height(190.dp).background(pocketColors.Navy)) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_back), tint = Color.White)
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.dai_name), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(stringResource(R.string.dai_tagline), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        // Dai "montado" sobre el borde de la franja, como en el Figma.
        Box(Modifier.fillMaxWidth().offset(y = (-55).dp), contentAlignment = Alignment.Center) {
            DaiAvatar(DaiMood.Happy, size = 110.dp)
        }
        Column(
            Modifier.fillMaxWidth().offset(y = (-40).dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (first.isBlank()) stringResource(R.string.dai_welcome_hello) else stringResource(R.string.dai_welcome_hello_name, first),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(R.string.dai_welcome_intro), fontSize = 13.sp, color = pocketColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.dai_welcome_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            // ---- ¿Qué te gustaría hacer hoy? ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(pocketColors.Card)
                    .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.dai_welcome_question), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                DaiWelcomeOption.entries.forEach { option ->
                    val color = when (option) {
                        DaiWelcomeOption.Spending -> pocketColors.Coral
                        DaiWelcomeOption.Goals -> pocketColors.Teal
                        DaiWelcomeOption.Advice -> pocketColors.Muted
                        DaiWelcomeOption.Voice -> pocketColors.Ink
                    }
                    Text(
                        stringResource(option.label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(50))
                            .clickable {
                                when {
                                    option != DaiWelcomeOption.Voice -> onOption(option.prompt)
                                    onVoice != null -> onVoice()
                                    else -> onTalk()
                                }
                            }
                            .padding(vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            PocketPrimaryButton(text = stringResource(R.string.dai_welcome_talk), onClick = onTalk)
        }
    }
}
