package com.pocketpal.app.ui.dai

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.data.dai.DaiCard
import com.pocketpal.app.data.dai.DaiMessage
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.budgets.BudgetProgressBar
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*
 * Piezas visuales de Dai (base del Módulo 6): las caras de Dai, la barra superior del
 * chat, las burbujas, las respuestas rápidas, la barra para escribir y las tarjetas.
 */

/** Las caras de Dai (imágenes del Figma en res/drawable-nodpi). */
enum class DaiMood(@DrawableRes val image: Int) {
    Happy(R.drawable.dai_feliz),
    Calm(R.drawable.dai_tranquilo),
    Sad(R.drawable.dai_triste),
    Confused(R.drawable.dai_confundido),
    Thinking(R.drawable.dai_pensando),
    Celebrating(R.drawable.dai_celebrando),
    Motivated(R.drawable.dai_motivado),
    Curious(R.drawable.dai_curioso),
    Grateful(R.drawable.dai_agradecido),
    Online(R.drawable.dai_en_linea),
}

/** Dai en un círculo blanco con borde rosado. */
@Composable
fun DaiAvatar(mood: DaiMood, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(if (size >= 64.dp) 2.dp else 1.dp, pocketColors.RoseBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(mood.image),
            contentDescription = stringResource(R.string.a11y_dai),
            modifier = Modifier.size(size * 0.82f),
        )
    }
}

/**
 * Barra azul del chat (Figma "02_Chat_Principal"): atrás, Dai en línea, "Daikoku" y
 * "Dai · Asistente financiero", y acciones a la derecha (el menú ⋮).
 */
@Composable
fun DaiChatHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = stringResource(R.string.dai_subtitle),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(pocketColors.Navy)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_back), tint = Color.White)
        }
        DaiAvatar(DaiMood.Online, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.dai_name), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
        }
        actions()
    }
}

/** Hora de la burbuja ("9:41"). */
private val TIME = DateTimeFormatter.ofPattern("H:mm")

/**
 * Una burbuja del chat. Usuario: azul oscuro a la derecha. Dai: rosada a la izquierda
 * con su cara. Debajo, la tarjeta (si la hay).
 */
@Composable
fun DaiBubble(
    message: DaiMessage,
    currency: String,
    modifier: Modifier = Modifier,
    actions: DaiCardActions = DaiCardActions(),
) {
    if (message.isUser) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(
                Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .background(pocketColors.Navy)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(message.text, fontSize = 13.sp, color = Color.White)
                BubbleTime(message.createdAt, Color.White.copy(alpha = 0.6f))
            }
        }
    } else {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            DaiAvatar(moodFor(message.card), size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                        .background(pocketColors.Card)
                        .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(message.text, fontSize = 13.sp, color = pocketColors.Ink)
                    BubbleTime(message.createdAt, pocketColors.Muted)
                }
                message.card?.let { card ->
                    Spacer(Modifier.height(8.dp))
                    DaiCardView(card, currency, actions)
                }
            }
        }
    }
}

@Composable
private fun BubbleTime(time: LocalDateTime?, color: Color) {
    if (time == null) return
    Text(
        TIME.format(time),
        fontSize = 9.sp,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        textAlign = TextAlign.End,
    )
}

/** La cara de Dai según lo que responde. */
fun moodFor(card: DaiCard?): DaiMood = when (card) {
    is DaiCard.MissingData -> DaiMood.Confused
    is DaiCard.Alert -> DaiMood.Curious
    is DaiCard.ReceiptSaved -> DaiMood.Celebrating
    is DaiCard.Goals -> DaiMood.Motivated
    else -> DaiMood.Happy
}

/** "Dai está escribiendo…": la burbuja con puntos mientras arma la respuesta. */
@Composable
fun DaiTypingBubble(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DaiAvatar(DaiMood.Thinking, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("• • •", fontSize = 14.sp, color = pocketColors.Muted)
        }
    }
}

/** Respuestas rápidas ("Ver resumen", "Consejos"): píldoras con borde coral. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaiChips(chips: List<String>, onChip: (String) -> Unit, modifier: Modifier = Modifier) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier.fillMaxWidth().padding(start = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            Text(
                chip,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pocketColors.Card)
                    .border(1.dp, pocketColors.Coral, RoundedCornerShape(50))
                    .clickable { onChip(chip) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * Barra de abajo: "Escribe un mensaje…", micrófono, cámara y el botón coral de enviar.
 * [onMic] y [onCamera] en `null` = esos íconos no se muestran.
 */
@Composable
fun DaiInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onMic: (() -> Unit)? = null,
    onCamera: (() -> Unit)? = null,
) {
    val canSend = enabled && value.isNotBlank()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(28.dp))
            .padding(start = 4.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = { onValueChange(it.take(DaiBrain.MAX_MESSAGE)) },
            placeholder = { Text(stringResource(R.string.dai_input_hint), fontSize = 13.sp, color = pocketColors.Muted) },
            modifier = Modifier.weight(1f),
            maxLines = 3,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = pocketColors.Ink,
                unfocusedTextColor = pocketColors.Ink,
            ),
        )
        if (onMic != null) {
            IconButton(onClick = onMic, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Mic, stringResource(R.string.a11y_dai_voice), tint = pocketColors.Muted)
            }
        }
        if (onCamera != null) {
            IconButton(onClick = onCamera, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.CameraAlt, stringResource(R.string.a11y_dai_camera), tint = pocketColors.Muted)
            }
        }
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (canSend) pocketColors.Coral else pocketColors.Coral.copy(alpha = 0.4f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.a11y_dai_send), tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Lo que pueden hacer los botones dentro de las tarjetas. */
data class DaiCardActions(
    /** "Ver video": abre la búsqueda del video recomendado. */
    val onOpenVideo: (String) -> Unit = {},
    /** "Completar datos" de "Todavía no tengo suficientes datos". */
    val onCompleteData: (() -> Unit)? = null,
)

