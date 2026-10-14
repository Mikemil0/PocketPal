package com.pocketpal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.CategoryIcon
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.PocketColors
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Categorías (RF-B7, §8.8; págs. 37 y 38). Pestañas Gastos/Ingresos, encabezado
 * con el conteo, filas con asa de arrastre (D-10), estado vacío con el botón de
 * crear (D-11) y el botón "+ Crear categoría personalizada".
 *
 * Los contadores de movimientos y los totales son 0 hasta que exista la tabla
 * `movements` (§6, "dependencia con módulos futuros").
 */
@Composable
fun CategoriesScreen(
    state: CategoriesViewModel.UiState,
    error: ErrorType?,
    currency: String,
    onBack: () -> Unit,
    onSelectKind: (CategoryKind) -> Unit,
    onMove: (fromId: String, toId: String) -> Unit,
    onCreate: (CategoryKind) -> Unit,
    onOpen: (String) -> Unit,
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

    AuthScaffold(
        onBack = onBack,
        scrollable = false,
        backBackground = PocketColors.ChipPink,
        backTint = PocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.categories_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        val content = state as? CategoriesViewModel.UiState.Content
        val kind = content?.data?.kind ?: CategoryKind.Expense
        val list = content?.data?.current ?: emptyList()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CategoryTab(
                text = stringResource(R.string.categories_tab_expenses),
                selected = kind == CategoryKind.Expense,
                onClick = { onSelectKind(CategoryKind.Expense) },
            )
            Spacer(Modifier.width(12.dp))
            CategoryTab(
                text = stringResource(R.string.categories_tab_income),
                selected = kind == CategoryKind.Income,
                onClick = { onSelectKind(CategoryKind.Income) },
            )
        }
        Spacer(Modifier.height(16.dp))

        when (state) {
            is CategoriesViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PocketColors.Coral)
            }

            is CategoriesViewModel.UiState.Error -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ErrorBanner(stringResource(state.type.messageRes()))
            }

            is CategoriesViewModel.UiState.Content -> {
                Text(
                    stringResource(R.string.categories_header, list.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PocketColors.Muted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                )

                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    onMove(from.key.toString(), to.key.toString())
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (list.isEmpty()) {
                        item(key = "empty") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    stringResource(R.string.categories_empty_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PocketColors.Ink,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.categories_empty_hint),
                                    fontSize = 12.sp,
                                    color = PocketColors.Muted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    items(list, key = { it.id }) { item ->
                        ReorderableItem(reorderableState, key = item.id) {
                            CategoryRow(
                                name = item.name,
                                icon = CategoryIcon.fromId(item.icon),
                                color = CategoryColors.parse(item.color),
                                movementCount = item.movementCount,
                                total = item.total,
                                currency = currency,
                                dragHandle = {
                                    Icon(
                                        Icons.Outlined.UnfoldMore,
                                        stringResource(R.string.a11y_reorder_category),
                                        tint = PocketColors.Coral,
                                        modifier = Modifier.draggableHandle().size(20.dp),
                                    )
                                },
                                onClick = { onOpen(item.id) },
                            )
                        }
                    }

                    item(key = "create") {
                        CreateCategoryButton(onClick = { onCreate(kind) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PocketColors.Navy else PocketColors.TabInactive)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else PocketColors.Navy,
        )
    }
}

/**
 * Fila de categoría (§8.8): asa de reordenar, ícono con color, nombre,
 * "N movimientos" y el total.
 *
 * El ícono va en coral (como el Figma) y el color de la categoría tiñe el chip
 * ("ficha clara" de §8.9); con el rosa del Figma se obtiene #FFE4E4.
 */
@Composable
private fun CategoryRow(
    name: String,
    icon: CategoryIcon,
    color: Color,
    movementCount: Int,
    total: Double,
    currency: String,
    dragHandle: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, PocketColors.RoseBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dragHandle()
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon.icon,
                stringResource(R.string.a11y_category_icon),
                tint = PocketColors.Coral,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PocketColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                pluralStringResource(R.plurals.category_movements, movementCount, movementCount),
                fontSize = 12.sp,
                color = PocketColors.Muted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            MoneyFormat.format(total, currency),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = PocketColors.Ink,
        )
    }
}

/** Botón "+ Crear categoría personalizada" (borde discontinuo, §8.8). */
@Composable
private fun CreateCategoryButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .dashedBorder(PocketColors.Coral, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Add,
            null,
            tint = PocketColors.Coral,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.categories_create),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PocketColors.Coral,
        )
    }
}

/** Borde discontinuo como el botón del Figma (Compose no lo trae en `border`). */
private fun Modifier.dashedBorder(
    color: Color,
    shape: RoundedCornerShape,
    strokeWidth: Dp = 2.dp,
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(shape.topStart.toPx(size, this)),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        ),
    )
}

private const val ERROR_SHOWN_MS = 3_500L
