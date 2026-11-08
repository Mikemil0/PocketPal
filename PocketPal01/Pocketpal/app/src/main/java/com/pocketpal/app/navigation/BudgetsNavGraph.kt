package com.pocketpal.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocketpal.app.ui.budgets.BudgetCategoryPickerScreen
import com.pocketpal.app.ui.budgets.BudgetCreatedScreen
import com.pocketpal.app.ui.budgets.BudgetDetailScreen
import com.pocketpal.app.ui.budgets.BudgetDetailViewModel
import com.pocketpal.app.ui.budgets.BudgetFormScreen
import com.pocketpal.app.ui.budgets.BudgetFormViewModel
import com.pocketpal.app.ui.budgets.BudgetHistoryScreen
import com.pocketpal.app.ui.budgets.BudgetsErrorState
import com.pocketpal.app.ui.budgets.BudgetsScreen
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Módulo 5 · Presupuestos: lista, detalle, historial, crear, editar y eliminar.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.budgetsGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    profileVm: ProfileViewModel,
    budgetsVm: BudgetsViewModel,
    budgetFormVm: BudgetFormViewModel,
    budgetDetailVm: BudgetDetailViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()

    // Módulo 5 · Flujo 1: lista de presupuestos (presupuestado, gastado y una tarjeta por categoría).
    composable(Routes.Budgets) {
        LaunchedEffect(Unit) { budgetsVm.refresh() }
        val budgetsState by budgetsVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        BudgetsScreen(
            state = budgetsState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onCreate = { nav.navigate(Routes.BudgetNew) },
            onOpen = { id -> nav.navigate(Routes.budgetDetail(id)) },
            onRetry = budgetsVm::refresh,
            onEdit = { id -> nav.navigate(Routes.budgetEdit(id)) },
        )
    }

    // Módulo 5 · Flujo 3: detalle del presupuesto (gastado, lo que queda y gastos del período) e historial.
    composable(
        Routes.BudgetDetail,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        LaunchedEffect(id) { budgetDetailVm.load(id) }
        val detailState by budgetDetailVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        BudgetDetailScreen(
            state = detailState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onOpenHistory = { nav.navigate(Routes.budgetHistory(id)) },
            onOpenMovement = { movementId -> nav.navigate(Routes.movementDetail(movementId)) },
            onRetry = budgetDetailVm::refresh,
            onEdit = { nav.navigate(Routes.budgetEdit(id)) },
        )
    }
    composable(
        Routes.BudgetHistory,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        LaunchedEffect(Unit) { budgetDetailVm.loadHistory() }
        val historyState by budgetDetailVm.history.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        BudgetHistoryScreen(
            state = historyState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onRetry = budgetDetailVm::loadHistory,
        )
    }

    // Módulo 5 · Flujo 4: "Editar presupuesto" (mismo formulario) y "Eliminar presupuesto".
    composable(
        Routes.BudgetEdit,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        var prepared by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(id) {
            if (!prepared) {
                budgetFormVm.prepareEdit(id)
                prepared = true
            }
        }
        val editing by budgetFormVm.editing.collectAsStateWithLifecycle()
        val selectedId by budgetFormVm.selectedCategoryId.collectAsStateWithLifecycle()
        val categories by budgetFormVm.categories.collectAsStateWithLifecycle()
        val budgets by budgetFormVm.budgets.collectAsStateWithLifecycle()
        val saving by budgetFormVm.saving.collectAsStateWithLifecycle()
        val deleting by budgetFormVm.deleting.collectAsStateWithLifecycle()
        val formError by budgetFormVm.error.collectAsStateWithLifecycle()
        val formEvent by budgetFormVm.event.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(formEvent) {
            when (formEvent) {
                is BudgetFormViewModel.Event.Updated -> {
                    budgetFormVm.consumeEvent()
                    budgetsVm.refresh()
                    budgetDetailVm.refresh()
                    nav.popBackStack()
                }
                BudgetFormViewModel.Event.Deleted -> {
                    budgetFormVm.consumeEvent()
                    budgetsVm.refresh()
                    // El detalle ya no existe: se vuelve a la lista (o al inicio si se llegó desde allí).
                    if (!nav.popBackStack(Routes.Budgets, inclusive = false)) {
                        nav.popBackStack(Routes.Home, inclusive = false)
                    }
                }
                else -> Unit
            }
        }
        val budget = editing
        if ((budget == null || budget.id != id) && formError != null) {
            // No se pudo traer el presupuesto (sin internet o ya no existe).
            Box(Modifier.fillMaxSize().background(pocketColors.Cream).padding(24.dp)) {
                BudgetsErrorState(onRetry = { budgetFormVm.prepareEdit(id) })
            }
        } else if (budget == null || budget.id != id) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
        } else {
            BudgetFormScreen(
                category = categories.firstOrNull { it.id == selectedId },
                currency = profileCurrency(profileState),
                isDuplicate = budgets.isNotEmpty() && budgetFormVm.isDuplicate(),
                saving = saving,
                error = formError,
                onBack = ::back,
                onChangeCategory = { nav.navigate(Routes.BudgetCategory) },
                onSave = budgetFormVm::update,
                onErrorShown = budgetFormVm::consumeError,
                editing = budget,
                deleting = deleting,
                onDelete = budgetFormVm::delete,
            )
        }
    }

    // Módulo 5 · Flujo 2: "Nuevo presupuesto" → "Elegir categoría" → "Presupuesto creado".
    composable(Routes.BudgetNew) {
        // Solo la primera vez: al volver de "Elegir categoría" no se borra lo escrito.
        var prepared by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!prepared) {
                budgetFormVm.prepareNew()
                prepared = true
            }
        }
        val selectedId by budgetFormVm.selectedCategoryId.collectAsStateWithLifecycle()
        // Se leen para que el aviso de categoría repetida se recalcule cuando cambian.
        val categories by budgetFormVm.categories.collectAsStateWithLifecycle()
        val budgets by budgetFormVm.budgets.collectAsStateWithLifecycle()
        val saving by budgetFormVm.saving.collectAsStateWithLifecycle()
        val formError by budgetFormVm.error.collectAsStateWithLifecycle()
        val formEvent by budgetFormVm.event.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(formEvent) {
            if (formEvent is BudgetFormViewModel.Event.Created) {
                budgetFormVm.consumeEvent()
                budgetsVm.refresh()
                // El formulario sale de la pila: "atrás" desde "Presupuesto creado" no vuelve a él.
                nav.navigate(Routes.BudgetCreated) { popUpTo(Routes.BudgetNew) { inclusive = true } }
            }
        }
        BudgetFormScreen(
            category = categories.firstOrNull { it.id == selectedId },
            currency = profileCurrency(profileState),
            isDuplicate = budgets.isNotEmpty() && budgetFormVm.isDuplicate(),
            saving = saving,
            error = formError,
            onBack = ::back,
            onChangeCategory = { nav.navigate(Routes.BudgetCategory) },
            onSave = budgetFormVm::create,
            onErrorShown = budgetFormVm::consumeError,
        )
    }
    composable(Routes.BudgetCategory) {
        val selectedId by budgetFormVm.selectedCategoryId.collectAsStateWithLifecycle()
        val categories by budgetFormVm.categories.collectAsStateWithLifecycle()
        val budgets by budgetFormVm.budgets.collectAsStateWithLifecycle()
        val monthSpent by budgetFormVm.monthSpent.collectAsStateWithLifecycle()
        val loaded by budgetFormVm.loaded.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        BudgetCategoryPickerScreen(
            categories = remember(categories, budgets) { budgetFormVm.availableCategories() },
            monthSpent = monthSpent,
            selectedId = selectedId,
            loaded = loaded,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onConfirm = { id ->
                budgetFormVm.selectCategory(id)
                nav.popBackStack()
            },
        )
    }
    composable(Routes.BudgetCreated) {
        val created by budgetFormVm.created.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        fun seeBudgets() {
            // Si se llegó desde la lista se vuelve a ella; si fue desde el inicio, se abre.
            if (!nav.popBackStack(Routes.Budgets, inclusive = false)) {
                nav.navigate(Routes.Budgets) { popUpTo(Routes.Home) }
            }
        }
        val status = created
        if (status == null) {
            LaunchedEffect(Unit) { seeBudgets() }
        } else {
            BudgetCreatedScreen(
                status = status,
                currency = profileCurrency(profileState),
                onSeeBudgets = ::seeBudgets,
                onCreateAnother = {
                    nav.navigate(Routes.BudgetNew) { popUpTo(Routes.BudgetCreated) { inclusive = true } }
                },
            )
        }
    }
}
