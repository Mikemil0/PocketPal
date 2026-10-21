package com.pocketpal.app.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/**
 * Flujo 2 · "Meta creada" (Figma "META CREADA"): confirma la meta recién guardada con
 * lo que falta, la fecha límite y cuánto ahorrar por mes para llegar a tiempo.
 * El botón atrás del sistema lleva a la lista (no vuelve al formulario ya guardado).
 */
@Composable
fun GoalCreatedScreen(
    goal: Goal,
    currency: String,
    onSeeGoals: () -> Unit,
    onCreateAnother: () -> Unit,
) {
    BackHandler(onBack = onSeeGoals)
    val locale = currentLocale()
    val today = LocalDate.now()
    val money = { amount: Double -> MoneyFormat.format(amount, currency) }
    val suggestion = GoalRules.suggestedMonthly(goal.savedAmount, goal.targetAmount, today, goal.deadlineDate)

    AuthScaffold {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(pocketColors.Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.goal_created_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.goal_created_body, goal.name, money(goal.savedAmount)),
            fontSize = 14.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryRow(
                stringResource(R.string.goal_created_remaining),
                money(GoalRules.remaining(goal.savedAmount, goal.targetAmount)),
            )
            HorizontalDivider(color = pocketColors.ChipPink)
            SummaryRow(
                stringResource(R.string.goal_created_deadline),
                goal.deadlineDate?.let { GoalFormat.prettyDate(it, locale) }
                    ?: stringResource(R.string.goal_no_deadline),
            )
            if (suggestion != null) {
                HorizontalDivider(color = pocketColors.ChipPink)
                SummaryRow(
                    stringResource(R.string.goal_created_suggested),
                    stringResource(R.string.goal_created_per_month, money(suggestion)),
                    highlight = true,
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        PocketPrimaryButton(text = stringResource(R.string.goal_created_see_goals), onClick = onSeeGoals)
        Spacer(Modifier.height(12.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.goal_created_another),
            onClick = onCreateAnother,
            containerColor = pocketColors.Card,
            contentColor = pocketColors.Coral,
            border = BorderStroke(1.dp, pocketColors.Coral),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = pocketColors.Muted, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) pocketColors.Coral else pocketColors.Ink,
        )
    }
}
