package com.pocketpal.app.ui.movements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.DaiInsight
import com.pocketpal.app.data.movement.Recurrence
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay
import java.time.ZoneId

private const val RECEIPT_MESSAGE_MS = 3_000L

/**
 * Flujo 4 · "Detalle del movimiento" (Figma "03_detalle_movimiento"): tarjeta con el
 * título, la fecha y hora, el monto y la categoría; los datos (categoría, fecha,
 * descripción y, si es recurrente, frecuencia y próximo cobro); "Dai te ayuda" y los
 * botones Compartir / Descargar.
 */
@Composable
fun MovementDetailScreen(
    state: MovementDetailViewModel.UiState,
    currency: String,
    downloading: Boolean,
    receiptResult: MovementDetailViewModel.ReceiptResult?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    onShare: (ReceiptContent) -> Unit,
    onDownload: (ReceiptContent) -> Unit,
    onReceiptShown: () -> Unit,
) {
    LaunchedEffect(receiptResult) {
        if (receiptResult != null) {
            delay(RECEIPT_MESSAGE_MS)
            onReceiptShown()
        }
    }

    MovementsScaffold(title = stringResource(R.string.detail_title), onBack = onBack) {
        when (state) {
            MovementDetailViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = 80.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = pocketColors.Coral) }

            MovementDetailViewModel.UiState.NotFound -> NotFound(onBack)

            is MovementDetailViewModel.UiState.Error -> MovementsErrorState(onRetry = onRetry, onGoHome = onGoHome)

            is MovementDetailViewModel.UiState.Content -> {
                val receipt = rememberReceipt(state.data, currency)
                DetailContent(state.data, currency)
                Spacer(Modifier.height(20.dp))

                when (receiptResult) {
                    MovementDetailViewModel.ReceiptResult.Saved -> SavedMessage()
                    MovementDetailViewModel.ReceiptResult.Failed ->
                        ErrorBanner(stringResource(R.string.error_receipt_failed))
                    null -> Unit
                }
                if (receiptResult != null) Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PocketPrimaryButton(
                        text = stringResource(R.string.detail_share),
                        onClick = { onShare(receipt) },
                        modifier = Modifier.weight(1f),
                        containerColor = pocketColors.Card,
                        contentColor = pocketColors.Ink,
                        border = BorderStroke(1.dp, pocketColors.RoseBorder),
                    )
                    PocketPrimaryButton(
                        text = stringResource(R.string.detail_download),
                        onClick = { onDownload(receipt) },
                        modifier = Modifier.weight(1f),
                        loading = downloading,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailContent(data: MovementDetailViewModel.DetailUi, currency: String) {
    val movement = data.movement
    val locale = currentLocale()
    val dateTime = movement.localDateTime(ZoneId.systemDefault())

    // Tarjeta principal.
    DetailCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(data.category, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(movement.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                dateTime?.let {
                    Text(MovementFormat.dateTime(it, locale), fontSize = 12.sp, color = pocketColors.Muted)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    MovementFormat.signedAmount(movement, currency),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor(movement),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pocketColors.ChipPink)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(categoryLabel(movement, data.category), fontSize = 11.sp, color = pocketColors.Coral)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // Datos.
    DetailCard {
        Field(stringResource(R.string.detail_category), categoryLabel(movement, data.category))
        dateTime?.let { Field(stringResource(R.string.detail_date), MovementFormat.dateTime(it, locale)) }
        Field(stringResource(R.string.detail_kind), stringResource(if (movement.isIncome) R.string.movement_kind_income else R.string.movement_kind_expense))
        movement.description?.takeIf { it.isNotBlank() }?.let { Field(stringResource(R.string.detail_description), it) }
        movement.recurrenceType?.let { Field(stringResource(R.string.detail_frequency), recurrenceLabel(it)) }
        data.nextCharge?.let { Field(stringResource(R.string.detail_next_charge), MovementFormat.date(it, locale)) }
    }

    // "Dai te ayuda".
    data.insight?.let { insight ->
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(pocketColors.ChipPink)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(pocketColors.Navy),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SmartToy, null, tint = pocketColors.Coral, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.detail_dai_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
                Text(insightText(insight), fontSize = 12.sp, color = pocketColors.Muted)
            }
        }
    }
}

/** Texto de "Dai te ayuda" según el tipo de comparación. */
@Composable
private fun insightText(insight: DaiInsight): String = when (insight) {
    is DaiInsight.OfBudget -> stringResource(R.string.dai_of_budget, insight.percent, insight.categoryName.lowercase())
    is DaiInsight.OfCategorySpending -> stringResource(R.string.dai_of_category, insight.percent, insight.categoryName.lowercase())
    is DaiInsight.OfMonthSpending -> stringResource(R.string.dai_of_month_spending, insight.percent)
    is DaiInsight.OfMonthIncome -> stringResource(R.string.dai_of_month_income, insight.percent)
}

@Composable
fun recurrenceLabel(recurrence: Recurrence): String = stringResource(
    when (recurrence) {
        Recurrence.Weekly -> R.string.recurrence_weekly
        Recurrence.Monthly -> R.string.recurrence_monthly
        Recurrence.Yearly -> R.string.recurrence_yearly
    }
)

/** Arma el comprobante con los textos ya traducidos (se usa al compartir y al descargar). */
@Composable
private fun rememberReceipt(data: MovementDetailViewModel.DetailUi, currency: String): ReceiptContent {
    val movement = data.movement
    val locale = currentLocale()
    val dateTime = movement.localDateTime(ZoneId.systemDefault())
    val fields = buildList {
        add(stringResource(R.string.detail_category) to categoryLabel(movement, data.category))
        dateTime?.let { add(stringResource(R.string.detail_date) to MovementFormat.dateTime(it, locale)) }
        add(stringResource(R.string.detail_description) to movement.description.orEmpty())
        movement.recurrenceType?.let { add(stringResource(R.string.detail_frequency) to recurrenceLabel(it)) }
    }
    return ReceiptContent(
        heading = stringResource(R.string.receipt_heading),
        title = movement.title,
        amount = MovementFormat.signedAmount(movement, currency),
        fields = fields,
        footer = stringResource(R.string.receipt_footer),
        movementId = movement.id,
    )
}

@Composable
private fun SavedMessage() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(pocketColors.TealSoft)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, null, tint = pocketColors.Teal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.detail_receipt_saved), fontSize = 12.sp, color = pocketColors.Teal)
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.detail_not_found),
            fontSize = 15.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        PocketPrimaryButton(text = stringResource(R.string.detail_back_to_list), onClick = onBack)
    }
}

@Composable
internal fun DetailCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** Etiqueta gris pequeña con el valor debajo ("Categoría" / "Comida"). */
@Composable
internal fun Field(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = pocketColors.Muted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
    }
}
