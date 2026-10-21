package com.pocketpal.app.ui.goals

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.goal.GoalRules
import com.pocketpal.app.data.goal.GoalSort
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.components.AuthScaffold
import com.pocketpal.app.ui.components.ErrorBanner
import com.pocketpal.app.ui.messageRes
import com.pocketpal.app.ui.theme.pocketColors
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Flujo 1 · Lista de metas (Figma "METAS LIST"): total ahorrado, pestañas
 * Activas/Completadas, filtro de orden, filas con asa para reordenar y el botón
 * "+ Crear nueva meta".
 */
@Composable
fun GoalsScreen(
    state: GoalsViewModel.UiState,
    error: ErrorType?,
    currency: String,
    onBack: () -> Unit,
    onSelectTab: (GoalsViewModel.Tab) -> Unit,
    onSelectSort: (GoalSort) -> Unit,
    onMove: (fromId: String, toId: String) -> Unit,
    onCreate: () -> Unit,
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
        backBackground = pocketColors.ChipPink,
        backTint = pocketColors.Coral,
    ) {
        Text(
            stringResource(R.string.goals_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        if (shownError != null) {
            ErrorBanner(stringResource(shownError!!.messageRes()))
            Spacer(Modifier.height(12.dp))
        }

        when (state) {
            is GoalsViewModel.UiState.Loading -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = pocketColors.Coral) }

            is GoalsViewModel.UiState.Error -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) { ErrorBanner(stringResource(state.type.messageRes())) }

            is GoalsViewModel.UiState.Content -> {
                val data = state.data
                TotalSavedCard(MoneyFormat.format(data.totalSaved, currency))
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.goals_header, data.goals.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Muted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                )
                TabsRow(
                    tab = data.tab,
                    sort = data.sort,
                    onSelectTab = onSelectTab,
                    onSelectSort = onSelectSort,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = pocketColors.RoseBorder)
                Spacer(Modifier.height(12.dp))

                val list = data.current
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
                        item(key = "empty") { EmptyGoals(data.tab) }
                    }
                    items(list, key = { it.id }) { goal ->
                        if (data.canReorder) {
                            ReorderableItem(reorderableState, key = goal.id) {
                                GoalRow(
                                    goal = goal,
                                    currency = currency,
                                    dragHandle = {
                                        Icon(
                                            Icons.Outlined.UnfoldMore,
                                            stringResource(R.string.a11y_reorder_goal),
                                            tint = pocketColors.Coral,
                                            modifier = Modifier.draggableHandle().size(20.dp),
                                        )
                                    },
                                    onClick = { onOpen(goal.id) },
                                )
                            }
                        } else {
                            GoalRow(goal = goal, currency = currency, dragHandle = null, onClick = { onOpen(goal.id) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                DashedActionButton(stringResource(R.string.goals_create), onClick = onCreate)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Tarjeta rosada "Total ahorrado en todas tus metas". */
@Composable
private fun TotalSavedCard(amount: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.ChipPink)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Savings, null, tint = pocketColors.Coral, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(R.string.goals_total_label), fontSize = 12.sp, color = pocketColors.Coral)
            Spacer(Modifier.height(2.dp))
            Text(amount, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pocketColors.Coral)
        }
    }
}

/** Ícono de filtro + pestañas "Activas" / "Completadas". */
@Composable
private fun TabsRow(
    tab: GoalsViewModel.Tab,
    sort: GoalSort,
    onSelectTab: (GoalsViewModel.Tab) -> Unit,
    onSelectSort: (GoalSort) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Outlined.FilterList,
                    stringResource(R.string.a11y_filter_goals),
                    tint = if (sort == GoalSort.Manual) pocketColors.Muted else pocketColors.Coral,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    stringResource(R.string.goals_sort_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pocketColors.Muted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SORT_OPTIONS.forEach { (option, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes), color = pocketColors.Ink) },
                        trailingIcon = {
                            if (option == sort) Icon(Icons.Outlined.Check, null, tint = pocketColors.Coral)
                        },
                        onClick = {
                            onSelectSort(option)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TabPill(
            text = stringResource(R.string.goals_tab_active),
            selected = tab == GoalsViewModel.Tab.Active,
            onClick = { onSelectTab(GoalsViewModel.Tab.Active) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        TabPill(
            text = stringResource(R.string.goals_tab_completed),
            selected = tab == GoalsViewModel.Tab.Completed,
            onClick = { onSelectTab(GoalsViewModel.Tab.Completed) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) pocketColors.Coral else pocketColors.ChipPink)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else pocketColors.Coral,
        )
    }
}

/** Fila de meta: asa (opcional), ícono, nombre, "$ahorrado / $meta" y porcentaje. */
@Composable
private fun GoalRow(
    goal: Goal,
    currency: String,
    dragHandle: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pocketColors.Card)
            .border(1.dp, pocketColors.RoseBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dragHandle != null) {
            dragHandle()
            Spacer(Modifier.width(8.dp))
        }
        GoalIconBadge(goal.icon, goal.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(goal.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pocketColors.Ink)
            Spacer(Modifier.height(2.dp))
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
        Spacer(Modifier.width(8.dp))
        if (goal.isCompleted) {
            Icon(Icons.Outlined.Check, stringResource(R.string.goal_completed_badge), tint = pocketColors.Teal)
        } else {
            Text(
                stringResource(R.string.goals_percent, GoalRules.percent(goal.savedAmount, goal.targetAmount)),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = pocketColors.Ink,
            )
        }
    }
}

@Composable
private fun EmptyGoals(tab: GoalsViewModel.Tab) {
    val active = tab == GoalsViewModel.Tab.Active
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(if (active) R.string.goals_empty_active_title else R.string.goals_empty_completed_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = pocketColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(if (active) R.string.goals_empty_active_hint else R.string.goals_empty_completed_hint),
            fontSize = 12.sp,
            color = pocketColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

private val SORT_OPTIONS = listOf(
    GoalSort.Manual to R.string.goals_sort_manual,
    GoalSort.Closest to R.string.goals_sort_closest,
    GoalSort.Deadline to R.string.goals_sort_deadline,
    GoalSort.Recent to R.string.goals_sort_recent,
)

private const val ERROR_SHOWN_MS = 3_500L
