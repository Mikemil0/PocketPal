package com.pocketpal.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalContribution
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

private const val DETAIL_ERROR_MS = 4_000L

/**
 * Flujo 3 · Detalle de la meta (Figma "DETALLE META"): cuánto lleva y cuánto falta, la
 * fecha límite con los días restantes, la sugerencia mensual, el botón "Agregar aporte"
 * y el "HISTORIAL DE APORTES". Si la meta acaba de llegar al 100 % se muestra la
 * celebración "¡Lo lograste! 🎉".
 *
 * [onEdit] (lápiz arriba a la derecha) lo conecta el flujo 4; si es `null` no se muestra.
 */
@Composable
fun GoalDetailScreen(
    state: GoalDetailViewModel.UiState,
    error: ErrorType?,
    celebration: Goal?,
    currency: String,
    onBack: () -> Unit,
    onAddContribution: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onDismissCelebration: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(DETAIL_ERROR_MS)
            shownError = null
        }
    }
    val money = { amount: Double -> MoneyFormat.format(amount, currency) }

    AuthScaffold {
        GoalTopBar(onBack = onBack, onEdit = onEdit?.takeIf { state is GoalDetailViewModel.UiState.Content })
        Spacer(Modifier.height(16.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        when (state) {
            GoalDetailViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = 80.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = pocketColors.Coral) }

            GoalDetailViewModel.UiState.NotFound -> Column(
                Modifier.fillMaxWidth().padding(vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.goal_detail_not_found),
                    fontSize = 15.sp,
                    color = pocketColors.Muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PocketPrimaryButton(text = stringResource(R.string.goal_detail_back_to_list), onClick = onBack)
            }

            is GoalDetailViewModel.UiState.Error -> Column(
                Modifier.fillMaxWidth().padding(vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorBanner(stringResource(state.type.messageRes()))
                Spacer(Modifier.height(16.dp))
                PocketPrimaryButton(text = stringResource(R.string.goal_detail_retry), onClick = onRetry)
            }

            is GoalDetailViewModel.UiState.Content -> DetailContent(
                goal = state.data.goal,
                contributions = state.data.contributions,
                money = money,
                onAddContribution = onAddContribution,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (celebration != null) {
        CelebrationDialog(goal = celebration, money = money, onDismiss = onDismissCelebration)
    }
}

@Composable
private fun DetailContent(
    goal: Goal,
    contributions: List<GoalContribution>,
    money: (Double) -> String,
    onAddContribution: () -> Unit,
) {
    val locale = currentLocale()
    val today = LocalDate.now()
    val deadline = goal.deadlineDate
    val goalColor = CategoryColors.parse(goal.color)
    val percent = GoalRules.percent(goal.savedAmount, goal.targetAmount)
    val suggestion = GoalRules.suggestedMonthly(goal.savedAmount, goal.targetAmount, today, deadline)

    // Encabezado: ícono, nombre y fecha límite.
    Row(verticalAlignment = Alignment.CenterVertically) {
        GoalIconBadge(goal.icon, goal.color, size = 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                goal.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = pocketColors.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    deadlineText(goal, deadline, today, locale),
                    fontSize = 12.sp,
                    color = pocketColors.Muted,
                )
            }
        }
    }
    Spacer(Modifier.height(20.dp))

    // Tarjeta de progreso.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        if (goal.isCompleted) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pocketColors.TealSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = pocketColors.Teal, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.goal_completed_badge),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Teal,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(money(goal.savedAmount), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
        Text(
            stringResource(R.string.goal_saved_of, money(goal.targetAmount)),
            fontSize = 13.sp,
            color = pocketColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        GoalProgressBar(
            progress = GoalRules.progress(goal.savedAmount, goal.targetAmount),
            color = goalColor,
            height = 10.dp,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.goal_percent_saved, percent),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                modifier = Modifier.weight(1f),
            )
            if (!goal.isCompleted) {
                Text(
                    stringResource(R.string.goal_remaining, money(GoalRules.remaining(goal.savedAmount, goal.targetAmount))),
                    fontSize = 12.sp,
                    color = pocketColors.Muted,
                )
            }
        }
    }

    if (suggestion != null) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(pocketColors.ChipPink)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Lightbulb, null, tint = pocketColors.Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.goal_suggestion, money(suggestion)),
                fontSize = 13.sp,
                color = pocketColors.Ink,
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    PocketPrimaryButton(
        text = stringResource(R.string.goal_detail_add),
        onClick = onAddContribution,
        icon = Icons.Outlined.Add,
    )

    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.goal_detail_history),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Muted,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(8.dp))
    if (contributions.isEmpty()) {
        Text(
            stringResource(R.string.goal_detail_history_empty),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contributions.forEach { ContributionRow(it, money, locale) }
        }
    }
}

@Composable
private fun deadlineText(goal: Goal, deadline: LocalDate?, today: LocalDate, locale: Locale): String {
    if (deadline == null) return stringResource(R.string.goal_no_deadline)
    val date = stringResource(R.string.goal_deadline_label, GoalFormat.prettyDate(deadline, locale))
    if (goal.isCompleted) return date
    val days = GoalRules.daysLeft(today, deadline)
    return if (days < 0) {
        "$date · ${stringResource(R.string.goal_deadline_passed)}"
    } else {
        "$date · ${pluralStringResource(R.plurals.goal_days_left, days.toInt(), days.toInt())}"
    }
}

@Composable
private fun ContributionRow(item: GoalContribution, money: (Double) -> String, locale: Locale) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.ChipPink, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(pocketColors.ChipPink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, null, tint = pocketColors.Coral, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    item.isInitial -> stringResource(R.string.goal_detail_initial)
                    item.note != null -> item.note
                    else -> stringResource(R.string.goal_detail_contribution)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = pocketColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.date?.let {
                Text(GoalFormat.prettyDate(it, locale), fontSize = 12.sp, color = pocketColors.Muted)
            }
        }
        Text(
            stringResource(R.string.goal_detail_plus, money(item.amount)),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Teal,
        )
    }
}

/** "¡Lo lograste! 🎉" cuando un aporte completa la meta. */
@Composable
private fun CelebrationDialog(goal: Goal, money: (Double) -> String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(pocketColors.Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.goal_celebration_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.goal_celebration_body, goal.name, money(goal.targetAmount)),
                fontSize = 14.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PocketPrimaryButton(text = stringResource(R.string.goal_celebration_ok), onClick = onDismiss)
        }
    }
}
