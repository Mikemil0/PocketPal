package com.pocketpal.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocketpal.app.ui.goals.AddContributionScreen
import com.pocketpal.app.ui.goals.GoalCreatedScreen
import com.pocketpal.app.ui.goals.GoalDetailScreen
import com.pocketpal.app.ui.goals.GoalDetailViewModel
import com.pocketpal.app.ui.goals.GoalFormScreen
import com.pocketpal.app.ui.goals.GoalFormViewModel
import com.pocketpal.app.ui.goals.GoalsScreen
import com.pocketpal.app.ui.goals.GoalsViewModel
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.theme.pocketColors

/**
 * Módulo 3 · Metas de ahorro: lista, crear, detalle, aportes, editar y eliminar.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.goalsGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    profileVm: ProfileViewModel,
    preferencesVm: PreferencesViewModel,
    goalsVm: GoalsViewModel,
    goalFormVm: GoalFormViewModel,
    goalDetailVm: GoalDetailViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()

    // Módulo 3 · Flujo 1: lista de metas (activas/completadas, filtro y reordenar).
    composable(Routes.Goals) {
        LaunchedEffect(Unit) { goalsVm.refresh() }
        val goalsState by goalsVm.state.collectAsStateWithLifecycle()
        val goalsError by goalsVm.error.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        GoalsScreen(
            state = goalsState,
            error = goalsError,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onSelectTab = goalsVm::selectTab,
            onSelectSort = goalsVm::selectSort,
            onMove = goalsVm::move,
            onCreate = { nav.navigate(Routes.GoalNew) },
            onOpen = { id -> nav.navigate(Routes.goalDetail(id)) },
            onErrorShown = goalsVm::consumeError,
        )
    }

    // Módulo 3 · Flujo 2: crear meta → "Meta creada".
    composable(Routes.GoalNew) {
        LaunchedEffect(Unit) { goalFormVm.prepareNew() }
        val saving by goalFormVm.saving.collectAsStateWithLifecycle()
        val formError by goalFormVm.error.collectAsStateWithLifecycle()
        val formEvent by goalFormVm.event.collectAsStateWithLifecycle()
        // Se lee para que el aviso de nombre repetido se recalcule cuando llegan las metas.
        val existing by goalFormVm.existing.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(formEvent) {
            if (formEvent is GoalFormViewModel.Event.Created) {
                goalFormVm.consumeEvent()
                goalsVm.refresh()
                // El formulario sale de la pila: "atrás" desde "Meta creada" va a la lista.
                nav.navigate(Routes.GoalCreated) { popUpTo(Routes.GoalNew) { inclusive = true } }
            }
        }
        GoalFormScreen(
            currency = profileCurrency(profileState),
            datePreference = prefs.dateFormat,
            isDuplicateName = { name -> existing.isNotEmpty() && goalFormVm.isDuplicateName(name) },
            saving = saving,
            error = formError,
            onBack = ::back,
            onSave = goalFormVm::create,
            onErrorShown = goalFormVm::consumeError,
        )
    }
    // Módulo 3 · Flujo 3: detalle de la meta, historial y "Agregar aporte".
    composable(
        Routes.GoalDetail,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        LaunchedEffect(id) { goalDetailVm.load(id) }
        val detailState by goalDetailVm.state.collectAsStateWithLifecycle()
        val detailError by goalDetailVm.error.collectAsStateWithLifecycle()
        val celebration by goalDetailVm.celebration.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        GoalDetailScreen(
            state = detailState,
            error = detailError,
            celebration = celebration,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onAddContribution = { nav.navigate(Routes.goalContribute(id)) },
            onRetry = goalDetailVm::refresh,
            onErrorShown = goalDetailVm::consumeError,
            onDismissCelebration = goalDetailVm::dismissCelebration,
            onEdit = { nav.navigate(Routes.goalEdit(id)) },
        )
    }
    composable(
        Routes.GoalContribute,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) {
        val detailState by goalDetailVm.state.collectAsStateWithLifecycle()
        val saving by goalDetailVm.saving.collectAsStateWithLifecycle()
        val detailError by goalDetailVm.error.collectAsStateWithLifecycle()
        val detailEvent by goalDetailVm.event.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(detailEvent) {
            if (detailEvent is GoalDetailViewModel.Event.ContributionAdded) {
                goalDetailVm.consumeEvent()
                goalsVm.refresh()
                // Vuelve al detalle; si la meta se cumplió, allí aparece la celebración.
                nav.popBackStack()
            }
        }
        val goal = (detailState as? GoalDetailViewModel.UiState.Content)?.data?.goal
        if (goal == null) {
            LaunchedEffect(Unit) { nav.popBackStack() }
        } else {
            AddContributionScreen(
                goal = goal,
                currency = profileCurrency(profileState),
                datePreference = prefs.dateFormat,
                saving = saving,
                error = detailError,
                onBack = ::back,
                onSave = goalDetailVm::addContribution,
                onErrorShown = goalDetailVm::consumeError,
            )
        }
    }
    // Módulo 3 · Flujo 4: editar la meta (mismo formulario prellenado) y eliminarla.
    composable(
        Routes.GoalEdit,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        LaunchedEffect(id) { goalFormVm.prepareEdit(id) }
        val editing by goalFormVm.editing.collectAsStateWithLifecycle()
        val contributionsCount by goalFormVm.contributionsCount.collectAsStateWithLifecycle()
        val saving by goalFormVm.saving.collectAsStateWithLifecycle()
        val deleting by goalFormVm.deleting.collectAsStateWithLifecycle()
        val formError by goalFormVm.error.collectAsStateWithLifecycle()
        val formEvent by goalFormVm.event.collectAsStateWithLifecycle()
        val existing by goalFormVm.existing.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(formEvent) {
            when (formEvent) {
                is GoalFormViewModel.Event.Updated -> {
                    goalFormVm.consumeEvent()
                    goalsVm.refresh()
                    goalDetailVm.refresh()
                    nav.popBackStack()
                }
                GoalFormViewModel.Event.Deleted -> {
                    goalFormVm.consumeEvent()
                    goalsVm.refresh()
                    // Se sale del formulario y del detalle de la meta borrada: queda la lista.
                    nav.navigate(Routes.Goals) { popUpTo(Routes.Goals) { inclusive = true } }
                }
                else -> Unit
            }
        }
        val goal = editing?.takeIf { it.id == id }
        // Si no se pudo cargar la meta (sin internet o ya no existe), se vuelve al detalle.
        LaunchedEffect(goal, formError) {
            if (goal == null && formError != null) {
                goalFormVm.consumeError()
                nav.popBackStack()
            }
        }
        if (goal == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = pocketColors.Coral)
            }
        } else {
            // key: si cambia la meta, el formulario se vuelve a prellenar desde cero.
            key(goal.id) {
                GoalFormScreen(
                    currency = profileCurrency(profileState),
                    datePreference = prefs.dateFormat,
                    isDuplicateName = { name -> existing.isNotEmpty() && goalFormVm.isDuplicateName(name) },
                    saving = saving,
                    error = formError,
                    onBack = ::back,
                    onSave = goalFormVm::update,
                    onErrorShown = goalFormVm::consumeError,
                    editing = goal,
                    contributionsCount = contributionsCount,
                    deleting = deleting,
                    onDelete = goalFormVm::delete,
                )
            }
        }
    }
    composable(Routes.GoalCreated) {
        val created by goalFormVm.created.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val goal = created
        if (goal == null) {
            LaunchedEffect(Unit) { nav.popBackStack() }
        } else {
            GoalCreatedScreen(
                goal = goal,
                currency = profileCurrency(profileState),
                onSeeGoals = {
                    nav.navigate(Routes.Goals) { popUpTo(Routes.Goals) { inclusive = true } }
                },
                onCreateAnother = {
                    nav.navigate(Routes.GoalNew) { popUpTo(Routes.GoalCreated) { inclusive = true } }
                },
            )
        }
    }
}
