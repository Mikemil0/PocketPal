package com.pocketpal.app.ui.movements

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.DayGroup
import com.pocketpal.app.data.movement.QuickFilter
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val ERROR_SHOWN_MS = 4_000L

/**
 * Flujo 1 · Lista principal (Figma "01_lista_principal"): total y resumen del mes,
 * botón "Por categoría", filtros Hoy / 7 días / Este mes + calendario, y la lista
 * agrupada por día. Sin datos muestra "05_sin_movimientos"; con error, "08_error_datos".
 *
 * Los botones de búsqueda, categorías, calendario y cada fila llaman a funciones que
 * conectan los otros flujos (2, 3 y 4) desde la navegación.
 */
@Composable
fun MovementsScreen(
    state: MovementsViewModel.UiState,
    error: ErrorType?,
    currency: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectFilter: (QuickFilter) -> Unit,
    onOpenMovement: (String) -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    onErrorShown: () -> Unit,
) {
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }

    MovementsScaffold(
        title = stringResource(R.string.movements_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, stringResource(R.string.a11y_search_movements), tint = Color.White)
            }
        },
    ) {
        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }
        when (state) {
            MovementsViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = 80.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = pocketColors.Coral) }

            is MovementsViewModel.UiState.Error -> MovementsErrorState(onRetry = onRetry, onGoHome = onGoHome)

            is MovementsViewModel.UiState.Content -> {
                val data = state.data
                if (data.monthIsEmpty && data.period == MovementsViewModel.Period.Quick(QuickFilter.ThisMonth)) {
                    MovementsEmptyState(onGoHome = onGoHome)
                } else {
                    Content(data, currency, onOpenCategories, onOpenCalendar, onSelectFilter, onOpenMovement, onGoHome)
                }
            }
        }
    }
}

@Composable
private fun Content(
    data: MovementsViewModel.MovementsUi,
    currency: String,
    onOpenCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectFilter: (QuickFilter) -> Unit,
    onOpenMovement: (String) -> Unit,
    onGoHome: () -> Unit,
) {
    val today = LocalDate.now()
    val locale = currentLocale()

    // "Total del mes" + variación frente al mes anterior.
    Card {
        Text(stringResource(R.string.movements_month_total), fontSize = 12.sp, color = pocketColors.Muted)
        Spacer(Modifier.height(4.dp))
        Text(
            (if (data.monthSummary.balance < 0) "- " else "") + MoneyFormat.format(abs(data.monthSummary.balance), currency),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = pocketColors.Ink,
        )
        data.monthChange?.let { change ->
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    if (change >= 0) R.string.movements_change_up else R.string.movements_change_down,
                    abs(change),
                ),
                fontSize = 12.sp,
                color = if (change >= 0) pocketColors.Teal else pocketColors.Coral,
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    // "Resumen del mes": ingresos | gastos.
    Card {
        Text(
            stringResource(R.string.movements_month_summary),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.movements_income), fontSize = 12.sp, color = pocketColors.Teal)
                Text(
                    "+" + MoneyFormat.format(data.monthSummary.income, currency),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Teal,
                )
            }
            Box(Modifier.width(1.dp).height(40.dp).background(pocketColors.ChipPink))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.movements_expenses), fontSize = 12.sp, color = pocketColors.Coral)
                Text(
                    "-" + MoneyFormat.format(data.monthSummary.expense, currency),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Coral,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // "Por categoría ›"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, pocketColors.Coral, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenCategories)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.movements_by_category),
            fontSize = 13.sp,
            color = pocketColors.Coral,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = pocketColors.Coral)
    }
    Spacer(Modifier.height(12.dp))

    // Filtros rápidos + calendario.
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selected = (data.period as? MovementsViewModel.Period.Quick)?.filter
        PillChip(stringResource(R.string.movements_filter_today), selected == QuickFilter.Today, { onSelectFilter(QuickFilter.Today) })
        PillChip(stringResource(R.string.movements_filter_week), selected == QuickFilter.Last7Days, { onSelectFilter(QuickFilter.Last7Days) })
        PillChip(stringResource(R.string.movements_filter_month), selected == QuickFilter.ThisMonth, { onSelectFilter(QuickFilter.ThisMonth) })
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenCalendar, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.CalendarMonth,
                stringResource(R.string.a11y_filter_dates),
                tint = if (data.period is MovementsViewModel.Period.Custom) pocketColors.Coral else pocketColors.Muted,
            )
        }
    }
    // Con un rango del calendario, se muestra cuál es ("Abr 1 - Abr 30, 2026").
    if (data.period is MovementsViewModel.Period.Custom) {
        Spacer(Modifier.height(8.dp))
        PillChip(MovementFormat.rangeLabel(data.range, locale), selected = true, onClick = onOpenCalendar)
    }
    Spacer(Modifier.height(12.dp))

    if (data.isEmpty) {
        MovementsEmptyState(onGoHome = onGoHome, modifier = Modifier.padding(top = 0.dp))
    } else {
        data.groups.forEach { group ->
            DaySection(group, data, today, currency, onOpenMovement)
        }
    }
    Spacer(Modifier.height(24.dp))
}

/** Encabezado "Hoy, 12 abr 2025" y las filas de ese día separadas por líneas. */
@Composable
private fun DaySection(
    group: DayGroup,
    data: MovementsViewModel.MovementsUi,
    today: LocalDate,
    currency: String,
    onOpenMovement: (String) -> Unit,
) {
    val locale = currentLocale()
    Text(
        stringResource(R.string.movements_day_header, dayName(group.date, today), MovementFormat.date(group.date, locale)),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Ink,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
    group.movements.forEachIndexed { index, movement ->
        val category = movement.categoryId?.let { data.categories[it] }
        MovementRow(
            movement = movement,
            category = category,
            subtitle = "${categoryLabel(movement, category)} · ${relativeDay(group.date, today)}",
            currency = currency,
            onClick = { onOpenMovement(movement.id) },
        )
        if (index < group.movements.lastIndex) HorizontalDivider(color = pocketColors.ChipPink)
    }
}

/** "Hoy", "Ayer" o el día de la semana ("Sáb") para el encabezado de cada día. */
@Composable
private fun dayName(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.movements_today)
    today.minusDays(1) -> stringResource(R.string.movements_yesterday)
    else -> {
        val locale = currentLocale()
        DateTimeFormatter.ofPattern("EEE", locale).format(date).replace(".", "")
            .replaceFirstChar { it.titlecase(locale) }
    }
}

/** Tarjeta blanca redondeada de los resúmenes. */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) { content() }
}
