package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.budget.BudgetStatus
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.movements.CategoryBadge
import com.pocketpal.app.ui.theme.pocketColors

/*
 * Piezas visuales compartidas por todas las pantallas de Presupuestos (base del Módulo 5).
 * Reutilizan el ícono de categoría del Módulo 4 (CategoryBadge) y los colores del Módulo 2.
 */

/** Color de la categoría del presupuesto (coral si no se encuentra). */
@Composable
fun budgetColor(category: MovementCategory?): Color =
    category?.let { CategoryColors.parse(it.color) } ?: pocketColors.Coral

/** Barra delgada: fondo del color de la categoría muy claro y avance del color fuerte. */
@Composable
fun BudgetProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.25f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** "$420.000 de $500.000". */
@Composable
fun spentOfLimit(spent: Double, limit: Double, currency: String): String =
    stringResource(R.string.budget_spent_of_limit, MoneyFormat.format(spent, currency), MoneyFormat.format(limit, currency))

/** "Límite mensual" / "Límite semanal". */
@Composable
fun periodLimitLabel(period: BudgetPeriod): String = stringResource(
    when (period) {
        BudgetPeriod.Monthly -> R.string.budget_limit_monthly
        BudgetPeriod.Weekly -> R.string.budget_limit_weekly
    }
)

/**
 * Tarjeta de un presupuesto (Figma "PRESUPUESTOS LISTA" y tarjeta del inicio): ícono de la
 * categoría, nombre, "$420.000 de $500.000", el porcentaje a la derecha y la barra.
 * [onLongClick] (dejar presionado) abre "Editar presupuesto" en la lista.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetCard(
    status: BudgetStatus,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val color = budgetColor(status.category)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(status.category, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    status.budget.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    spentOfLimit(status.spent, status.budget.amountLimit, currency),
                    fontSize = 11.sp,
                    color = pocketColors.Muted,
                    maxLines = 1,
                )
            }
            Text(
                stringResource(R.string.budget_percent, status.percent),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status.exceeded) pocketColors.ErrorRed else pocketColors.Coral,
            )
        }
        Spacer(Modifier.height(10.dp))
        BudgetProgressBar(status.fraction, color)
    }
}

/** Fila superior de las pantallas claras: botón atrás rosado y el título. */
@Composable
fun BudgetTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, centered: Boolean = false) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
                .background(pocketColors.ChipPink, RoundedCornerShape(8.dp)),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.a11y_back),
                tint = pocketColors.Coral,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = if (centered) {
                Modifier.align(Alignment.Center).padding(horizontal = 44.dp)
            } else {
                Modifier.align(Alignment.CenterStart).padding(start = 44.dp)
            },
        )
    }
}

/** Rótulo gris en mayúsculas de las secciones ("MOVIMIENTOS DE ESTE MES"). */
@Composable
fun BudgetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Muted,
        letterSpacing = 0.5.sp,
        modifier = modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
    )
}

/** "Ups… no pudimos cargar tus presupuestos" + Reintentar. */
@Composable
fun BudgetsErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(110.dp).clip(CircleShape).background(pocketColors.ChipPink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CloudOff, null, tint = pocketColors.Coral, modifier = Modifier.size(52.dp))
        }
        Text(
            stringResource(R.string.budgets_error_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.budgets_error_body),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PocketPrimaryButton(text = stringResource(R.string.budgets_retry), onClick = onRetry)
    }
}
