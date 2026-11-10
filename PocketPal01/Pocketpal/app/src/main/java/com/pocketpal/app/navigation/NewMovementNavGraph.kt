package com.pocketpal.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pocketpal.app.R
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.dai.DaiCameraScreen
import com.pocketpal.app.ui.dai.DaiChatViewModel
import com.pocketpal.app.ui.dai.DaiReceiptReviewScreen
import com.pocketpal.app.ui.dai.DaiReceiptViewModel
import com.pocketpal.app.ui.dai.DaiVoiceScreen
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.ui.newmovement.NewMovementFormScreen
import com.pocketpal.app.ui.newmovement.NewMovementSuccessScreen
import com.pocketpal.app.ui.newmovement.NewMovementViewModel
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel

/**
 * Módulo 6 · Flujo 5: registrar un movimiento desde el + del inicio (escáner, voz o manual) y "Movimiento registrado correctamente".
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.newMovementGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    profileVm: ProfileViewModel,
    preferencesVm: PreferencesViewModel,
    movementsVm: MovementsViewModel,
    budgetsVm: BudgetsViewModel,
    daiChatVm: DaiChatViewModel,
    daiReceiptVm: DaiReceiptViewModel,
    newMovementVm: NewMovementViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()

    // Módulo 6 · Flujo 5 · "Registro mediante escáner": la misma cámara y OCR de Dai (flujo 3),
    // pero al guardar abre "Movimiento registrado correctamente" en vez del chat.
    composable(Routes.NewMovementScan) {
        val receipt by daiReceiptVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(receipt.stage) {
            if (receipt.stage == DaiReceiptViewModel.Stage.Review) nav.navigate(Routes.NewMovementScanReview)
        }
        DaiCameraScreen(
            state = receipt,
            onBack = ::back,
            onReading = daiReceiptVm::onReading,
            onTextRecognized = daiReceiptVm::onTextRecognized,
            onReadFailed = daiReceiptVm::onReadFailed,
            // "Cámara bloqueada → Manual": el formulario de registro manual.
            onManual = {
                newMovementVm.startManual()
                nav.navigate(Routes.NewMovementManual) { popUpTo(Routes.NewMovementScan) { inclusive = true } }
            },
            onRetake = daiReceiptVm::retake,
            onPhoto = daiReceiptVm::onPhoto,
        )
    }
    composable(Routes.NewMovementScanReview) {
        val receipt by daiReceiptVm.state.collectAsStateWithLifecycle()
        val event by daiReceiptVm.event.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        fun backToCamera() {
            daiReceiptVm.retake()
            nav.popBackStack()
        }
        androidx.activity.compose.BackHandler(onBack = ::backToCamera)
        LaunchedEffect(event) {
            if (event is DaiReceiptViewModel.Event.Saved) {
                daiReceiptVm.consumeEvent()
                daiChatVm.reload()
                movementsVm.refreshRecent()
                budgetsVm.refresh()
                nav.navigate(Routes.NewMovementSuccess) { popUpTo(Routes.Home) }
            }
        }
        DaiReceiptReviewScreen(
            state = receipt,
            currency = profileCurrency(profileState),
            datePreference = prefs.dateFormat,
            onBack = ::backToCamera,
            onChange = daiReceiptVm::update,
            onSave = daiReceiptVm::requestSave,
            onConfirm = daiReceiptVm::confirmSave,
            onCancelConfirm = daiReceiptVm::cancelConfirm,
            onErrorShown = daiReceiptVm::consumeError,
        )
    }

    // Módulo 6 · Flujo 5 · "Registro por voz": lo dicho → Dai llena "Confirmar".
    composable(Routes.NewMovementVoice) {
        DaiVoiceScreen(
            onBack = ::back,
            onHeard = { text ->
                newMovementVm.startVoice(text)
                nav.navigate(Routes.NewMovementConfirm) { popUpTo(Routes.NewMovementVoice) { inclusive = true } }
            },
            onKeyboard = {
                newMovementVm.startManual()
                nav.navigate(Routes.NewMovementManual) { popUpTo(Routes.NewMovementVoice) { inclusive = true } }
            },
            hint = stringResource(R.string.reg_voice_hint),
            keyboardLabel = stringResource(R.string.reg_voice_manual),
        )
    }

    // Módulo 6 · Flujo 5 · "Registro manual" y "Confirmar" (voz): el mismo formulario.
    listOf(Routes.NewMovementManual, Routes.NewMovementConfirm).forEach { route ->
        composable(route) {
            val register by newMovementVm.state.collectAsStateWithLifecycle()
            val event by newMovementVm.event.collectAsStateWithLifecycle()
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            val prefs by preferencesVm.state.collectAsStateWithLifecycle()
            LaunchedEffect(event) {
                if (event is NewMovementViewModel.Event.Saved) {
                    newMovementVm.consumeEvent()
                    movementsVm.refreshRecent()
                    budgetsVm.refresh()
                    nav.navigate(Routes.NewMovementSuccess) { popUpTo(Routes.Home) }
                }
            }
            NewMovementFormScreen(
                state = register,
                currency = profileCurrency(profileState),
                datePreference = prefs.dateFormat,
                onExit = ::back,
                onKind = newMovementVm::setKind,
                onAmount = newMovementVm::setAmount,
                onDate = newMovementVm::setDate,
                onCategory = newMovementVm::setCategory,
                onReason = newMovementVm::setReason,
                onSave = newMovementVm::save,
                onErrorShown = newMovementVm::consumeError,
            )
        }
    }

    // Módulo 6 · Flujo 5 · "Movimiento registrado correctamente".
    composable(Routes.NewMovementSuccess) {
        NewMovementSuccessScreen(
            onContinue = { nav.navigate(Routes.Movements) { popUpTo(Routes.Home) } },
            onHome = { nav.popBackStack(Routes.Home, inclusive = false) },
        )
    }
}
