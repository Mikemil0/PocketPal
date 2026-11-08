package com.pocketpal.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocketpal.app.ui.movements.MovementCalendarScreen
import com.pocketpal.app.ui.movements.MovementCalendarViewModel
import com.pocketpal.app.ui.movements.MovementCategoriesScreen
import com.pocketpal.app.ui.movements.MovementCategoriesViewModel
import com.pocketpal.app.ui.movements.MovementDetailViewModel
import com.pocketpal.app.ui.movements.MovementSearchScreen
import com.pocketpal.app.ui.movements.MovementSearchViewModel
import com.pocketpal.app.ui.movements.MovementsScreen
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.ui.movements.RecurringMovementScreen
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel

/**
 * Módulo 4 · Movimientos: lista, búsqueda, detalle, gastos por categoría y calendario.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.movementsGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    profileVm: ProfileViewModel,
    preferencesVm: PreferencesViewModel,
    movementsVm: MovementsViewModel,
    movementSearchVm: MovementSearchViewModel,
    movementCategoriesVm: MovementCategoriesViewModel,
    movementCalendarVm: MovementCalendarViewModel,
    movementDetailVm: MovementDetailViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()

    // Módulo 4 · Flujo 1: lista principal de movimientos (resumen, filtros y lista por día).
    composable(Routes.Movements) {
        LaunchedEffect(Unit) { movementsVm.refresh() }
        val movementsState by movementsVm.state.collectAsStateWithLifecycle()
        val movementsError by movementsVm.error.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        MovementsScreen(
            state = movementsState,
            error = movementsError,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onSearch = { nav.navigate(Routes.MovementSearch) },
            onOpenCategories = { nav.navigate(Routes.MovementCategories) },
            onOpenCalendar = { nav.navigate(Routes.MovementCalendar) },
            onSelectFilter = movementsVm::selectFilter,
            onOpenMovement = { id -> nav.navigate(Routes.movementDetail(id)) },
            onRetry = movementsVm::refresh,
            onGoHome = { nav.popBackStack(Routes.Home, inclusive = false) },
            onErrorShown = movementsVm::consumeError,
        )
    }

    // Módulo 4 · Flujo 2: buscar movimientos (texto + Todos / Gastos / Ingresos).
    composable(Routes.MovementSearch) {
        LaunchedEffect(Unit) { movementSearchVm.prepare() }
        val searchState by movementSearchVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        MovementSearchScreen(
            state = searchState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onQueryChange = movementSearchVm::onQueryChange,
            onClear = movementSearchVm::clear,
            onSelectKind = movementSearchVm::selectKind,
            onRetry = movementSearchVm::retry,
            onOpenMovement = { id -> nav.navigate(Routes.movementDetail(id)) },
        )
    }

    // Módulo 4 · Flujo 4: detalle. Si es recurrente se ve primero "Movimiento recurrente"
    // y "Ver más detalles" abre el detalle completo.
    composable(
        Routes.MovementDetail,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        LaunchedEffect(id) { movementDetailVm.load(id) }
        val detailState by movementDetailVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val recurring = (detailState as? MovementDetailViewModel.UiState.Content)?.data?.movement?.isRecurring == true
        if (recurring) {
            RecurringMovementScreen(
                state = detailState,
                currency = profileCurrency(profileState),
                onBack = ::back,
                onRetry = movementDetailVm::refresh,
                onGoHome = { nav.popBackStack(Routes.Home, inclusive = false) },
                onSeeDetails = { nav.navigate(Routes.movementFullDetail(id)) },
            )
        } else {
            MovementDetailRoute(
                vm = movementDetailVm,
                currency = profileCurrency(profileState),
                onBack = ::back,
                onGoHome = { nav.popBackStack(Routes.Home, inclusive = false) },
            )
        }
    }
    composable(
        Routes.MovementFullDetail,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        LaunchedEffect(id) { movementDetailVm.load(id) }
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        MovementDetailRoute(
            vm = movementDetailVm,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onGoHome = { nav.popBackStack(Routes.Home, inclusive = false) },
        )
    }

    // Módulo 4 · Flujo 3: gastos del mes por categoría.
    composable(Routes.MovementCategories) {
        LaunchedEffect(Unit) { movementCategoriesVm.refresh() }
        val categoriesState by movementCategoriesVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        MovementCategoriesScreen(
            state = categoriesState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onRetry = movementCategoriesVm::refresh,
            onGoHome = { nav.popBackStack(Routes.Home, inclusive = false) },
        )
    }

    // Módulo 4 · Flujo 3: calendario para elegir un rango de fechas.
    composable(Routes.MovementCalendar) {
        LaunchedEffect(Unit) { movementCalendarVm.open(movementsVm.currentRange()) }
        val calendarState by movementCalendarVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        MovementCalendarScreen(
            state = calendarState,
            weekStart = prefs.weekStart,
            canGoNext = calendarState.month.isBefore(java.time.YearMonth.now()),
            onBack = ::back,
            onPreviousMonth = movementCalendarVm::previousMonth,
            onNextMonth = movementCalendarVm::nextMonth,
            onTapDay = movementCalendarVm::tapDay,
            onLast30Days = movementCalendarVm::selectLast30Days,
            onCustom = movementCalendarVm::selectCustom,
            onApply = {
                // El rango vuelve a la lista principal, que se recarga con esas fechas.
                movementCalendarVm.confirm()?.let { range ->
                    movementsVm.selectRange(range)
                    nav.popBackStack()
                }
            },
        )
    }
}
