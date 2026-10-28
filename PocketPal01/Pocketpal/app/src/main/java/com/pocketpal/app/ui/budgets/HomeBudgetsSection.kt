package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 1 · Tarjeta "Presupuestos" del inicio (componente "Presupuestos" del Figma, con
 * sus 3 variantes):
 * - 2 o más presupuestos: las dos primeras tarjetas.
 * - 1 presupuesto: su tarjeta y "¿Necesitas presupuestar otra categoría? ¡Hazlo ya!".
 * - ninguno: "Aún no tienes presupuestos hechos. ¡Empieza ya!".
 * "ver todos" abre la lista; los textos coral abren "Nuevo presupuesto".
 */
@Composable
fun HomeBudgetsSection(
    home: BudgetsViewModel.HomeBudgets,
    currency: String,
    onSeeAll: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.budgets_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.home_budgets_see_all),
                fontSize = 12.sp,
                color = pocketColors.Coral,
                modifier = Modifier.clickable(onClick = onSeeAll).padding(8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        home.statuses.forEach { status ->
            BudgetCard(status = status, currency = currency, onClick = { onOpen(status.budget.id) })
            Spacer(Modifier.height(8.dp))
        }
        when {
            !home.loaded -> Unit
            home.statuses.isEmpty() -> HomeInvite(stringResource(R.string.home_budgets_empty), onCreate)
            home.statuses.size == 1 && home.canAddMore -> HomeInvite(stringResource(R.string.home_budgets_add_more), onCreate)
        }
    }
}

/** Texto gris + llamada coral ("…¡Hazlo ya!") que abre "Nuevo presupuesto". */
@Composable
private fun HomeInvite(text: String, onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onCreate).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val lines = text.split("\n")
        Text(
            lines.first(),
            fontSize = 12.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
        if (lines.size > 1) {
            Text(
                lines.drop(1).joinToString(" "),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Coral,
                textAlign = TextAlign.Center,
            )
        }
    }
}
