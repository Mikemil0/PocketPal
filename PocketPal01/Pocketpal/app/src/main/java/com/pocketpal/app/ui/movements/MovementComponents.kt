package com.pocketpal.app.ui.movements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CreditCardOff
import androidx.compose.material.icons.outlined.Dashboard
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
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.CategoryIcon
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/*
 * Piezas visuales compartidas por todas las pantallas de Movimientos (base del Módulo 4).
 * Reutilizan los íconos y colores de las categorías del Módulo 2.
 */

/**
 * Marco de las pantallas de Movimientos del Figma: franja azul oscuro arriba con la
 * flecha atrás, el título y acciones opcionales (lupa, menú), y debajo una "hoja"
 * clara con las esquinas superiores redondeadas donde va el contenido.
 */
@Composable
fun MovementsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(pocketColors.Navy)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.a11y_back), tint = Color.White)
            }
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        val sheet = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(pocketColors.Cream)
            .padding(horizontal = 20.dp, vertical = 20.dp)
        Column(if (scrollable) sheet.verticalScroll(rememberScrollState()) else sheet) {
            content()
        }
    }
}

/** Ícono de la categoría en un cuadrito de su color; sin categoría, ícono gris "Otros". */
@Composable
fun CategoryBadge(category: MovementCategory?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = category?.let { CategoryColors.parse(it.color) } ?: pocketColors.Muted
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            category?.let { CategoryIcon.fromId(it.icon).icon } ?: Icons.Outlined.Dashboard,
            stringResource(R.string.a11y_movement_category),
            tint = if (category != null) color else pocketColors.Muted,
            modifier = Modifier.size(size / 2),
        )
    }
}

/** Color del monto: verde (teal) para ingresos, coral para gastos. */
@Composable
fun amountColor(movement: Movement): Color = if (movement.isIncome) pocketColors.Teal else pocketColors.Coral

/**
 * Fila de un movimiento: ícono de la categoría, título, una línea gris debajo
 * ("Comida · Hoy") y el monto con signo a la derecha.
 */
@Composable
fun MovementRow(
    movement: Movement,
    category: MovementCategory?,
    subtitle: String,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                movement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(subtitle, fontSize = 11.sp, color = pocketColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            MovementFormat.signedAmount(movement, currency),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor(movement),
        )
    }
}

/** Nombre de la categoría, o "Ingreso"/"Sin categoría" si no tiene. */
@Composable
fun categoryLabel(movement: Movement, category: MovementCategory?): String = when {
    category != null -> category.name
    movement.isIncome -> stringResource(R.string.movement_kind_income)
    else -> stringResource(R.string.movement_no_category)
}

/** "Hoy", "Ayer" o "12 abr" según qué tan lejos esté la fecha. */
@Composable
fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.movements_today)
    today.minusDays(1) -> stringResource(R.string.movements_yesterday)
    else -> MovementFormat.shortDate(date, com.pocketpal.app.ui.goals.currentLocale())
}

/** Botón tipo píldora de los filtros ("Hoy", "7 días", "Todos", "Gastos"…). */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) pocketColors.Coral else pocketColors.ChipPink)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else pocketColors.Coral,
        )
    }
}

/** Círculo rosado grande con un ícono, para los estados vacío y error. */
@Composable
private fun StateIllustration(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.size(120.dp).clip(CircleShape).background(pocketColors.ChipPink),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = pocketColors.Coral, modifier = Modifier.size(56.dp))
    }
}

/** Figma "05_sin_movimientos": "No hay movimientos en este período" + "Volver al inicio". */
@Composable
fun MovementsEmptyState(onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StateIllustration(Icons.Outlined.CreditCardOff)
        Text(
            stringResource(R.string.movements_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.movements_empty_body),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PocketPrimaryButton(text = stringResource(R.string.movements_go_home), onClick = onGoHome)
    }
}

/** Figma "08_error_datos": "Ups… no pudimos cargar tus movimientos" + Reintentar / Volver al inicio. */
@Composable
fun MovementsErrorState(onRetry: () -> Unit, onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StateIllustration(Icons.Outlined.CloudOff)
        Text(
            stringResource(R.string.movements_error_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.movements_error_body),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PocketPrimaryButton(text = stringResource(R.string.movements_retry), onClick = onRetry)
        PocketPrimaryButton(
            text = stringResource(R.string.movements_go_home),
            onClick = onGoHome,
            containerColor = pocketColors.Card,
            contentColor = pocketColors.Coral,
            border = BorderStroke(1.dp, pocketColors.Coral),
        )
    }
}
