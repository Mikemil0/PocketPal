package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 1 · "Presupuestos" (Figma "PRESUPUESTOS LISTA").
 *
 * Arriba, la franja azul con "Presupuestado" (suma de los límites) y "Gastado" (suma de lo
 * gastado en el período de cada uno). Abajo, "N categorías activas", una tarjeta por
 * presupuesto y el botón "Crear nuevo presupuesto".
 *
 * Flujo 4 · Dejar presionada una tarjeta abre "Editar presupuesto" ([onEdit]).
 */
@Composable
fun BudgetsScreen(
    state: BudgetsViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit = {},
) {
    val content = (state as? BudgetsViewModel.UiState.Content)?.data

    Column(Modifier.fillMaxSize().background(pocketColors.Navy).systemBarsPadding()) {
        // ---- Franja azul: atrás, título y los dos totales ----
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp).background(pocketColors.ChipPink, RoundedCornerShape(8.dp)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.a11y_back),
                        tint = pocketColors.Coral,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.budgets_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryBox(
                    label = stringResource(R.string.budgets_total_limit),
                    value = MoneyFormat.format(content?.summary?.limit ?: 0.0, currency),
                    modifier = Modifier.weight(1f),
                )
                SummaryBox(
                    label = stringResource(R.string.budgets_total_spent),
                    value = MoneyFormat.format(content?.summary?.spent ?: 0.0, currency),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---- Hoja clara con la lista ----
        Column(
            Modifier
                .fillMaxSize()
                .background(pocketColors.Cream)
                .padding(horizontal = 20.dp),
        ) {
            when (state) {
                BudgetsViewModel.UiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = pocketColors.Coral)
                }
                is BudgetsViewModel.UiState.Error -> BudgetsErrorState(onRetry = onRetry)
                is BudgetsViewModel.UiState.Content -> {
                    val data = state.data
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            pluralStringResource(R.plurals.budgets_active_count, data.summary.active, data.summary.active),
                            fontSize = 13.sp,
                            color = pocketColors.Muted,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (data.isEmpty) {
                            Text(
                                stringResource(R.string.budgets_empty),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pocketColors.Coral,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            )
                        }
                        data.statuses.forEach { status ->
                            BudgetCard(
                                status = status,
                                currency = currency,
                                onClick = { onOpen(status.budget.id) },
                                onLongClick = { onEdit(status.budget.id) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    PocketPrimaryButton(
                        text = stringResource(R.string.budgets_create),
                        onClick = onCreate,
                        icon = Icons.Outlined.Add,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** Cajita de la franja azul ("Presupuestado $1.850.000"). */
@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pocketColors.NavyLight)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
