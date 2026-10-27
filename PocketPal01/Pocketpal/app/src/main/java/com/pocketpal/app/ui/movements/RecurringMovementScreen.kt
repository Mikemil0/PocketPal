package com.pocketpal.app.ui.movements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.Recurrence
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.theme.pocketColors

/** Ámbar suave del aviso "Movimiento recurrente" (como en el Figma). */
private val RecurringBannerColor = Color(0xFFFFF1CC)

/**
 * Flujo 4 · "Movimiento recurrente" (Figma "04_movimiento_recurrente"): aviso de que se
 * repite, tarjeta con el monto, la categoría, la frecuencia y el próximo cobro, y
 * "Ver más detalles" que abre el detalle completo.
 */
@Composable
fun RecurringMovementScreen(
    state: MovementDetailViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    onSeeDetails: () -> Unit,
) {
    MovementsScaffold(title = stringResource(R.string.movements_title), onBack = onBack) {
        when (state) {
            is MovementDetailViewModel.UiState.Content -> Content(state.data, currency, onSeeDetails)
            is MovementDetailViewModel.UiState.Error -> MovementsErrorState(onRetry = onRetry, onGoHome = onGoHome)
            else -> Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
        }
    }
}

@Composable
private fun Content(data: MovementDetailViewModel.DetailUi, currency: String, onSeeDetails: () -> Unit) {
    val movement = data.movement
    val locale = currentLocale()
    val recurrence = movement.recurrenceType ?: Recurrence.Monthly

    // Aviso "Movimiento recurrente · Se repite cada mes".
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RecurringBannerColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = pocketColors.Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.recurring_banner_title), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Text(repeatsText(recurrence), fontSize = 11.sp, color = pocketColors.Muted)
        }
    }
    Spacer(Modifier.height(12.dp))

    DetailCard {
        Row(verticalAlignment = Alignment.Top) {
            CategoryBadge(data.category)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(movement.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                Text(
                    MovementFormat.signedAmount(movement, currency),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor(movement),
                )
                Spacer(Modifier.height(4.dp))
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
        HorizontalDivider(color = pocketColors.ChipPink)
        Field(stringResource(R.string.detail_frequency), recurrenceLabel(recurrence))
        data.nextCharge?.let { Field(stringResource(R.string.detail_next_charge), MovementFormat.date(it, locale)) }
    }
    Spacer(Modifier.height(16.dp))

    PocketPrimaryButton(
        text = stringResource(R.string.recurring_see_details),
        onClick = onSeeDetails,
        containerColor = pocketColors.Card,
        contentColor = pocketColors.Coral,
        border = BorderStroke(1.dp, pocketColors.Coral),
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun repeatsText(recurrence: Recurrence): String = stringResource(
    when (recurrence) {
        Recurrence.Weekly -> R.string.recurring_repeats_weekly
        Recurrence.Monthly -> R.string.recurring_repeats_monthly
        Recurrence.Yearly -> R.string.recurring_repeats_yearly
    }
)
