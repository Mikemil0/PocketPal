package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pocketpal.app.data.budget.BudgetDraft
import com.pocketpal.app.data.budget.BudgetPeriod
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.GoalFieldLabel
import com.pocketpal.app.ui.goals.MoneyField
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.movements.CategoryBadge
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Flujo 2 · "Nuevo presupuesto" (Figma "NUEVO PRE" y "NUEVO PRE ERROR").
 *
 * Arriba la categoría elegida con "Cambiar categoría" (abre "Elegir categoría"), luego
 * NOMBRE, MONTO LÍMITE, PERÍODO (Mensual / Semanal), "Avísame al acercarme al límite" y
 * Guardar. Si la categoría ya tiene presupuesto se ve el aviso rojo del Figma y Guardar
 * queda desactivado.
 *
 * Al elegir una categoría se proponen su nombre y, si en Mi perfil → Categorías tenía
 * un límite mensual (Módulo 2), ese monto.
 */
@Composable
fun BudgetFormScreen(
    category: MovementCategory?,
    currency: String,
    isDuplicate: Boolean,
    saving: Boolean,
    error: ErrorType?,
    onBack: () -> Unit,
    onChangeCategory: () -> Unit,
    onSave: (BudgetDraft) -> Unit,
    onErrorShown: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amountDigits by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(BudgetPeriod.Monthly) }
    var alert by rememberSaveable { mutableStateOf(false) }
    // Nombre propuesto por la última categoría (para cambiarlo solo si el usuario no lo editó).
    var suggestedName by rememberSaveable { mutableStateOf("") }
    var suggestedAmount by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(category?.id) {
        if (category != null) {
            if (name.isBlank() || name == suggestedName) name = category.name
            suggestedName = category.name
            val limit = category.monthlyLimit?.toLong()?.toString().orEmpty()
            if (amountDigits.isEmpty() || amountDigits == suggestedAmount) amountDigits = limit
            suggestedAmount = limit
        }
    }

    // Error de validación o de red: se queda visible hasta que el usuario cambie algo.
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
        }
    }
    val duplicate = isDuplicate || shownError == ErrorType.DuplicateBudget

    AuthScaffold {
        BudgetTopBar(stringResource(R.string.budget_new_title), onBack = onBack, centered = true)
        Spacer(Modifier.height(20.dp))

        CategoryHeader(category = category, onChangeCategory = onChangeCategory)

        GoalFieldLabel(stringResource(R.string.budget_name_label))
        PocketOutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40); shownError = null },
            label = "",
            placeholder = stringResource(R.string.budget_name_hint),
            borderColor = if (duplicate) pocketColors.ErrorRed else pocketColors.RoseBorder,
            shape = RoundedCornerShape(16.dp),
        )
        if (duplicate) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(stringResource(R.string.error_duplicate_budget))
        }

        GoalFieldLabel(stringResource(R.string.budget_amount_label))
        MoneyField(
            digits = amountDigits,
            onDigitsChange = { amountDigits = it; shownError = null },
            currency = currency,
        )

        GoalFieldLabel(stringResource(R.string.budget_period_label))
        PeriodSelector(selected = period, onSelect = { period = it })

        Spacer(Modifier.height(20.dp))
        AlertToggle(checked = alert, onCheckedChange = { alert = it })

        val otherError = shownError?.takeIf { it != ErrorType.DuplicateBudget }
        if (otherError != null) {
            Spacer(Modifier.height(16.dp))
            ErrorBanner(stringResource(otherError.messageRes()))
        }

        Spacer(Modifier.height(32.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.budget_save),
            onClick = {
                onSave(
                    BudgetDraft(
                        categoryId = category?.id,
                        name = name.trim(),
                        amountLimit = amountDigits.toDoubleOrNull() ?: 0.0,
                        period = period,
                        alertEnabled = alert,
                    )
                )
            },
            enabled = category != null && name.isNotBlank() && amountDigits.isNotEmpty() && !duplicate,
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Ícono grande de la categoría y el enlace coral "Cambiar categoría" (o "Elegir categoría"). */
@Composable
internal fun CategoryHeader(category: MovementCategory?, onChangeCategory: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CategoryBadge(category, size = 56.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (category != null) R.string.budget_change_category else R.string.budget_pick_category),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Coral,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onChangeCategory).padding(6.dp),
        )
    }
}

/** Botones "Mensual" / "Semanal": el elegido va relleno de rosado con borde coral. */
@Composable
internal fun PeriodSelector(selected: BudgetPeriod, onSelect: (BudgetPeriod) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
        BudgetPeriod.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .width(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) pocketColors.ChipPink else pocketColors.Card)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) pocketColors.Coral else pocketColors.RoseBorder, RoundedCornerShape(10.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(
                        when (option) {
                            BudgetPeriod.Monthly -> R.string.budget_period_monthly
                            BudgetPeriod.Weekly -> R.string.budget_period_weekly
                        }
                    ),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) pocketColors.Coral else pocketColors.Ink,
                )
            }
        }
    }
}

/** Fila con la campana y el interruptor "Avísame al acercarme al límite". */
@Composable
internal fun AlertToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.NotificationsNone, null, tint = pocketColors.Ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.budget_alert_toggle),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = pocketColors.Coral,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = pocketColors.ToggleOff,
                uncheckedBorderColor = pocketColors.ToggleOff,
            ),
        )
    }
}
