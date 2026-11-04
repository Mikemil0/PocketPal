package com.pocketpal.app.ui.dai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.dai.DaiCard
import com.pocketpal.app.data.dai.DaiTopic
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.budgets.BudgetProgressBar
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors

/*
 * Flujo 2 · Lo que se abre desde el menú ⋮ del chat (Figma "03_Opciones"):
 * Acciones rápidas, Alertas, Metas, Educación (y Configuración, que llega con el flujo 4).
 */

/** Opciones del menú ⋮ del chat. */
enum class DaiMenuOption(val label: Int, val icon: ImageVector) {
    QuickActions(R.string.dai_menu_quick_actions, Icons.Outlined.Bolt),
    Alerts(R.string.dai_menu_alerts, Icons.Outlined.NotificationsNone),
    Goals(R.string.dai_menu_goals, Icons.Outlined.EmojiEvents),
    Education(R.string.dai_menu_education, Icons.AutoMirrored.Outlined.MenuBook),
    Settings(R.string.dai_menu_settings, Icons.Outlined.Settings),
}

/**
 * El ⋮ de la barra azul y su menú (fondo azul oscuro como en el Figma).
 * [options] permite ocultar "Configuración" hasta que exista (flujo 4).
 */
@Composable
fun DaiMenuButton(
    onSelect: (DaiMenuOption) -> Unit,
    options: List<DaiMenuOption> = DaiMenuOption.entries.filter { it != DaiMenuOption.Settings },
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, stringResource(R.string.a11y_dai_menu), tint = Color.White)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = pocketColors.Navy,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label), color = Color.White, fontSize = 13.sp) },
                    leadingIcon = { Icon(option.icon, null, tint = Color.White) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * Marco común: barra azul de Dai, la tarjeta clara con el contenido y abajo la barra
 * para escribir (lo que se escriba aquí se manda al chat con [onAsk]).
 */
@Composable
fun DaiPage(
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding().imePadding()) {
        DaiChatHeader(onBack = onBack, actions = headerActions)
        Column(Modifier.fillMaxSize().background(pocketColors.Cream).padding(12.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(pocketColors.Card)
                    .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(18.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                content = content,
            )
            Spacer(Modifier.height(10.dp))
            DaiInputBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    onAsk(draft)
                    draft = ""
                },
            )
        }
    }
}

/** Título grande de cada página ("¿Qué quieres hacer?", "Tus metas"…). */
@Composable
private fun PageTitle(text: String, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink, modifier = Modifier.weight(1f))
        trailing()
    }
    subtitle?.let {
        Spacer(Modifier.height(2.dp))
        Text(it, fontSize = 11.sp, color = pocketColors.Muted)
    }
    Spacer(Modifier.height(16.dp))
}

// ---------------------------------------------------------------- Acciones rápidas

/** Las 5 acciones de "¿Qué quieres hacer?" (Figma "03_Acciones_Rapidas"). */
enum class DaiQuickAction(val title: Int, val subtitle: Int, val icon: ImageVector) {
    Analyze(R.string.dai_quick_analyze, R.string.dai_quick_analyze_sub, Icons.Outlined.BarChart),
    ShouldKnow(R.string.dai_quick_should_know, R.string.dai_quick_should_know_sub, Icons.Outlined.Search),
    Goals(R.string.dai_quick_goals, R.string.dai_quick_goals_sub, Icons.Outlined.EmojiEvents),
    Advice(R.string.dai_quick_advice, R.string.dai_quick_advice_sub, Icons.Outlined.Lightbulb),
    Learn(R.string.dai_quick_learn, R.string.dai_quick_learn_sub, Icons.Outlined.School),
}

