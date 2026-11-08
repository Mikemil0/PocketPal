package com.pocketpal.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.app.R
import com.pocketpal.app.data.goal.Goal
import com.pocketpal.app.data.movement.Movement
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.ui.MoneyFormat
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.budgets.HomeBudgetsSection
import com.pocketpal.app.ui.components.Avatar
import com.pocketpal.app.ui.dai.DaiAvatar
import com.pocketpal.app.ui.dai.DaiMood
import com.pocketpal.app.ui.goals.HomeGoalsSection
import com.pocketpal.app.ui.movements.HomeMovementsSection
import com.pocketpal.app.ui.newmovement.HomeAddSheet
import com.pocketpal.app.ui.newmovement.NewMovementOption
import com.pocketpal.app.ui.theme.pocketColors

/**
 * RF-A1: ícono circular de 40 dp arriba a la derecha — la foto del perfil si existe;
 * si no, un círculo con la inicial del nombre (o la del correo). Al tocarlo abre
 * "Mi perfil".
 *
 * Módulo 3 (flujo 1): debajo del saludo aparece la sección "Metas de ahorro" con las
 * metas activas; la flecha abre la lista completa.
 *
 * Módulo 4 (flujo 1): debajo de las metas, "Movimientos recientes" con "ver todos".
 *
 * Módulo 5 (flujo 1): entre las metas y los movimientos, la tarjeta "Presupuestos".
 *
 * Módulo 6 (flujo 1): Dai flotando abajo a la derecha; al tocarlo abre su bienvenida.
 *
 * Módulo 6 (flujo 5, Figma "INICIO" e "INICIO MAS MENÚ"): arriba la tarjeta azul
 * "Hola, Nombre · Disponible este mes" (ingresos − gastos del mes) y abajo la barra de
 * navegación con el **+** en el centro, que abre "Registro mediante escáner / por voz /
 * manual".
 */
@Composable
fun HomeScreen(
    name: String,
    email: String,
    avatarUrl: String?,
    goals: List<Goal>,
    onOpenProfile: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onSignOut: () -> Unit,
    movements: List<Movement> = emptyList(),
    movementCategories: Map<String, MovementCategory> = emptyMap(),
    currency: String = "COP",
    onOpenMovements: () -> Unit = {},
    onOpenMovement: (String) -> Unit = {},
    budgets: BudgetsViewModel.HomeBudgets = BudgetsViewModel.HomeBudgets(),
    onOpenBudgets: () -> Unit = {},
    onOpenBudget: (String) -> Unit = {},
    onCreateBudget: () -> Unit = {},
    onOpenDai: (() -> Unit)? = null,
    monthIncome: Double = 0.0,
    monthExpense: Double = 0.0,
    onRegister: ((NewMovementOption) -> Unit)? = null,
) {
    val firstName = name.trim().substringBefore(" ")
    // Estado propio y pequeño: si la hoja del + está abierta.
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(pocketColors.Cream)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                HomeHeader(
                    firstName = firstName,
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    available = monthIncome - monthExpense,
                    income = monthIncome,
                    expense = monthExpense,
                    currency = currency,
                    onOpenProfile = onOpenProfile,
                )
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(20.dp))
                    HomeGoalsSection(
                        goals = goals,
                        onOpenGoals = onOpenGoals,
                        onOpenGoal = onOpenGoal,
                    )
                    Spacer(Modifier.height(24.dp))
                    HomeBudgetsSection(
                        home = budgets,
                        currency = currency,
                        onSeeAll = onOpenBudgets,
                        onOpen = onOpenBudget,
                        onCreate = onCreateBudget,
                    )
                    Spacer(Modifier.height(24.dp))
                    HomeMovementsSection(
                        movements = movements,
                        categories = movementCategories,
                        currency = currency,
                        onSeeAll = onOpenMovements,
                        onOpenMovement = onOpenMovement,
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        stringResource(R.string.home_sign_out), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pocketColors.Coral,
                        modifier = Modifier.clickable(onClick = onSignOut),
                    )
                    Spacer(Modifier.height(96.dp))
                }
            }
            if (onRegister != null) {
                HomeBottomBar(
                    onGoals = onOpenGoals,
                    onAdd = { showAdd = true },
                    onBudgets = onOpenBudgets,
                    onMovements = onOpenMovements,
                )
            }
        }

        // Módulo 6: botón flotante de Dai, encima de la barra de abajo.
        if (onOpenDai != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = if (onRegister != null) 96.dp else 16.dp)
                    .clickable(onClickLabel = stringResource(R.string.home_open_dai), onClick = onOpenDai),
            ) {
                DaiAvatar(DaiMood.Online, size = 60.dp)
            }
        }
    }

    if (showAdd && onRegister != null) {
        HomeAddSheet(
            onDismiss = { showAdd = false },
            onSelect = { option ->
                showAdd = false
                onRegister(option)
            },
        )
    }
}

/** Tarjeta azul: "Hola, Nombre", avatar y "Disponible este mes" con ingresos y gastos. */
@Composable
private fun HomeHeader(
    firstName: String,
    name: String,
    email: String,
    avatarUrl: String?,
    available: Double,
    income: Double,
    expense: Double,
    currency: String,
    onOpenProfile: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(pocketColors.Navy)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.home_hello), fontSize = 11.sp, color = pocketColors.Muted)
                Text(
                    firstName.ifBlank { stringResource(R.string.home_hello_default) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = pocketColors.Coral,
                )
            }
            Spacer(Modifier.width(12.dp))
            // RNF-07: área táctil de 48 dp alrededor del ícono de 40 dp.
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center,
            ) {
                Avatar(
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    size = 40.dp,
                    contentDescription = stringResource(R.string.home_open_profile),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.home_available), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
        Text(
            MoneyFormat.format(available, currency),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowUpward, null, tint = pocketColors.Teal, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.home_income, MoneyFormat.format(income, currency)),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Outlined.ArrowDownward, null, tint = pocketColors.Coral, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.home_expense, MoneyFormat.format(expense, currency)),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Barra de abajo del Figma: Inicio · Metas · ( + ) · Presupuestos · Movimientos. */
@Composable
private fun HomeBottomBar(
    onGoals: () -> Unit,
    onAdd: () -> Unit,
    onBudgets: () -> Unit,
    onMovements: () -> Unit,
) {
    // Rectángulo flotante (Figma): márgenes alrededor, esquinas redondeadas y sombra.
    val barShape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(8.dp, barShape)
                .clip(barShape)
                .background(pocketColors.Card)
                .border(1.dp, pocketColors.RoseBorder.copy(alpha = 0.5f), barShape)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarItem(Icons.Outlined.Home, stringResource(R.string.home_nav_home), selected = true, onClick = {})
            BarItem(Icons.Outlined.Savings, stringResource(R.string.home_nav_goals), onClick = onGoals)
            Box(
                Modifier
                    .size(52.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(pocketColors.Coral)
                    .clickable(onClickLabel = stringResource(R.string.reg_add_title), onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.reg_add_title), tint = Color.White, modifier = Modifier.size(30.dp))
            }
            BarItem(Icons.Outlined.PieChart, stringResource(R.string.home_nav_budgets), onClick = onBudgets)
            BarItem(Icons.Outlined.SwapVert, stringResource(R.string.home_nav_movements), onClick = onMovements)
        }
    }
}

@Composable
private fun BarItem(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (selected) pocketColors.Coral else pocketColors.Muted, modifier = Modifier.size(24.dp))
    }
}