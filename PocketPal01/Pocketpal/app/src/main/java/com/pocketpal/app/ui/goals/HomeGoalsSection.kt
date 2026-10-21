package com.pocketpal.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 1 · Sección "Metas de ahorro" del Home (Figma "INICIO"): tarjetas pequeñas con
 * ícono, nombre, barra de progreso del color de la meta, porcentaje y monto corto, y la
 * flecha para ver todas. Sin metas muestra un botón para crear la primera.
 */
@Composable
fun HomeGoalsSection(
    goals: List<Goal>,
    onOpenGoals: () -> Unit,
    onOpenGoal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_goals_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenGoals) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    stringResource(R.string.a11y_open_goals),
                    tint = pocketColors.Coral,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (goals.isEmpty()) {
            DashedActionButton(stringResource(R.string.home_goals_empty), onClick = onOpenGoals)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(goals, key = { it.id }) { goal ->
                    HomeGoalCard(goal, onClick = { onOpenGoal(goal.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeGoalCard(goal: Goal, onClick: () -> Unit) {
    Column(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalIconBadge(goal.icon, goal.color, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                goal.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        GoalProgressBar(
            progress = GoalRules.progress(goal.savedAmount, goal.targetAmount),
            color = CategoryColors.parse(goal.color),
            height = 6.dp,
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.goals_percent, GoalRules.percent(goal.savedAmount, goal.targetAmount)),
                fontSize = 11.sp,
                color = pocketColors.Muted,
                modifier = Modifier.weight(1f),
            )
            Text(GoalFormat.compactMoney(goal.targetAmount), fontSize = 11.sp, color = pocketColors.Muted)
        }
    }
}