@Composable
fun DaiQuickActionsScreen(
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onAction: (DaiQuickAction) -> Unit,
) {
    DaiPage(onBack = onBack, onAsk = onAsk) {
        PageTitle(stringResource(R.string.dai_quick_title), stringResource(R.string.dai_quick_subtitle))
        DaiQuickAction.entries.forEach { action ->
            ActionRow(stringResource(action.title), stringResource(action.subtitle), action.icon) { onAction(action) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.Cream)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = pocketColors.Coral, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
            Text(subtitle, fontSize = 10.sp, color = pocketColors.Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = pocketColors.Muted)
    }
}

// ---------------------------------------------------------------- Alertas

/** Figma "04_Alertas_Recomendaciones": "Recomendaciones de Dai". */
@Composable
fun DaiAlertsScreen(
    state: DaiInsightsViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onCompleteData: () -> Unit,
    onRetry: () -> Unit,
) {
    DaiPage(onBack = onBack, onAsk = onAsk) {
        when (state) {
            DaiInsightsViewModel.UiState.Loading -> Loading()
            is DaiInsightsViewModel.UiState.Error -> InsightsError(onRetry)
            is DaiInsightsViewModel.UiState.Content -> {
                val data = state.data
                if (!data.hasData) {
                    MissingDataContent(data.missing, onCompleteData = onCompleteData, onLater = onBack)
                    return@DaiPage
                }
                val money = { amount: Double -> MoneyFormat.format(amount, currency) }
                PageTitle(stringResource(R.string.dai_alerts_title))

                // ---- Alerta de gasto ----
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(pocketColors.Card)
                        .border(1.dp, pocketColors.Coral, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DaiAvatar(if (data.alert != null) DaiMood.Curious else DaiMood.Happy, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(if (data.alert != null) R.string.dai_card_alert_title else R.string.dai_alerts_all_good),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = pocketColors.Ink,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(data.alertText, fontSize = 12.sp, color = pocketColors.Ink)
                    data.alert?.let { alert ->
                        Spacer(Modifier.height(12.dp))
                        OutlineAction(stringResource(R.string.dai_alerts_explain)) {
                            onAsk(DaiInsightsViewModel.explainPrompt(alert.category))
                        }
                        Spacer(Modifier.height(8.dp))
                        PocketPrimaryButton(
                            text = stringResource(R.string.dai_alerts_alternatives),
                            onClick = { onAsk(DaiInsightsViewModel.ALTERNATIVES_PROMPT) },
                        )
                    }
                }

                // ---- También te puede interesar ----
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.dai_alerts_more), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                Spacer(Modifier.height(10.dp))
                DaiCardView(data.suggestedLesson, currency, DaiCardActions(onOpenVideo = onOpenVideo))
                Spacer(Modifier.height(10.dp))
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = pocketColors.Coral, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dai_alerts_tip), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(data.tip, fontSize = 12.sp, color = pocketColors.Ink)
                }

                // ---- Impacto estimado ----
                data.alert?.projectedOver?.let { over ->
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.dai_card_impact), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                    Text(stringResource(R.string.dai_card_impact_body, money(over)), fontSize = 12.sp, color = pocketColors.Coral)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Metas

/** Figma "05_Metas_Seguimiento": "Tus metas". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaiGoalsScreen(
    state: DaiInsightsViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onAddGoal: () -> Unit,
    onSeeGoals: () -> Unit,
    onRetry: () -> Unit,
) {
    DaiPage(onBack = onBack, onAsk = onAsk) {
        PageTitle(stringResource(R.string.dai_goals_title)) {
            Text(
                stringResource(R.string.dai_goals_add),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAddGoal).padding(6.dp),
            )
        }
        when (state) {
            DaiInsightsViewModel.UiState.Loading -> Loading()
            is DaiInsightsViewModel.UiState.Error -> InsightsError(onRetry)
            is DaiInsightsViewModel.UiState.Content -> {
                val data = state.data
                val money = { amount: Double -> MoneyFormat.format(amount, currency) }
                if (data.goals.isEmpty()) {
                    Text(stringResource(R.string.dai_goals_empty), fontSize = 13.sp, color = pocketColors.Muted)
                }
                data.goals.forEach { goal ->
                    CardBox {
                        Row {
                            Text(goal.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink, modifier = Modifier.weight(1f))
                            Text("${goal.percent} %", fontSize = 12.sp, color = pocketColors.Muted)
                        }
                        Text("${money(goal.saved)} / ${money(goal.target)}", fontSize = 11.sp, color = pocketColors.Muted)
                        Spacer(Modifier.height(6.dp))
                        BudgetProgressBar(goal.percent / 100f, pocketColors.Teal)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (data.goals.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(pocketColors.Card)
                            .border(1.dp, pocketColors.Coral, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text(data.goalsMessage, fontSize = 12.sp, color = pocketColors.Ink)
                        Spacer(Modifier.height(10.dp))
                        OutlineAction(stringResource(R.string.dai_goals_follow), onClick = onSeeGoals)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.dai_goals_ask), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(DaiInsightsViewModel.SAVE_FASTER_PROMPT, DaiInsightsViewModel.RECOMMENDATION_PROMPT).forEach { prompt ->
                        Text(
                            prompt,
                            fontSize = 12.sp,
                            color = pocketColors.Coral,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, pocketColors.Coral, RoundedCornerShape(50))
                                .clickable { onAsk(prompt) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Educación

/** Figma "08_Educacion_Integrada": "Aprende con Dai". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaiEducationScreen(
    topic: DaiTopic,
    lesson: com.pocketpal.app.data.dai.DaiReply,
    currency: String,
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onSelectTopic: (DaiTopic) -> Unit,
    onOpenVideo: (String) -> Unit,
    onApplyToGoal: () -> Unit,
) {
    DaiPage(onBack = onBack, onAsk = onAsk) {
        PageTitle(stringResource(R.string.dai_learn_title))
        // La pregunta del usuario (burbuja azul) y la respuesta de Dai con su video.
        val question = stringResource(R.string.dai_learn_question, stringResource(topicName(topic)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                question,
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                    .background(pocketColors.Navy)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            DaiAvatar(DaiMood.Curious, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                CardBox { Text(lesson.text, fontSize = 12.sp, color = pocketColors.Ink) }
                (lesson.card as? DaiCard.Lesson)?.let {
                    Spacer(Modifier.height(8.dp))
                    DaiCardView(it, currency, DaiCardActions(onOpenVideo = onOpenVideo))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PocketPrimaryButton(
                text = stringResource(R.string.dai_learn_apply),
                onClick = onApplyToGoal,
                modifier = Modifier.weight(1f),
            )
            PocketPrimaryButton(
                text = stringResource(R.string.dai_learn_later),
                onClick = onBack,
                modifier = Modifier.weight(1f),
                containerColor = Color.White,
                contentColor = pocketColors.Coral,
                border = androidx.compose.foundation.BorderStroke(1.dp, pocketColors.Coral),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.dai_learn_more), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DaiTopic.entries.forEach { option ->
                val selected = option == topic
                Text(
                    stringResource(topicName(option)),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else pocketColors.Coral,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) pocketColors.Coral else Color.White)
                        .border(1.dp, pocketColors.Coral, RoundedCornerShape(50))
                        .clickable { onSelectTopic(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Nombre de cada tema en el idioma de la app. */
fun topicName(topic: DaiTopic): Int = when (topic) {
    DaiTopic.CompoundInterest -> R.string.dai_topic_compound
    DaiTopic.Budget -> R.string.dai_topic_budget
    DaiTopic.Saving -> R.string.dai_topic_saving
    DaiTopic.EmergencyFund -> R.string.dai_topic_emergency
    DaiTopic.Inflation -> R.string.dai_topic_inflation
    DaiTopic.Credit -> R.string.dai_topic_credit
    DaiTopic.Rule503020 -> R.string.dai_topic_503020
    DaiTopic.AntExpenses -> R.string.dai_topic_ant
}

// ---------------------------------------------------------------- Faltan datos

/** Figma "09_Falta_Datos" como pantalla completa (se abre desde "Completar datos" del chat). */
@Composable
fun DaiMissingDataScreen(
    missing: DaiCard.MissingData?,
    onBack: () -> Unit,
    onAsk: (String) -> Unit,
    onCompleteData: () -> Unit,
) {
    DaiPage(onBack = onBack, onAsk = onAsk) {
        MissingDataContent(
            missing ?: DaiCard.MissingData(needsIncome = true, needsExpenses = true, needsGoals = true),
            onCompleteData = onCompleteData,
            onLater = onBack,
        )
    }
}

@Composable
private fun MissingDataContent(missing: DaiCard.MissingData, onCompleteData: () -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        DaiAvatar(DaiMood.Confused, size = 96.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.dai_missing_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.dai_missing_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Column {
            CheckRow(stringResource(R.string.dai_missing_income), !missing.needsIncome)
            CheckRow(stringResource(R.string.dai_missing_expenses), !missing.needsExpenses)
            CheckRow(stringResource(R.string.dai_missing_goals), !missing.needsGoals)
        }
        Spacer(Modifier.height(20.dp))
        PocketPrimaryButton(text = stringResource(R.string.dai_complete_data), onClick = onCompleteData)
        Spacer(Modifier.height(10.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.dai_later),
            onClick = onLater,
            containerColor = Color.White,
            contentColor = pocketColors.Coral,
            border = androidx.compose.foundation.BorderStroke(1.dp, pocketColors.Coral),
        )
    }
}

/** Chulo verde si ya lo tienes; gris si falta. */
@Composable
private fun CheckRow(text: String, done: Boolean) {
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

// ---------------------------------------------------------------- Apoyo

@Composable
private fun OutlineAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    )
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = pocketColors.Coral)
    }
}

@Composable
private fun InsightsError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DaiAvatar(DaiMood.Sad, size = 80.dp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.dai_error_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.dai_error_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        PocketPrimaryButton(text = stringResource(R.string.dai_retry), onClick = onRetry)
    }
}