package com.pocketpal.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate
import java.time.ZoneId

/**
 * Flujo 1 · Sección "Movimientos recientes" del inicio (Figma "INICIO"): los últimos
 * movimientos con su hora ("Hoy, 2:10 p. m.") y el enlace "ver todos" que abre la lista.
 */
@Composable
fun HomeMovementsSection(
    movements: List<Movement>,
    categories: Map<String, MovementCategory>,
    currency: String,
    onSeeAll: () -> Unit,
    onOpenMovement: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val locale = currentLocale()

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_movements_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.home_movements_see_all),
                fontSize = 12.sp,
                color = pocketColors.Coral,
                modifier = Modifier.clickable(onClick = onSeeAll).padding(8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            if (movements.isEmpty()) {
                Text(
                    stringResource(R.string.home_movements_empty),
                    fontSize = 13.sp,
                    color = pocketColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
            movements.forEachIndexed { index, movement ->
                val category = movement.categoryId?.let { categories[it] }
                val dateTime = movement.localDateTime(zone)
                val subtitle = if (dateTime != null) {
                    "${relativeDay(dateTime.toLocalDate(), today)}, ${MovementFormat.time(dateTime, locale)}"
                } else {
                    categoryLabel(movement, category)
                }
                MovementRow(
                    movement = movement,
                    category = category,
                    subtitle = subtitle,
                    currency = currency,
                    onClick = { onOpenMovement(movement.id) },
                )
                if (index < movements.lastIndex) HorizontalDivider(color = pocketColors.ChipPink)
            }
        }
    }
}
