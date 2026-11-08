package com.pocketpal.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pocketpal.app.auth.*
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.dai.DaiReceiptViewModel
import com.pocketpal.app.ui.goals.GoalsViewModel
import com.pocketpal.app.ui.home.*
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.ui.newmovement.NewMovementOption
import com.pocketpal.app.ui.newmovement.NewMovementViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.profile.SecurityViewModel

/**
 * Inicio: saludo, "Disponible este mes", metas, presupuestos, movimientos recientes, Dai y el botón +.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.homeGraph(
    nav: NavHostController,
    vm: AuthViewModel,
    profileVm: ProfileViewModel,
    securityVm: SecurityViewModel,
    goalsVm: GoalsViewModel,
    movementsVm: MovementsViewModel,
    budgetsVm: BudgetsViewModel,
    daiReceiptVm: DaiReceiptViewModel,
    newMovementVm: NewMovementViewModel,
) {
    composable(Routes.Home) {
        LaunchedEffect(Unit) {
            profileVm.refresh()
            securityVm.registerCurrentDevice()
            goalsVm.refresh()
            movementsVm.refreshRecent()
            budgetsVm.refresh()
        }
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val goalsState by goalsVm.state.collectAsStateWithLifecycle()
        val recentMovements by movementsVm.home.collectAsStateWithLifecycle()
        val homeBudgets by budgetsVm.home.collectAsStateWithLifecycle()
        val data = (profileState as? ProfileViewModel.UiState.Content)?.data
        HomeScreen(
            name = data?.fullName.orEmpty(),
            email = data?.email.orEmpty(),
            avatarUrl = data?.avatarUrl,
            goals = (goalsState as? GoalsViewModel.UiState.Content)?.data?.active.orEmpty(),
            onOpenProfile = { nav.navigate(Routes.Profile) },
            onOpenGoals = { nav.navigate(Routes.Goals) },
            onOpenGoal = { id -> nav.navigate(Routes.goalDetail(id)) },
            onSignOut = vm::signOut,
            movements = recentMovements.movements,
            movementCategories = recentMovements.categories,
            currency = profileCurrency(profileState),
            onOpenMovements = { nav.navigate(Routes.Movements) },
            onOpenMovement = { id -> nav.navigate(Routes.movementDetail(id)) },
            budgets = homeBudgets,
            onOpenBudgets = { nav.navigate(Routes.Budgets) },
            onCreateBudget = { nav.navigate(Routes.BudgetNew) },
            onOpenBudget = { id -> nav.navigate(Routes.budgetDetail(id)) },
            onOpenDai = { nav.navigate(Routes.Dai) },
            // Módulo 6 · Flujo 5: "Disponible este mes" y el + de la barra de abajo.
            monthIncome = recentMovements.monthIncome,
            monthExpense = recentMovements.monthExpense,
            onRegister = { option ->
                when (option) {
                    NewMovementOption.Scan -> {
                        daiReceiptVm.start()
                        nav.navigate(Routes.NewMovementScan)
                    }
                    NewMovementOption.Voice -> nav.navigate(Routes.NewMovementVoice)
                    NewMovementOption.Manual -> {
                        newMovementVm.startManual()
                        nav.navigate(Routes.NewMovementManual)
                    }
                }
            },
        )
    }
}
