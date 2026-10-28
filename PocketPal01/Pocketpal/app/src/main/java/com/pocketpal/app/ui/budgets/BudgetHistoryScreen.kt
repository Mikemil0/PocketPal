package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.budget.MonthUsage
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.movements.MovementFormat
import com.pocketpal.app.ui.theme.pocketColors
import java.time.format.TextStyle
import java.util.Locale

/**
 * Flujo 3 · "Historial - Comida" (Figma "HISTORIAL").
 *
 * "ÚLTIMOS 6 MESES": una barra por mes, llena según lo gastado frente al límite (la del
 * mes actual en coral). "DETALLES POR MES": los meses con gastos, el más reciente primero.
 */
@Composable
fun BudgetHistoryScreen(
    state: BudgetDetailViewModel.HistoryState,
    currency: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val name = (state as? BudgetDetailViewModel.HistoryState.Content)?.data?.budget?.name.orEmpty()

    AuthScaffold {
        BudgetTopBar(stringResource(R.string.budget_history_title, name), onBack = onBack)
        when (state) {
            BudgetDetailViewModel.HistoryState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
            is BudgetDetailViewModel.HistoryState.Error -> BudgetsErrorState(onRetry = onRetry)
            is BudgetDetailViewModel.HistoryState.Content -> {
                val data = state.data
                val locale = currentLocale()
                BudgetSectionLabel(stringResource(R.string.budget_history_last_months))
                MonthBars(months = data.months, locale = locale)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = pocketColors.ChipPink)
                BudgetSectionLabel(stringResource(R.string.budget_history_by_month))
                if (data.monthsWithSpending.isEmpty()) {
                    Text(
                        stringResource(R.string.budget_history_empty),
                        fontSize = 13.sp,
                        color = pocketColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.monthsWithSpending.forEach { usage -> MonthRow(usage, currency, locale) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Las 6 barras verticales con la inicial del mes debajo ("Abr", "May"…). */
@Composable
private fun MonthBars(months: List<MonthUsage>, locale: Locale) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.Coral, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        months.forEachIndexed { index, usage ->
            val current = index == months.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(22.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (current) pocketColors.ChipPink else pocketColors.Gray.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(usage.fraction.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (current) pocketColors.Coral else pocketColors.Gray.copy(alpha = 0.6f)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    usage.month.month.getDisplayName(TextStyle.SHORT, locale).replace(".", "")
                        .replaceFirstChar { it.titlecase(locale) },
                    fontSize = 10.sp,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (current) pocketColors.Coral else pocketColors.Muted,
                )
            }
        }
    }
}

/** "Septiembre 2026 · $420.000 / $500.000 · 85% usado". */
@Composable
private fun MonthRow(usage: MonthUsage, currency: String, locale: Locale) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            MovementFormat.monthTitle(usage.month.atDay(1), locale),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${MoneyFormat.format(usage.spent, currency)} / ${MoneyFormat.format(usage.limit, currency)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (usage.spent > usage.limit) pocketColors.ErrorRed else pocketColors.Coral,
            )
            Text(stringResource(R.string.budget_used, usage.percent), fontSize = 11.sp, color = pocketColors.Muted)
        }
    }
}
