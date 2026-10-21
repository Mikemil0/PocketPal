package com.pocketpal.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.components.PocketOutlinedTextField
import com.pocketpal.app.ui.components.PocketPrimaryButton
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import java.time.LocalDate

/**
 * Flujo 3 · "Agregar aporte" (Figma "AGREGAR APORTE"): MONTO, FECHA (hoy por defecto,
 * nunca futura) y MOTIVO opcional. Abajo se ve en vivo cómo queda el "Nuevo total
 * ahorrado" antes de guardar.
 */
@Composable
fun AddContributionScreen(
    goal: Goal,
    currency: String,
    datePreference: String,
    saving: Boolean,
    error: ErrorType?,
    onBack: () -> Unit,
    onSave: (amount: Double, date: LocalDate, note: String?) -> Unit,
    onErrorShown: () -> Unit,
) {
    val today = LocalDate.now()
    var digits by rememberSaveable { mutableStateOf("") }
    var dateIso by rememberSaveable { mutableStateOf(today.toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    val amount = digits.toDoubleOrNull() ?: 0.0

    var shownError by remember { mutableStateOf<ErrorType?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            shownError = error
            onErrorShown()
        }
    }

    AuthScaffold(
        onBack = onBack,
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.contribution_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GoalIconBadge(goal.icon, goal.color, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(goal.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
                Text(
                    stringResource(
                        R.string.goals_amount_of,
                        MoneyFormat.format(goal.savedAmount, currency),
                        MoneyFormat.format(goal.targetAmount, currency),
                    ),
                    fontSize = 12.sp,
                    color = pocketColors.Muted,
                )
            }
        }

        GoalFieldLabel(stringResource(R.string.contribution_amount_label))
        MoneyField(
            digits = digits,
            onDigitsChange = { digits = it; shownError = null },
            currency = currency,
        )

        GoalFieldLabel(stringResource(R.string.contribution_date_label))
        GoalDateField(
            date = LocalDate.parse(dateIso),
            onDateChange = { dateIso = it.toString(); shownError = null },
            datePreference = datePreference,
            isSelectable = { !it.isAfter(today) },
        )

        GoalFieldLabel(stringResource(R.string.contribution_note_label))
        PocketOutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= GoalRules.MAX_NOTE_LENGTH) note = it; shownError = null },
            label = "",
            placeholder = stringResource(R.string.contribution_note_hint),
            shape = RoundedCornerShape(16.dp),
        )
        Text(
            "${note.length}/${GoalRules.MAX_NOTE_LENGTH}",
            fontSize = 11.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
        )

        // Vista previa: cómo queda la meta con este aporte.
        val newTotal = goal.savedAmount + amount
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(pocketColors.Card, RoundedCornerShape(16.dp))
                .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.contribution_new_total),
                    fontSize = 13.sp,
                    color = pocketColors.Muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    MoneyFormat.format(newTotal, currency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = pocketColors.Ink,
                )
            }
            Spacer(Modifier.height(10.dp))
            GoalProgressBar(progress = GoalRules.progress(newTotal, goal.targetAmount))
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.goal_percent_saved, GoalRules.percent(newTotal, goal.targetAmount)),
                fontSize = 12.sp,
                color = pocketColors.Coral,
            )
        }

        if (shownError != null) {
            Spacer(Modifier.height(16.dp))
            ErrorBanner(stringResource(shownError!!.messageRes()))
        }

        Spacer(Modifier.height(24.dp))
        PocketPrimaryButton(
            text = stringResource(R.string.contribution_save),
            onClick = { onSave(amount, LocalDate.parse(dateIso), note) },
            enabled = digits.isNotEmpty(),
            loading = saving,
        )
        Spacer(Modifier.height(24.dp))
    }
}
