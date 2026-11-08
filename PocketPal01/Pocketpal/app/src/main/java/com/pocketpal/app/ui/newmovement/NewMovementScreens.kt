package com.pocketpal.app.ui.newmovement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.dai.DaiAvatar
import com.pocketpal.app.ui.dai.DaiMood
import com.pocketpal.app.ui.goals.GoalDateField
import com.pocketpal.app.ui.goals.GoalFieldLabel
import com.pocketpal.app.ui.goals.MoneyField
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.movements.CategoryBadge
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/** Las 3 formas de registrar que salen al tocar el + del inicio. */
enum class NewMovementOption(val label: Int, val icon: ImageVector) {
    Scan(R.string.reg_option_scan, Icons.Outlined.CameraAlt),
    Voice(R.string.reg_option_voice, Icons.Outlined.Mic),
    Manual(R.string.reg_option_manual, Icons.Outlined.Edit),
}

/**
 * Figma "INICIO MAS MENÚ": la hoja azul que sale al tocar el **+** de la barra de abajo,
 * con "Registro mediante escáner", "Registro por voz" y "Registro manual".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAddSheet(onDismiss: () -> Unit, onSelect: (NewMovementOption) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = pocketColors.Navy,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.reg_add_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            NewMovementOption.entries.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(option) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(pocketColors.ChipPink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(option.icon, null, tint = pocketColors.Coral, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(option.label), fontSize = 15.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Figma "REG MANUAL" (formulario vacío) y "CONFI DATOS" (lo que Dai entendió por voz,
 * listo para corregir). Es una pantalla **sin estado**: el formulario vive en
 * [NewMovementViewModel] y cada cambio sube con una lambda. Lo único que recuerda ella misma
 * es si están abiertas la hoja de categorías o la pregunta de descartar.
 */
@Composable
fun NewMovementFormScreen(
    state: NewMovementViewModel.NewMovementUi,
    currency: String,
    datePreference: String,
    onExit: () -> Unit,
    onKind: (MovementKind) -> Unit,
    onAmount: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onCategory: (String?) -> Unit,
    onReason: (String) -> Unit,
    onSave: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val form = state.form
    val today = LocalDate.now()
    var showCategories by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            shownError = state.error
            onErrorShown()
        }
    }
    // Atrás con cambios sin guardar → "¿Quieres descartar los cambios?" (Figma "REG MANUAL E").
    fun tryExit() {
        if (state.hasChanges && !state.saving) askDiscard = true else onExit()
    }
    BackHandler(onBack = ::tryExit)

    AuthScaffold(onBack = ::tryExit, backBackground = pocketColors.ChipPink, backTint = pocketColors.Coral) {
        if (state.mode == NewMovementViewModel.Mode.Voice) {
            Text(stringResource(R.string.reg_confirm_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pocketColors.Ink)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DaiAvatar(DaiMood.Happy, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.reg_voice_heard, state.heard),
                    fontSize = 12.sp,
                    color = pocketColors.Muted,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Elige el tipo de registro que harás: Gasto / Ingreso ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(pocketColors.ChipPink)
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text(stringResource(R.string.reg_kind_title), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Coral, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KindChip(stringResource(R.string.reg_kind_expense), form.kind == MovementKind.Expense) { onKind(MovementKind.Expense) }
                KindChip(stringResource(R.string.reg_kind_income), form.kind == MovementKind.Income) { onKind(MovementKind.Income) }
            }
        }

        GoalFieldLabel(stringResource(R.string.reg_amount))
        MoneyField(
            digits = form.amountDigits,
            onDigitsChange = onAmount,
            currency = currency,
            isError = state.problem == NewMovementViewModel.Problem.Amount,
        )

        GoalFieldLabel(stringResource(R.string.reg_date))
        GoalDateField(
            date = form.date,
            onDateChange = onDate,
            datePreference = datePreference,
            isSelectable = { !it.isAfter(today) },
        )

        Spacer(Modifier.height(16.dp))
        // ---- Categoría ⌄  Seleccionar → hoja "CATEGORÍAS" ----
        FieldRow(
            label = stringResource(R.string.reg_category),
            value = state.selectedCategory?.name ?: stringResource(R.string.reg_category_select),
            hasValue = state.selectedCategory != null,
            isError = state.problem == NewMovementViewModel.Problem.Category,
            onClick = { showCategories = true },
        )

        GoalFieldLabel(stringResource(R.string.reg_reason))
        PocketOutlinedTextField(
            value = form.reason,
            onValueChange = onReason,
            label = "",
            placeholder = stringResource(R.string.reg_reason_hint),
            shape = RoundedCornerShape(16.dp),
        )

        state.problem?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(problemText(it)))
        }
        shownError?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(stringResource(it.messageRes()))
        }

        Spacer(Modifier.height(28.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.reg_save),
            onClick = {
                shownError = null
                onSave()
            },
            loading = state.saving,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showCategories) {
        CategorySheet(
            categories = state.kindCategories,
            selectedId = form.categoryId,
            onSelect = {
                showCategories = false
                onCategory(it)
            },
            onDismiss = { showCategories = false },
        )
    }
    if (askDiscard) {
        DiscardDialog(
            onCancel = { askDiscard = false },
            onDiscard = {
                askDiscard = false
                onExit()
            },
        )
    }
}

