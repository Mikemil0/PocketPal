package com.pocketpal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pocketpal.app.auth.*
import com.pocketpal.app.ui.budgets.BudgetDetailViewModel
import com.pocketpal.app.ui.budgets.BudgetFormViewModel
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.dai.DaiChatViewModel
import com.pocketpal.app.ui.dai.DaiInsightsViewModel
import com.pocketpal.app.ui.dai.DaiReceiptViewModel
import com.pocketpal.app.ui.dai.DaiSettingsViewModel
import com.pocketpal.app.ui.goals.GoalDetailViewModel
import com.pocketpal.app.ui.goals.GoalFormViewModel
import com.pocketpal.app.ui.goals.GoalsViewModel
import com.pocketpal.app.ui.movements.MovementCalendarViewModel
import com.pocketpal.app.ui.movements.MovementCategoriesViewModel
import com.pocketpal.app.ui.movements.MovementDetailViewModel
import com.pocketpal.app.ui.movements.MovementSearchViewModel
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.ui.newmovement.NewMovementViewModel
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.ExportViewModel
import com.pocketpal.app.ui.profile.NotificationSettingsViewModel
import com.pocketpal.app.ui.profile.PhoneViewModel
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.profile.SecurityViewModel

/**
 * La navegación de toda la app (Navigation Compose).
 *
 * Cada módulo tiene su propio grafo en un archivo aparte (`AuthNavGraph`, `HomeNavGraph`,
 * `ProfileNavGraph`, `GoalsNavGraph`, `MovementsNavGraph`, `BudgetsNavGraph`, `DaiNavGraph`
 * y `NewMovementNavGraph`) y las rutas están en [Routes]. Aquí solo:
 * 1. se lee el estado de la sesión ([AuthViewModel]) para decidir a dónde ir
 *    (login correcto, código enviado, sesión vencida, sin internet…);
 * 2. se juntan todos los grafos en un solo `NavHost` que empieza en el splash.
 *
 * Los ViewModels se crean en `MainActivity` y llegan aquí ya listos (estado elevado).
 */
@Composable
fun PocketNavHost(
    vm: AuthViewModel,
    profileVm: ProfileViewModel,
    categoriesVm: CategoriesViewModel,
    exportVm: ExportViewModel,
    preferencesVm: PreferencesViewModel,
    notificationsVm: NotificationSettingsViewModel,
    securityVm: SecurityViewModel,
    phoneVm: PhoneViewModel,
    goalsVm: GoalsViewModel,
    goalFormVm: GoalFormViewModel,
    goalDetailVm: GoalDetailViewModel,
    movementsVm: MovementsViewModel,
    movementSearchVm: MovementSearchViewModel,
    movementCategoriesVm: MovementCategoriesViewModel,
    movementCalendarVm: MovementCalendarViewModel,
    movementDetailVm: MovementDetailViewModel,
    budgetsVm: BudgetsViewModel,
    budgetFormVm: BudgetFormViewModel,
    budgetDetailVm: BudgetDetailViewModel,
    daiChatVm: DaiChatViewModel,
    daiInsightsVm: DaiInsightsViewModel,
    daiReceiptVm: DaiReceiptViewModel,
    daiSettingsVm: DaiSettingsViewModel,
    newMovementVm: NewMovementViewModel,
    nav: NavHostController = rememberNavController(),
) {
    val authState = vm.state.collectAsStateWithLifecycle()
    val otpExpiresAtState = vm.otpExpiresAt.collectAsStateWithLifecycle()
    val state by authState
    val context = LocalContext.current
    val actions = remember(nav, context) { PocketNavActions(nav, context, vm, securityVm, daiChatVm) }

    // Un solo lugar decide la navegación a partir del estado del ViewModel.
    LaunchedEffect(state) {
        when (val s = state) {
            is AuthState.Success<*> -> when (s.step) {
                AuthStep.LoggedIn -> actions.afterAuth()
                AuthStep.SignupOtpSent -> actions.go(Routes.OtpSignup)
                AuthStep.EmailVerified -> actions.go(Routes.AccountCreated)
                AuthStep.RecoveryCodeSent -> actions.go(Routes.ForgotOtp)
                AuthStep.RecoveryCodeVerified -> actions.go(Routes.ForgotNew)
                AuthStep.PasswordUpdated -> actions.go(Routes.ForgotDone)
                AuthStep.CodeResent -> vm.consume()
                AuthStep.LoggedOut -> { vm.consume(); nav.navigate(Routes.Welcome) { popUpTo(0) } }
                AuthStep.GoogleConnected -> Unit // la pantalla de Google muestra el éxito
            }
            is AuthState.Error -> when (s.type) {
                ErrorType.NoNetwork -> if (nav.currentDestination?.route != Routes.NoWifi) nav.navigate(Routes.NoWifi)
                ErrorType.SessionExpired -> nav.navigate(Routes.SessionExpired) { popUpTo(0) }
                else -> Unit // los banners los pintan las propias pantallas
            }
            else -> Unit
        }
    }

    NavHost(nav, startDestination = Routes.Splash) {
        authGraph(
            nav = nav,
            actions = actions,
            vm = vm,
            authState = authState,
            otpExpiresAtState = otpExpiresAtState,
        )
        homeGraph(
            nav = nav,
            vm = vm,
            profileVm = profileVm,
            securityVm = securityVm,
            goalsVm = goalsVm,
            movementsVm = movementsVm,
            budgetsVm = budgetsVm,
            daiReceiptVm = daiReceiptVm,
            newMovementVm = newMovementVm,
        )
        newMovementGraph(
            nav = nav,
            actions = actions,
            profileVm = profileVm,
            preferencesVm = preferencesVm,
            movementsVm = movementsVm,
            budgetsVm = budgetsVm,
            daiChatVm = daiChatVm,
            daiReceiptVm = daiReceiptVm,
            newMovementVm = newMovementVm,
        )
        daiGraph(
            nav = nav,
            actions = actions,
            context = context,
            profileVm = profileVm,
            preferencesVm = preferencesVm,
            movementsVm = movementsVm,
            budgetsVm = budgetsVm,
            daiChatVm = daiChatVm,
            daiInsightsVm = daiInsightsVm,
            daiReceiptVm = daiReceiptVm,
            daiSettingsVm = daiSettingsVm,
        )
        budgetsGraph(
            nav = nav,
            actions = actions,
            profileVm = profileVm,
            budgetsVm = budgetsVm,
            budgetFormVm = budgetFormVm,
            budgetDetailVm = budgetDetailVm,
            newMovementVm = newMovementVm,
        )
        movementsGraph(
            nav = nav,
            actions = actions,
            profileVm = profileVm,
            preferencesVm = preferencesVm,
            movementsVm = movementsVm,
            movementSearchVm = movementSearchVm,
            movementCategoriesVm = movementCategoriesVm,
            movementCalendarVm = movementCalendarVm,
            movementDetailVm = movementDetailVm,
        )
        goalsGraph(
            nav = nav,
            actions = actions,
            profileVm = profileVm,
            preferencesVm = preferencesVm,
            goalsVm = goalsVm,
            goalFormVm = goalFormVm,
            goalDetailVm = goalDetailVm,
        )
        profileGraph(
            nav = nav,
            actions = actions,
            vm = vm,
            profileVm = profileVm,
            categoriesVm = categoriesVm,
            exportVm = exportVm,
            preferencesVm = preferencesVm,
            notificationsVm = notificationsVm,
            securityVm = securityVm,
            phoneVm = phoneVm,
        )
    }
}
