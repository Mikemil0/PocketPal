package com.pocketpal.app.ui.movements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/** Iniciales de los días (0 = domingo), las mismas de "Inicio de semana" del Módulo 2. */
private val DAY_INITIALS = listOf(
    R.string.day_initial_sun, R.string.day_initial_mon, R.string.day_initial_tue, R.string.day_initial_wed,
    R.string.day_initial_thu, R.string.day_initial_fri, R.string.day_initial_sat,
)

/**
 * Flujo 3 · Filtro por fechas (Figma "02_calendario_filtro 1"): rango elegido arriba,
 * calendario del mes con puntos en los días que tienen movimientos, botones
 * "Últimos 30 días" / "Personalizado" y "Ver movimientos".
 */
@Composable
fun MovementCalendarScreen(
    state: MovementCalendarViewModel.CalendarUi,
    weekStart: Int,
    canGoNext: Boolean,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTapDay: (LocalDate) -> Unit,
    onLast30Days: () -> Unit,
    onCustom: () -> Unit,
    onApply: () -> Unit,
) {
    val locale = currentLocale()
    val today = LocalDate.now()

    MovementsScaffold(title = stringResource(R.string.movements_title), onBack = onBack) {
        // Rango elegido ("Abr 1 - Abr 30, 2025").
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = pocketColors.Coral)
            Spacer(Modifier.width(10.dp))
            Text(
                state.selection?.let { MovementFormat.rangeLabel(it, locale) } ?: stringResource(R.string.calendar_pick_start),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
            )
        }
        Spacer(Modifier.height(16.dp))

        // Mes visible con flechas.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                MovementFormat.monthTitle(state.month.atDay(1), locale),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.a11y_previous_month), tint = pocketColors.Ink)
            }
            IconButton(onClick = onNextMonth, enabled = canGoNext) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    stringResource(R.string.a11y_next_month),
                    tint = if (canGoNext) pocketColors.Ink else pocketColors.Gray,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Calendario.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                .padding(10.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                MovementCalendarViewModel.weekOrder(weekStart).forEach { day ->
                    Text(
                        stringResource(DAY_INITIALS[day]),
                        fontSize = 11.sp,
                        color = pocketColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MovementCalendarViewModel.monthGrid(state.month, weekStart).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                DayCell(
                                    date = date,
                                    selected = state.isSelected(date),
                                    edge = state.isEdge(date),
                                    hasMovements = date in state.daysWithMovements,
                                    enabled = !date.isAfter(today),
                                    onClick = { onTapDay(date) },
                                )
                            }
                        }
                    }
                    // La última semana puede quedar incompleta.
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeButton(
                text = stringResource(R.string.calendar_last_30),
                selected = state.mode == MovementCalendarViewModel.Mode.Last30Days,
                onClick = onLast30Days,
                modifier = Modifier.weight(1f),
            )
            ModeButton(
                text = stringResource(R.string.calendar_custom),
                selected = state.mode == MovementCalendarViewModel.Mode.Custom,
                onClick = onCustom,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(state.error.messageRes()))
        }

        Spacer(Modifier.height(20.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.calendar_apply),
            onClick = onApply,
            enabled = state.selection != null,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Un día: círculo coral si está en el rango (más fuerte en los extremos) y puntico si tiene movimientos. */
@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    edge: Boolean,
    hasMovements: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        edge -> pocketColors.Coral
                        selected -> pocketColors.Coral.copy(alpha = 0.55f)
                        else -> Color.Transparent
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    selected -> Color.White
                    !enabled -> pocketColors.Gray
                    else -> pocketColors.Ink
                },
            )
        }
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasMovements) pocketColors.Teal else Color.Transparent),
        )
    }
}

/** "Últimos 30 días" / "Personalizado": relleno coral si está activo, contorno si no. */
@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PocketPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        containerColor = if (selected) pocketColors.Coral else pocketColors.Card,
        contentColor = if (selected) Color.White else pocketColors.Ink,
        border = if (selected) null else BorderStroke(1.dp, pocketColors.RoseBorder),
    )
}
