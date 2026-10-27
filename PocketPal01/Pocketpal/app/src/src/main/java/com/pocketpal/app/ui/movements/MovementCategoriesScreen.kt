package com.pocketpal.app.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.CategoryTotal
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.theme.pocketColors
import kotlin.math.abs

/**
 * Flujo 3 · "Movimientos por categorías" (Figma "01_Movimientos por categorias 1"):
 * tarjeta azul "Gastos este mes" con la variación frente al mes pasado y la lista de
 * categorías con su total y una barra del color de la categoría.
 */
@Composable
fun MovementCategoriesScreen(
    state: MovementCategoriesViewModel.UiState,
    currency: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
) {
    MovementsScaffold(title = stringResource(R.string.mov_categories_title), onBack = onBack) {
        when (state) {
            MovementCategoriesViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().padding(vertical = 80.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = pocketColors.Coral) }

            is MovementCategoriesViewModel.UiState.Error -> MovementsErrorState(onRetry = onRetry, onGoHome = onGoHome)

            is MovementCategoriesViewModel.UiState.Content -> {
                val data = state.data
                if (data.totals.isEmpty()) {
                    MovementsEmptyState(onGoHome = onGoHome)
                } else {
                    Content(data, currency)
                }
            }
        }
    }
}

@Composable
private fun Content(data: MovementCategoriesViewModel.CategoriesUi, currency: String) {
    Text(stringResource(R.string.mov_categories_month_label), fontSize = 12.sp, color = pocketColors.Muted)
    Spacer(Modifier.height(8.dp))

    // Tarjeta azul oscuro con el total de gastos del mes.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Navy)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.mov_categories_month_label), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Text(
            MoneyFormat.format(data.monthExpense, currency),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        data.change?.let { change ->
            Spacer(Modifier.height(4.dp))
            // Gastar menos es buena noticia (verde); gastar más, alerta (coral).
            Text(
                stringResource(
                    if (change <= 0) R.string.mov_categories_change_less else R.string.mov_categories_change_more,
                    abs(change),
                ),
                fontSize = 12.sp,
                color = if (change <= 0) pocketColors.Teal else pocketColors.Coral,
            )
        }
    }
    Spacer(Modifier.height(20.dp))

    Text(
        stringResource(R.string.mov_categories_header),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = pocketColors.Ink,
    )
    Spacer(Modifier.height(8.dp))
    data.totals.forEach { total ->
        CategoryTotalRow(total, total.categoryId?.let { data.categories[it] }, currency)
    }
    Spacer(Modifier.height(24.dp))
}

/** Ícono, nombre, total a la derecha y barra del color de la categoría. */
@Composable
private fun CategoryTotalRow(total: CategoryTotal, category: MovementCategory?, currency: String) {
    val color = category?.let { CategoryColors.parse(it.color) } ?: pocketColors.Muted
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category?.name ?: stringResource(R.string.mov_categories_others),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    MoneyFormat.format(total.total, currency),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Ink,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pocketColors.ChipPink),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(total.fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(color),
                )
            }
        }
    }
}
