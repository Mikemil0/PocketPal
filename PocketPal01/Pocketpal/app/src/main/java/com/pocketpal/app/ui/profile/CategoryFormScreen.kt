package com.pocketpal.app.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import kotlinx.coroutines.delay
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.category.CategoryDraft
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.data.category.CategoryRules
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.CategoryColors
import com.pocketpal.app.ui.components.CategoryIcon
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.components.SectionHeader
import com.pocketpal.app.ui.components.ToggleCard
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Crear y editar categoría (§8.9; págs. 54, 55, 67 y 68).
 *
 * - NOMBRE obligatorio de 1 a 30 caracteres (RN-02), con el banner
 *   "Ya tienes una categoría con este nombre" si ya existe en el mismo tipo.
 * - Cuadrícula de 10 íconos (2 × 5) y 5 colores; la elegida lleva anillo (§8.9).
 * - PRESUPUESTO: límite mensual opcional; el monto solo se habilita con el
 *   interruptor activo y debe ser mayor que 0 (RN-03).
 * - En edición aparece "Eliminar categoría" con confirmación (diálogo sin
 *   diseñar en el Figma; propuesto y registrado en §14).
 */
@Composable
fun CategoryFormScreen(
    categoryId: String?,
    requestedKind: CategoryKind,
    initial: CategoriesViewModel.CategoryUi?,
    existing: List<CategoriesViewModel.CategoryUi>,
    currency: String,
    saving: Boolean,
    error: ErrorType?,
    event: CategoriesViewModel.Event?,
    onBack: () -> Unit,
    onSave: (id: String?, CategoryDraft) -> Unit,
    onDelete: (String) -> Unit,
    onErrorShown: () -> Unit,
    onEventConsumed: () -> Unit,
    onDone: () -> Unit,
) {
    val kind = initial?.kind ?: requestedKind

    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var iconId by remember(initial?.id) { mutableStateOf(initial?.icon ?: CategoryIcon.Restaurant.id) }
    var colorHex by remember(initial?.id) { mutableStateOf(initial?.color ?: CategoryColors.default) }
    var limitEnabled by remember(initial?.id) { mutableStateOf(initial?.monthlyLimit != null) }
    var limitDigits by remember(initial?.id) {
        mutableStateOf(initial?.monthlyLimit?.toLong()?.toString().orEmpty())
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
            delay(ERROR_SHOWN_MS)
            shownError = null
        }
    }

    // Guardar/eliminar con éxito: volver a la lista (§7.5).
    LaunchedEffect(event) {
        if (event != null) {
            onEventConsumed()
            onDone()
        }
    }

    if (categoryId != null && initial == null) {
        AuthScaffold(onBack = onBack) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
        }
        return
    }

    val limitValue = if (limitEnabled) limitDigits.toDoubleOrNull() ?: 0.0 else null
    val isDuplicate = name.isNotBlank() &&
        CategoryRules.isDuplicateName(name, existing, excludeId = categoryId)
    val nameTooLong = name.trim().length > CategoryRules.MAX_NAME_LENGTH
    val formValid = CategoryRules.isFormValid(name, limitValue, existing, excludeId = categoryId)

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(
                if (categoryId == null) R.string.category_new_title else R.string.category_edit_title,
            ),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Vista previa del ícono elegido (§8.9), arriba y centrada.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(80.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(CategoryColors.parse(colorHex).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CategoryIcon.fromId(iconId).icon,
                    stringResource(R.string.a11y_category_icon),
                    tint = pocketColors.Coral,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionHeader(stringResource(R.string.category_name_label))
        PocketOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = "",
            placeholder = stringResource(R.string.category_name_hint),
            isError = nameTooLong,
            errorText = if (nameTooLong) stringResource(R.string.error_category_name_too_long) else null,
            borderColor = if (isDuplicate) pocketColors.ErrorRed else null,
        )
        if (isDuplicate) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(stringResource(R.string.error_duplicate_category))
        }

        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryIcon.grid.chunked(5).forEach { rowIcons ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowIcons.forEach { option ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(pocketColors.Card)
                                .border(1.dp, pocketColors.ChipPink, RoundedCornerShape(12.dp))
                                .clickable { iconId = option.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                option.icon,
                                stringResource(R.string.a11y_category_icon_option),
                                tint = pocketColors.Coral,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.category_color_label))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryColors.palette.forEach { hex ->
                val selected = hex.equals(colorHex, ignoreCase = true)
                Box(
                    Modifier.size(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .border(2.dp, pocketColors.Coral, CircleShape),
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CategoryColors.parse(hex))
                            .clickable { colorHex = hex },
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.category_budget_label))
        ToggleCard(
            title = stringResource(R.string.category_limit_toggle),
            subtitle = stringResource(R.string.category_limit_sub),
            checked = limitEnabled,
            onCheckedChange = { limitEnabled = it },
        )
        Spacer(Modifier.height(12.dp))
        PocketOutlinedTextField(
            value = MoneyFormat.formatInput(limitDigits, currency),
            onValueChange = { limitDigits = it.filter(Char::isDigit).take(MAX_LIMIT_DIGITS) },
            label = "",
            enabled = limitEnabled,
            keyboardType = KeyboardType.Number,
        )

        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.category_save),
            onClick = {
                onSave(
                    categoryId,
                    CategoryDraft(
                        kind = kind,
                        name = name.trim(),
                        icon = iconId,
                        color = colorHex,
                        monthlyLimit = limitValue,
                    ),
                )
            },
            enabled = formValid,
            loading = saving,
        )

        if (categoryId != null) {
            Spacer(Modifier.height(12.dp))
            PocketPrimaryButton(
                text = stringResource(R.string.category_delete),
                onClick = { showDeleteDialog = true },
                containerColor = pocketColors.Card,
                contentColor = pocketColors.Coral,
                border = BorderStroke(1.dp, pocketColors.Coral),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = pocketColors.Card,
            title = {
                Text(
                    stringResource(R.string.category_delete_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = pocketColors.Ink,
                )
            },
            text = {
                Text(
                    stringResource(R.string.category_delete_body),
                    fontSize = 13.sp,
                    color = pocketColors.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    categoryId?.let(onDelete)
                }) {
                    Text(
                        stringResource(R.string.category_delete_confirm),
                        color = pocketColors.ErrorRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = pocketColors.Muted)
                }
            },
        )
    }
}

private const val ERROR_SHOWN_MS = 3_500L

/** Límite mensual en unidades enteras (el Figma muestra $600.000, §8.9). */
private const val MAX_LIMIT_DIGITS = 12
