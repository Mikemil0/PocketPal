package com.pocketpal.app.ui.budgets

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
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.movements.CategoryBadge
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 2 · "Elegir categoría" (Figma "ELEGIR CAT" y "ELEGIR CAT - TODAS").
 *
 * Muestra las categorías de gastos que todavía no tienen presupuesto, cada una con lo
 * gastado este mes (y su límite del Módulo 2 si lo tiene). Si no queda ninguna:
 * "Ya tienes presupuesto en todas tus categorías".
 */
@Composable
fun BudgetCategoryPickerScreen(
    categories: List<MovementCategory>,
    monthSpent: Map<String, Double>,
    selectedId: String?,
    loaded: Boolean,
    currency: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var choice by rememberSaveable(selectedId) { mutableStateOf(selectedId) }

    AuthScaffold {
        BudgetTopBar(stringResource(R.string.budget_pick_category_title), onBack = onBack, centered = true)
        Spacer(Modifier.height(16.dp))

        when {
            !loaded -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
            categories.isEmpty() -> AllTaken()
            else -> {
                BudgetSectionLabel(stringResource(R.string.budget_your_categories))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.forEach { category ->
                        CategoryOption(
                            category = category,
                            spent = monthSpent[category.id] ?: 0.0,
                            currency = currency,
                            selected = choice == category.id,
                            onSelect = { choice = category.id },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.budget_save),
            onClick = { choice?.let(onConfirm) },
            enabled = choice != null && categories.any { it.id == choice },
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Tarjeta de una categoría con el botón de selección a la derecha. */
@Composable
private fun CategoryOption(
    category: MovementCategory,
    spent: Double,
    currency: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pocketColors.Card)
            .border(if (selected) 2.dp else 1.dp, if (selected) pocketColors.Coral else pocketColors.RoseBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = pocketColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val limit = category.monthlyLimit
            Text(
                if (limit != null) {
                    spentOfLimit(spent, limit, currency)
                } else {
                    stringResource(R.string.budget_spent_this_month, MoneyFormat.format(spent, currency))
                },
                fontSize = 11.sp,
                color = pocketColors.Muted,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = pocketColors.Coral, unselectedColor = pocketColors.Coral),
        )
    }
}

/** Figma "ELEGIR CAT - TODAS": ya no quedan categorías sin presupuesto. */
@Composable
private fun AllTaken() {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(pocketColors.ChipPink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CreateNewFolder, null, tint = pocketColors.Coral, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.budget_all_categories_taken),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.budget_all_categories_taken_hint),
            fontSize = 12.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}