/** Dibuja la tarjeta que acompaña la respuesta de Dai. */
@Composable
fun DaiCardView(card: DaiCard, currency: String, actions: DaiCardActions = DaiCardActions()) {
    val money = { amount: Double -> MoneyFormat.format(amount, currency) }
    CardBox {
        when (card) {
            is DaiCard.Spending -> {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dai_card_spent), fontSize = 11.sp, color = pocketColors.Muted)
                        Text(money(card.spent), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                    }
                    if (card.budget > 0.0) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.dai_card_budget), fontSize = 11.sp, color = pocketColors.Muted)
                            Text(money(card.budget), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                        }
                    }
                }
                if (card.budget > 0.0) {
                    Spacer(Modifier.height(8.dp))
                    BudgetProgressBar(card.percent / 100f, if (card.percent > 100) pocketColors.ErrorRed else pocketColors.Coral)
                    Text(
                        stringResource(R.string.dai_card_used, card.percent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pocketColors.Coral,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (card.categories.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.dai_card_top_categories), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                    card.categories.forEach { share ->
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text(share.name, fontSize = 12.sp, color = pocketColors.Ink, modifier = Modifier.weight(1f))
                            Text("${share.percent} %", fontSize = 12.sp, color = pocketColors.Muted)
                        }
                        Spacer(Modifier.height(4.dp))
                        BudgetProgressBar(share.percent / 100f, CategoryColors.parse(share.color))
                        Text(money(share.amount), fontSize = 10.sp, color = pocketColors.Muted, modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End)
                    }
                }
            }
            is DaiCard.Goals -> card.goals.forEach { goal ->
                Row {
                    Text(goal.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink, modifier = Modifier.weight(1f))
                    Text("${goal.percent} %", fontSize = 12.sp, color = pocketColors.Muted)
                }
                Text("${money(goal.saved)} / ${money(goal.target)}", fontSize = 11.sp, color = pocketColors.Muted)
                Spacer(Modifier.height(4.dp))
                BudgetProgressBar(goal.percent / 100f, pocketColors.Teal)
                Spacer(Modifier.height(10.dp))
            }
            is DaiCard.Alert -> {
                Text(stringResource(R.string.dai_card_alert_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dai_card_alert_body, card.days, money(card.amount), card.category),
                    fontSize = 12.sp,
                    color = pocketColors.Ink,
                )
                card.projectedOver?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.dai_card_impact), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                    Text(stringResource(R.string.dai_card_impact_body, money(it)), fontSize = 11.sp, color = pocketColors.Coral)
                }
            }
            is DaiCard.Lesson -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(pocketColors.Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.dai_card_video), fontSize = 11.sp, color = pocketColors.Muted)
                    Text(card.videoTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.dai_card_video_meta, card.minutes), fontSize = 10.sp, color = pocketColors.Muted)
                }
                Text(
                    stringResource(R.string.dai_card_watch),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pocketColors.Coral)
                        .clickable { actions.onOpenVideo(card.videoQuery) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            is DaiCard.MissingData -> {
                MissingRow(stringResource(R.string.dai_missing_income), !card.needsIncome)
                MissingRow(stringResource(R.string.dai_missing_expenses), !card.needsExpenses)
                MissingRow(stringResource(R.string.dai_missing_goals), !card.needsGoals)
                actions.onCompleteData?.let { onComplete ->
                    Spacer(Modifier.height(10.dp))
                    PocketPrimaryButton(text = stringResource(R.string.dai_complete_data), onClick = onComplete)
                }
            }
            is DaiCard.ReceiptSaved -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = pocketColors.Teal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dai_card_receipt_saved), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                }
                Spacer(Modifier.height(6.dp))
                Text("${card.merchant} · ${money(card.amount)}", fontSize = 12.sp, color = pocketColors.Ink)
                Text(card.category ?: stringResource(R.string.dai_no_category), fontSize = 11.sp, color = pocketColors.Muted)
                card.budgetPercent?.let {
                    Text(stringResource(R.string.dai_card_used, it), fontSize = 11.sp, color = pocketColors.Coral)
                }
            }
        }
    }
}

/** Una línea con chulo de "Todavía no tengo suficientes datos" (verde = ya lo tienes). */
@Composable
private fun MissingRow(text: String, done: Boolean) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            null,
            tint = if (done) pocketColors.Teal else pocketColors.Muted.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
    }
}

/** Marco blanco con borde rosado de las tarjetas. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pocketColors.Card)
            .border(BorderStroke(1.dp, pocketColors.RoseBorder), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) { content() }
}