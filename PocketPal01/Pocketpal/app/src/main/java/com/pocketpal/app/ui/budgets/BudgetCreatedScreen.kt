package com.pocketpal.app.ui.budgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.budget.BudgetStatus
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 2 · "Presupuesto creado" (Figma "PRE EXITOSO"): chulo verde, "Comida ya está activo
 * con un límite mensual de $500.000", la tarjeta del presupuesto y los botones
 * "Ver mis presupuestos" / "Crear otro presupuesto".
 * El botón atrás del sistema va a la lista (no vuelve al formulario ya guardado).
 */
@Composable
fun BudgetCreatedScreen(
    status: BudgetStatus,
    currency: String,
    onSeeBudgets: () -> Unit,
    onCreateAnother: () -> Unit,
) {
    BackHandler(onBack = onSeeBudgets)
    val limit = MoneyFormat.format(status.budget.amountLimit, currency)

    AuthScaffold {
        Spacer(Modifier.height(56.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(96.dp).border(10.dp, pocketColors.Teal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = pocketColors.Teal, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.budget_created_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(
                when (status.budget.budgetPeriod) {
                    BudgetPeriod.Monthly -> R.string.budget_created_body_monthly
                    BudgetPeriod.Weekly -> R.string.budget_created_body_weekly
                },
                status.budget.name,
                limit,
            ),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(36.dp))
        BudgetCard(status = status, currency = currency, onClick = onSeeBudgets)

        Spacer(Modifier.height(56.dp))
        PocketPrimaryButton(text = stringResource(R.string.budget_created_see_all), onClick = onSeeBudgets)
        Spacer(Modifier.height(12.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.budget_created_another),
            onClick = onCreateAnother,
            containerColor = pocketColors.Card,
            contentColor = pocketColors.Coral,
            border = BorderStroke(1.dp, pocketColors.Coral),
        )
        Spacer(Modifier.height(24.dp))
    }
}
