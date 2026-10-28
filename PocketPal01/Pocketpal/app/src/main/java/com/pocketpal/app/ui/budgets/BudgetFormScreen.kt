package com.pocketpal.app.ui.budgets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.budget.Budget
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
 *
 * Flujo 4 · Si llega [editing], el mismo formulario es "Editar presupuesto" (Figma
 * "EDITAR PRE"): viene prellenado y abajo aparece "Eliminar presupuesto", que pide
 * confirmación (Figma "ELIMINAR PRE").
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
    editing: Budget? = null,
    deleting: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    // Vacíos al crear; los del presupuesto al editar (se rellenan cuando llega de la base).
    var name by rememberSaveable(editing?.id) { mutableStateOf(editing?.name.orEmpty()) }
    var amountDigits by rememberSaveable(editing?.id) { mutableStateOf(editing?.amountLimit?.toLong()?.toString().orEmpty()) }
    var period by rememberSaveable(editing?.id) { mutableStateOf(editing?.budgetPeriod ?: BudgetPeriod.Monthly) }
    var alert by rememberSaveable(editing?.id) { mutableStateOf(editing?.alertEnabled ?: false) }
    var showDelete by remember { mutableStateOf(false) }
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
        BudgetTopBar(
            stringResource(if (editing != null) R.string.budget_edit_title else R.string.budget_new_title),
            onBack = onBack,
            centered = true,
        )
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

        if (editing != null && onDelete != null) {
            Spacer(Modifier.height(16.dp))
            DeleteBudgetButton(onClick = { showDelete = true }, enabled = !saving && !deleting)
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
            enabled = category != null && name.isNotBlank() && amountDigits.isNotEmpty() && !duplicate && !deleting,
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showDelete && editing != null && onDelete != null) {
        DeleteBudgetDialog(
            name = editing.name,
            deleting = deleting,
            onConfirm = {
                showDelete = false
                onDelete()
            },
            onDismiss = { showDelete = false },
        )
    }
}

/** Ícono grande de la categoría y el enlace coral "Cambiar categoría" (o "Elegir categoría"). */
@Composable
internal fun CategoryHeader(category: MovementCategory?, onChangeCategory: () -> Unit) {
    Column(
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

/** Botón de borde punteado rojo "Eliminar presupuesto" (Figma "EDITAR PRE"). */
@Composable
private fun DeleteBudgetButton(onClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = DELETE_BORDER,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.DeleteOutline, null, tint = pocketColors.Coral, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.budget_delete), fontSize = 13.sp, color = pocketColors.Coral)
    }
}

/**
 * "¿Eliminar presupuesto?" (Figma "ELIMINAR PRE"): avisa que se pierde el seguimiento pero
 * que los gastos ya registrados no se tocan.
 */
@Composable
private fun DeleteBudgetDialog(name: String, deleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(pocketColors.Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.DeleteOutline, null, tint = pocketColors.Coral, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.budget_delete_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.budget_delete_body, name),
                fontSize = 13.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PocketPrimaryButton(
                text = stringResource(R.string.budget_delete_confirm),
                onClick = onConfirm,
                loading = deleting,
            )
            Spacer(Modifier.height(10.dp))
            PocketPrimaryButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                containerColor = pocketColors.Card,
                contentColor = pocketColors.Coral,
                border = BorderStroke(1.dp, pocketColors.Coral),
            )
        }
    }
}

/** Rojo suave del borde punteado (igual en claro y oscuro). */
private val DELETE_BORDER = Color(0xFFF4AAAA)
