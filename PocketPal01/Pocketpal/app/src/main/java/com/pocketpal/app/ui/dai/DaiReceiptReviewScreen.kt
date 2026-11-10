package com.pocketpal.app.ui.dai


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.dai.ReceiptDraft
import com.pocketpal.app.data.dai.ReceiptProblem
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.ConfirmSheet
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.goals.GoalFormat
import com.pocketpal.app.ui.theme.PocketColors
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Flujo 3 · "Confirmar": el usuario revisa (y corrige) lo que leyó el OCR.
 * Nada se guarda hasta que toca Guardar y confirma "¿Dónde se guardará?".
 */
@Composable
fun DaiReceiptReviewScreen(
    state: DaiReceiptViewModel.UiState,
    currency: String,
    datePreference: String,
    onBack: () -> Unit,
    onChange: (ReceiptDraft) -> Unit,
    onSave: () -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirm: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val draft = state.draft
    val locale = Locale.getDefault()

    // Texto de los campos: se editan como texto y se convierten al borrador al escribir.
    var amountText by remember {
        mutableStateOf(draft.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var dateText by remember { mutableStateOf(GoalFormat.inputDate(draft.date, datePreference)) }
    var dateInvalid by remember { mutableStateOf(false) }
    var dateComplete by remember { mutableStateOf(true) } // 8 dígitos escritos (el error solo se muestra entonces)
    var showDatePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(pocketColors.Cream).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.dai_receipt_back), tint = pocketColors.Ink)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.dai_receipt_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
            )

            PocketOutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    amountText = clean
                    onChange(draft.copy(amount = clean.toDoubleOrNull()))
                },
                label = stringResource(R.string.dai_receipt_amount),
                keyboardType = KeyboardType.Decimal,
                isError = state.problem == ReceiptProblem.Amount,
                errorText = if (state.problem == ReceiptProblem.Amount) stringResource(R.string.dai_receipt_err_amount) else null,
            )

            val futureDate = state.problem == ReceiptProblem.FutureDate
            // El teclado numérico no tiene "/": se escriben solo los dígitos (12052026) y las
            // barras se ponen solas. El error solo aparece cuando la fecha está completa.
            val showDateError = dateInvalid && dateComplete
            PocketOutlinedTextField(
                value = dateText,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(8)
                    dateText = formatDateDigits(digits, datePreference)
                    dateComplete = digits.length == 8
                    val parsed = if (dateComplete) parseDate(dateText, datePreference) else null
                    dateInvalid = parsed == null
                    if (parsed != null) onChange(draft.copy(date = parsed))
                },
                label = stringResource(R.string.dai_receipt_date),
                placeholder = GoalFormat.placeholder(datePreference, locale),
                keyboardType = KeyboardType.Number,
                trailingIcon = Icons.Outlined.CalendarMonth,
                onTrailingIconClick = { showDatePicker = true },
                trailingIconDescription = stringResource(R.string.a11y_pick_date),
                isError = showDateError || futureDate,
                errorText = when {
                    futureDate -> stringResource(R.string.dai_receipt_err_future)
                    showDateError -> stringResource(R.string.dai_receipt_err_date)
                    else -> null
                },
            )

            if (state.categories.isNotEmpty()) {
                Text(
                    stringResource(R.string.dai_receipt_category),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = pocketColors.Ink,
                )
                CategoryChips(
                    categories = state.categories.map { it.id to it.name },
                    selectedId = draft.categoryId,
                    onSelect = { id -> onChange(draft.copy(categoryId = if (id == draft.categoryId) null else id)) },
                )
            }

            PocketOutlinedTextField(
                value = draft.reason,
                onValueChange = { onChange(draft.copy(reason = it)) },
                label = stringResource(R.string.dai_receipt_reason),
                imeAction = ImeAction.Done,
            )
        }

        PocketPrimaryButton(
            text = stringResource(R.string.dai_receipt_save),
            onClick = onSave,
            enabled = !dateInvalid,
            loading = state.saving,
            modifier = Modifier.padding(24.dp),
        )
    }

    if (showDatePicker) {
        ReceiptDatePickerDialog(
            current = draft.date,
            onPick = { picked ->
                dateText = GoalFormat.inputDate(picked, datePreference)
                dateInvalid = false
                dateComplete = true
                onChange(draft.copy(date = picked))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (state.confirming && state.impact != null) {
        val impact = state.impact
        val category = impact.categoryName ?: stringResource(R.string.dai_receipt_no_category)
        val body = buildString {
            append(stringResource(R.string.dai_receipt_confirm_where, MoneyFormat.format(draft.amount, currency), category))
            if (impact.budgetName != null && impact.budgetPercentBefore != null && impact.budgetPercentAfter != null) {
                append("\n")
                append(
                    stringResource(
                        R.string.dai_receipt_confirm_budget,
                        impact.budgetName,
                        impact.budgetPercentBefore,
                        impact.budgetPercentAfter,
                    ),
                )
                if (impact.exceedsBudget) {
                    append("\n")
                    append(stringResource(R.string.dai_receipt_confirm_exceeds))
                }
            }
        }
        ConfirmSheet(
            icon = Icons.Outlined.CheckCircle,
            title = stringResource(R.string.dai_receipt_confirm_title),
            body = body,
            confirmText = stringResource(R.string.dai_receipt_confirm_yes),
            cancelText = stringResource(R.string.dai_receipt_confirm_no),
            onConfirm = onConfirm,
            onDismiss = onCancelConfirm,
        )
    }

    if (state.error != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onErrorShown,
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onErrorShown) {
                    Text(stringResource(R.string.dai_receipt_error_ok))
                }
            },
            title = { Text(stringResource(R.string.dai_receipt_error_title)) },
            text = { Text(stringResource(R.string.dai_receipt_error_body)) },
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories.size) { index ->
            val (id, name) = categories[index]
            val selected = id == selectedId
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) PocketColors.Coral else PocketColors.ChipPink,
                border = BorderStroke(1.dp, if (selected) PocketColors.Coral else PocketColors.RoseBorder),
                modifier = Modifier.clickable { onSelect(id) },
            ) {
                Text(
                    name,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (selected) androidx.compose.ui.graphics.Color.White else PocketColors.Ink,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/** Convierte el texto del campo a fecha según la preferencia del usuario (`null` = inválida). */
private fun parseDate(text: String, preference: String): LocalDate? {
    val pattern = when (preference) {
        "MM/DD/YYYY" -> "MM/dd/yyyy"
        "YYYY-MM-DD" -> "yyyy-MM-dd"
        else -> "dd/MM/yyyy"
    }
    return try {
        LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(pattern))
    } catch (e: Exception) {
        null
    }
}

/** Calendario para elegir la fecha del recibo (no deja escoger días futuros). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptDatePickerDialog(current: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = GoalFormat.toPickerMillis(current),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !GoalFormat.fromPickerMillis(utcTimeMillis).isAfter(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(GoalFormat.fromPickerMillis(it)) } ?: onDismiss()
            }) {
                Text(stringResource(R.string.goal_date_ok), color = pocketColors.Coral, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = pocketColors.Muted)
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** "12052026" → "12/05/2026" (o "2026-05-12" según la preferencia); pone el separador solo cuando hay más dígitos. */
private fun formatDateDigits(digits: String, preference: String): String {
    val sep = if (preference == "YYYY-MM-DD") "-" else "/"
    val cuts = if (preference == "YYYY-MM-DD") listOf(4, 6) else listOf(2, 4)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i in cuts) append(sep)
            append(c)
        }
    }
}