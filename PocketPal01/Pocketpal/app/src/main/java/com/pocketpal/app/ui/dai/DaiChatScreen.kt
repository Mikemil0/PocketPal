package com.pocketpal.app.ui.dai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.dai.DaiBrain
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 1 · El chat con Dai (Figma "02_Chat_Principal 1 y 2"; si no hay mensajes,
 * "10_Chat_Nuevo").
 *
 * - [LazyColumn] con las burbujas: solo dibuja las que se ven, aunque el historial sea largo.
 * - Al llegar un mensaje nuevo baja solo hasta el final (`animateScrollToItem`).
 * - [headerActions] es el menú ⋮ (flujo 2); [onMic] y [onCamera] los íconos de la barra
 *   (flujos 4 y 3). En `null` no se muestran.
 */
@Composable
fun DaiChatScreen(
    state: DaiChatViewModel.ChatUi,
    currency: String,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    cardActions: DaiCardActions = DaiCardActions(),
    headerActions: @Composable RowScope.() -> Unit = {},
    onMic: (() -> Unit)? = null,
    onCamera: (() -> Unit)? = null,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Cada vez que llega un mensaje (o aparece "escribiendo…") se baja al final.
    val itemCount = state.messages.size + (if (state.typing) 1 else 0) + (if (state.chips.isNotEmpty()) 1 else 0)
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }
    fun submit(text: String) {
        onSend(text)
        draft = ""
    }

    Column(Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding().imePadding()) {
        DaiChatHeader(onBack = onBack, actions = headerActions)
        Column(
            Modifier
                .fillMaxSize()
                .background(pocketColors.Cream)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(pocketColors.Card)
                    .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(18.dp)),
            ) {
                when {
                    !state.loaded -> CircularProgressIndicator(color = pocketColors.Coral, modifier = Modifier.align(Alignment.Center))
                    state.isNew && !state.typing -> NewChat(onPrompt = ::submit)
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            DaiBubble(message, currency, actions = cardActions)
                        }
                        if (state.typing) item { DaiTypingBubble() }
                        if (state.chips.isNotEmpty()) item { DaiChips(state.chips, onChip = ::submit) }
                    }
                }
            }
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(
                    stringResource(R.string.dai_error_title),
                    actionText = stringResource(R.string.dai_retry),
                    onAction = onRetry,
                )
            }
            Spacer(Modifier.height(10.dp))
            DaiInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = { submit(draft) },
                enabled = !state.typing,
                onMic = onMic,
                onCamera = onCamera,
            )
        }
    }
}

/** Figma "10_Chat_Nuevo": Dai, "¿En qué puedo ayudarte hoy?" y 4 sugerencias. */
@Composable
private fun NewChat(onPrompt: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        DaiAvatar(DaiMood.Motivated, size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.dai_new_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.dai_new_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        DaiBrain.starterPrompts.zip(NEW_CHAT_HINTS).forEach { (prompt, hint) ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pocketColors.Cream)
                    .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(12.dp))
                    .clickable { onPrompt(prompt) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(prompt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                Text(stringResource(hint), fontSize = 10.sp, color = pocketColors.Muted)
            }
        }
    }
}

/** Subtítulos grises de las 4 sugerencias del chat nuevo. */
private val NEW_CHAT_HINTS = listOf(
    R.string.dai_new_hint_categories,
    R.string.dai_new_hint_progress,
    R.string.dai_new_hint_advice,
    R.string.dai_new_hint_learn,
)
