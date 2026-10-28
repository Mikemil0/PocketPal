package com.pocketpal.app.ui.budgets

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.budget.BudgetDetail
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.movements.CategoryBadge
import com.pocketpal.app.ui.movements.MovementFormat
import com.pocketpal.app.ui.movements.MovementRow
import com.pocketpal.app.ui.movements.relativeDay
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 3 · Detalle del presupuesto (Figma "DETALLE PRE").
 *
 * Tarjeta con "Límite mensual · Se reinicia en 17 días", "$420.000 de $500.000", la barra,
 * "85% usado" y "Quedan $80.000"; los botones "Registrar gasto" y "Ver historial"; y los
 * gastos del período. Tocar un gasto abre su detalle (Módulo 4).
 *
 * Flujo 4 · Tocar la tarjeta del límite abre "Editar presupuesto" ([onEdit]).
 */
@Composable
fun BudgetDetailScreen(
    state: BudgetDetailViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMovement: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit = {},
) {
    val title = (state as? BudgetDetailViewModel.UiState.Content)?.data?.status?.budget?.name.orEmpty()

    AuthScaffold {
        BudgetTopBar(title, onBack = onBack)
        Spacer(Modifier.height(16.dp))
        when (state) {
            BudgetDetailViewModel.UiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
            BudgetDetailViewModel.UiState.NotFound -> Text(
                stringResource(R.string.budget_not_found),
                fontSize = 15.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            )
            is BudgetDetailViewModel.UiState.Error -> BudgetsErrorState(onRetry = onRetry)
            is BudgetDetailViewModel.UiState.Content -> DetailContent(
                detail = state.data,
                currency = currency,
                onOpenHistory = onOpenHistory,
                onOpenMovement = onOpenMovement,
                onEdit = onEdit,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailContent(
    detail: BudgetDetail,
    currency: String,
    onOpenHistory: () -> Unit,
    onOpenMovement: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val status = detail.status
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.budget_register_coming_soon)

    // ---- Tarjeta del límite ----
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.Coral, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(status.category, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(periodLimitLabel(status.budget.budgetPeriod), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                Text(
                    pluralStringResource(R.plurals.budget_resets_in, detail.daysUntilReset, detail.daysUntilReset),
                    fontSize = 11.sp,
                    color = pocketColors.Muted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(MoneyFormat.format(status.spent, currency), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.budget_of_limit, MoneyFormat.format(status.budget.amountLimit, currency)),
                fontSize = 12.sp,
                color = pocketColors.Muted,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        BudgetProgressBar(status.fraction, if (status.exceeded) pocketColors.ErrorRed else pocketColors.Coral)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                stringResource(R.string.budget_used, status.percent),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status.exceeded) pocketColors.ErrorRed else pocketColors.Coral,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (status.exceeded) {
                    stringResource(R.string.budget_over_by, MoneyFormat.format(status.over, currency))
                } else {
                    stringResource(R.string.budget_remaining, MoneyFormat.format(status.remaining, currency))
                },
                fontSize = 11.sp,
                color = if (status.exceeded) pocketColors.ErrorRed else pocketColors.Muted,
            )
        }
    }

    // ---- Aviso "Avísame al acercarme al límite" ----
    if (detail.alertReached) {
        Spacer(Modifier.height(12.dp))
        ErrorBanner(
            stringResource(R.string.budget_alert_reached, status.percent),
            icon = Icons.Outlined.NotificationsActive,
        )
    }

    // ---- Botones ----
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PocketPrimaryButton(
            text = stringResource(R.string.budget_register_expense),
            // Registrar gastos llega con Dai (fotos de recibos); por ahora se avisa.
            onClick = { Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show() },
            modifier = Modifier.weight(1f),
        )
        PocketPrimaryButton(
            text = stringResource(R.string.budget_see_history),
            onClick = onOpenHistory,
            modifier = Modifier.weight(1f),
            containerColor = pocketColors.Card,
            contentColor = pocketColors.Ink,
            border = BorderStroke(1.dp, pocketColors.RoseBorder),
        )
    }

    // ---- Gastos del período ----
    BudgetSectionLabel(
        stringResource(
            when (status.budget.budgetPeriod) {
                BudgetPeriod.Monthly -> R.string.budget_movements_month
                BudgetPeriod.Weekly -> R.string.budget_movements_week
            }
        )
    )
    if (detail.movements.isEmpty()) {
        Text(
            stringResource(R.string.budget_no_movements),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
    } else {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val locale = currentLocale()
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            detail.movements.forEachIndexed { index, movement ->
                val dateTime = movement.localDateTime(zone)
                MovementRow(
                    movement = movement,
                    category = status.category,
                    subtitle = dateTime?.let { "${relativeDay(it.toLocalDate(), today)}, ${MovementFormat.time(it, locale)}" }.orEmpty(),
                    currency = currency,
                    onClick = { onOpenMovement(movement.id) },
                )
                if (index < detail.movements.lastIndex) HorizontalDivider(color = pocketColors.ChipPink)
            }
        }
    }
}
