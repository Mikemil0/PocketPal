package com.pocketpal.app.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.GoalDraft
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.CategoryIcon
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.ToggleCard
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/**
 * Flujo 2 · "Crear nueva meta" (Figma "NEUVA META" y "NEUVA META DESCARTE").
 *
 * NOMBRE, MONTO, YA TENGO AHORRADO (opcional), ícono, color (5) y FECHA LÍMITE
 * opcional con calendario. Si el usuario sale con cambios sin guardar aparece
 * "¿Quieres descartar los cambios?".
 */
@Composable
fun GoalFormScreen(
    currency: String,
    datePreference: String,
    isDuplicateName: (String) -> Boolean,
    saving: Boolean,
    error: ErrorType?,
    onBack: () -> Unit,
    onSave: (GoalDraft) -> Unit,
    onErrorShown: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var iconId by rememberSaveable { mutableStateOf(CategoryIcon.Savings.id) }
    var colorHex by rememberSaveable { mutableStateOf(CategoryColors.default) }
    var targetDigits by rememberSaveable { mutableStateOf("") }
    var savedDigits by rememberSaveable { mutableStateOf("") }
    var deadlineEnabled by rememberSaveable { mutableStateOf(false) }
    var deadlineIso by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscard by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val deadline = deadlineIso?.let(LocalDate::parse)
    val dirty = name.isNotBlank() || targetDigits.isNotEmpty() || savedDigits.isNotEmpty() || deadlineEnabled ||
        iconId != CategoryIcon.Savings.id || colorHex != CategoryColors.default

    // Error de validación o de red: se queda visible hasta que el usuario cambie algo.
    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
        }
    }

    fun requestBack() {
        if (dirty && !saving) showDiscard = true else onBack()
    }
    BackHandler(onBack = ::requestBack)

    val isDuplicate = isDuplicateName(name)

    AuthScaffold(
        onBack = ::requestBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.goal_new_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        // Vista previa del ícono elegido, arriba y centrada.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.shadow(6.dp, RoundedCornerShape(16.dp))) {
                GoalIconBadge(iconId, colorHex, size = 72.dp)
            }
        }

        GoalFieldLabel(stringResource(R.string.goal_name_label))
        PocketOutlinedTextField(
            value = name,
            onValueChange = { name = it; shownError = null },
            label = "",
            placeholder = stringResource(R.string.goal_name_hint),
            borderColor = if (isDuplicate) pocketColors.ErrorRed else pocketColors.RoseBorder,
            shape = RoundedCornerShape(16.dp),
        )
        if (isDuplicate) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(stringResource(R.string.error_duplicate_goal))
        }

        GoalFieldLabel(stringResource(R.string.goal_amount_label))
        MoneyField(
            digits = targetDigits,
            onDigitsChange = { targetDigits = it; shownError = null },
            currency = currency,
        )

        GoalFieldLabel(stringResource(R.string.goal_initial_saved_label))
        MoneyField(
            digits = savedDigits,
            onDigitsChange = { savedDigits = it; shownError = null },
            currency = currency,
        )

        Spacer(Modifier.height(20.dp))
        IconGrid(selected = iconId, onSelect = { iconId = it })

        GoalFieldLabel(stringResource(R.string.goal_color_label))
        ColorRow(selected = colorHex, onSelect = { colorHex = it })

        GoalFieldLabel(stringResource(R.string.goal_deadline_section))
        ToggleCard(
            title = stringResource(R.string.goal_deadline_toggle),
            subtitle = stringResource(R.string.goal_deadline_toggle_sub),
            checked = deadlineEnabled,
            onCheckedChange = { enabled ->
                deadlineEnabled = enabled
                // Al encenderlo se propone una fecha (3 meses) que el usuario puede cambiar.
                if (enabled && deadlineIso == null) deadlineIso = today.plusMonths(3).toString()
                shownError = null
            },
        )
        if (deadlineEnabled) {
            Spacer(Modifier.height(12.dp))
            GoalDateField(
                date = deadline,
                onDateChange = { deadlineIso = it.toString(); shownError = null },
                datePreference = datePreference,
                isSelectable = { it.isAfter(today) },
            )
        }

        if (shownError != null) {
            Spacer(Modifier.height(16.dp))
            ErrorBanner(stringResource(shownError!!.messageRes()))
        }

        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.goal_save),
            onClick = {
                onSave(
                    GoalDraft(
                        name = name.trim(),
                        icon = iconId,
                        color = colorHex,
                        targetAmount = targetDigits.toDoubleOrNull() ?: 0.0,
                        initialSaved = savedDigits.toDoubleOrNull() ?: 0.0,
                        deadline = if (deadlineEnabled) deadline else null,
                    )
                )
            },
            enabled = name.isNotBlank() && targetDigits.isNotEmpty() && !isDuplicate,
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showDiscard) {
        DiscardChangesDialog(
            onKeepEditing = { showDiscard = false },
            onDiscard = {
                showDiscard = false
                onBack()
            },
        )
    }
}

/** Cuadrícula de íconos (filas de 5), los mismos de las categorías. */
@Composable
internal fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryIcon.grid.chunked(5).forEach { rowIcons ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowIcons.forEach { option ->
                    val isSelected = option.id == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) pocketColors.ChipPink else pocketColors.Card)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) pocketColors.Coral else pocketColors.ChipPink,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(option.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            option.icon,
                            stringResource(R.string.a11y_goal_icon),
                            tint = pocketColors.Coral,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                // La última fila puede quedar incompleta: se rellena para que no se estiren.
                repeat(5 - rowIcons.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Los 5 colores; el elegido lleva un anillo coral. */
@Composable
internal fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryColors.palette.forEach { hex ->
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                if (hex.equals(selected, ignoreCase = true)) {
                    Box(Modifier.size(52.dp).border(2.dp, pocketColors.Coral, CircleShape))
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CategoryColors.parse(hex))
                        .clickable { onSelect(hex) },
                )
            }
        }
    }
}

/** "¿Quieres descartar los cambios?" (Figma "NEUVA META DESCARTE"). */
@Composable
internal fun DiscardChangesDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onKeepEditing) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(pocketColors.Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.WarningAmber, null, tint = pocketColors.Ink, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.goal_discard_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.goal_discard_body),
                fontSize = 13.sp,
                color = pocketColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                PocketPrimaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onKeepEditing,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                PocketPrimaryButton(
                    text = stringResource(R.string.goal_discard_confirm),
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    containerColor = pocketColors.Card,
                    contentColor = pocketColors.Coral,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pocketColors.Coral),
                )
            }
        }
    }
}