@Composable
private fun KindChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) pocketColors.Coral else Color.White)
            .border(1.dp, pocketColors.Coral, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else pocketColors.Coral)
    }
}

/** Fila "Categoría ⌄ … Seleccionar" del Figma. */
@Composable
private fun FieldRow(label: String, value: String, hasValue: Boolean, isError: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, if (isError) pocketColors.ErrorRed else pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
        Icon(Icons.Outlined.KeyboardArrowDown, null, tint = pocketColors.Coral, modifier = Modifier.size(18.dp))
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = if (hasValue) pocketColors.Ink else pocketColors.Muted)
    }
}

/** Figma "CONFI CAT" / "REG MANUAL C": la hoja azul "CATEGORÍAS" para elegir una. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    categories: List<MovementCategory>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = pocketColors.Navy,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.reg_categories_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
            Text(stringResource(R.string.reg_categories_subtitle), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(14.dp))
            if (categories.isEmpty()) {
                Text(
                    stringResource(R.string.reg_categories_empty),
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            categories.forEach { category ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(category.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(category, size = 34.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(category.name, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f))
                    if (category.id == selectedId) {
                        Icon(Icons.Outlined.Check, null, tint = pocketColors.Coral)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Figma "REG MANUAL E": "¿Quieres descartar los cambios?" · Cancelar / Descartar. */
@Composable
private fun DiscardDialog(onCancel: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(pocketColors.Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.WarningAmber, null, tint = pocketColors.Navy, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.reg_discard_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.reg_discard_body), fontSize = 12.sp, color = pocketColors.Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PocketPrimaryButton(
                    text = stringResource(R.string.reg_discard_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
                PocketPrimaryButton(
                    text = stringResource(R.string.reg_discard_confirm),
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.White,
                    contentColor = pocketColors.Coral,
                    border = BorderStroke(1.dp, pocketColors.Coral),
                )
            }
        }
    }
}

/** Figma "REGISTRO EXITO": "Movimiento registrado correctamente" · Continuar / Ir a inicio. */
@Composable
fun NewMovementSuccessScreen(onContinue: () -> Unit, onHome: () -> Unit) {
    BackHandler(onBack = onHome)
    Column(
        Modifier.fillMaxSize().background(pocketColors.Cream).systemBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(pocketColors.TealSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = pocketColors.Teal, modifier = Modifier.size(96.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.reg_success_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.4f))
        PocketPrimaryButton(text = stringResource(R.string.reg_success_continue), onClick = onContinue)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.reg_success_home),
            fontSize = 13.sp,
            color = pocketColors.Muted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onHome).padding(8.dp),
        )
        Spacer(Modifier.height(32.dp))
    }
}

private fun problemText(problem: NewMovementViewModel.Problem): Int = when (problem) {
    NewMovementViewModel.Problem.Amount -> R.string.reg_problem_amount
    NewMovementViewModel.Problem.Category -> R.string.reg_problem_category
    NewMovementViewModel.Problem.FutureDate -> R.string.reg_problem_date
}