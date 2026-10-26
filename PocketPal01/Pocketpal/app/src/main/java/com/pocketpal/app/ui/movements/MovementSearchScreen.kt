package com.pocketpal.app.ui.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.movement.MovementKind
import com.pocketpal.app.data.movement.MovementRules
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.goals.currentLocale
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import java.time.ZoneId

/**
 * Flujo 2 · "Buscar movimientos" (Figma "01_Movimientos por busqueda"): buscador con
 * la lupa y la "x", chips Todos / Gastos / Ingresos, "3 resultados para 'mercado'" y
 * las filas (fecha · categoría y monto). Tocar una fila abre el detalle (flujo 4).
 */
@Composable
fun MovementSearchScreen(
    state: MovementSearchViewModel.SearchUi,
    currency: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSelectKind: (MovementKind?) -> Unit,
    onRetry: () -> Unit,
    onOpenMovement: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Al abrir la pantalla el cursor ya queda en el buscador.
    LaunchedEffect(Unit) { focus.requestFocus() }

    MovementsScaffold(title = stringResource(R.string.movements_title), onBack = onBack) {
        Text(
            stringResource(R.string.search_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = { if (it.length <= MovementRules.MAX_QUERY_LENGTH) onQueryChange(it) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint), color = pocketColors.Muted) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = pocketColors.Coral) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.a11y_clear_search), tint = pocketColors.Muted)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = pocketColors.Coral,
                unfocusedBorderColor = pocketColors.RoseBorder,
                focusedContainerColor = pocketColors.Card,
                unfocusedContainerColor = pocketColors.Card,
                cursorColor = pocketColors.Coral,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(stringResource(R.string.search_all), state.kind == null, { onSelectKind(null) })
            PillChip(stringResource(R.string.search_expenses), state.kind == MovementKind.Expense, { onSelectKind(MovementKind.Expense) })
            PillChip(stringResource(R.string.search_incomes), state.kind == MovementKind.Income, { onSelectKind(MovementKind.Income) })
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.error != null -> ErrorBanner(
                stringResource(state.error.messageRes()),
                actionText = stringResource(R.string.movements_retry),
                onAction = onRetry,
            )

            state.searching -> Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }

            !state.canSearch -> CenteredHint(stringResource(R.string.search_min_chars))

            state.searched && state.results.isEmpty() ->
                CenteredHint(stringResource(R.string.search_no_results, state.query.trim()))

            state.searched -> Results(state, currency, onOpenMovement)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Results(
    state: MovementSearchViewModel.SearchUi,
    currency: String,
    onOpenMovement: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val locale = currentLocale()
    Text(
        pluralStringResource(R.plurals.search_results, state.results.size, state.results.size, state.query.trim()),
        fontSize = 12.sp,
        color = pocketColors.Muted,
    )
    Spacer(Modifier.height(8.dp))
    state.results.forEachIndexed { index, movement ->
        val category = movement.categoryId?.let { state.categories[it] }
        val date = movement.localDate(zone)
        val subtitle = listOfNotNull(
            date?.let { MovementFormat.shortDate(it, locale) },
            categoryLabel(movement, category),
        ).joinToString(" · ")
        MovementRow(
            movement = movement,
            category = category,
            subtitle = subtitle,
            currency = currency,
            onClick = { onOpenMovement(movement.id) },
        )
        if (index < state.results.lastIndex) HorizontalDivider(color = pocketColors.ChipPink)
    }
}

@Composable
private fun CenteredHint(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = pocketColors.Muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
    )
}
