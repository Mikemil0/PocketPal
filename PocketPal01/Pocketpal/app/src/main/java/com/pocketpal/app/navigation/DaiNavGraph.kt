package com.pocketpal.app.navigation

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.dai.DaiAlertsScreen
import com.pocketpal.app.ui.dai.DaiCameraScreen
import com.pocketpal.app.ui.dai.DaiCardActions
import com.pocketpal.app.ui.dai.DaiChatScreen
import com.pocketpal.app.ui.dai.DaiChatViewModel
import com.pocketpal.app.ui.dai.DaiClearHistoryScreen
import com.pocketpal.app.ui.dai.DaiEducationScreen
import com.pocketpal.app.ui.dai.DaiGoalsScreen
import com.pocketpal.app.ui.dai.DaiInsightsViewModel
import com.pocketpal.app.ui.dai.DaiMenuButton
import com.pocketpal.app.ui.dai.DaiMenuOption
import com.pocketpal.app.ui.dai.DaiMissingDataScreen
import com.pocketpal.app.ui.dai.DaiQuickAction
import com.pocketpal.app.ui.dai.DaiQuickActionsScreen
import com.pocketpal.app.ui.dai.DaiReceiptReviewScreen
import com.pocketpal.app.ui.dai.DaiReceiptViewModel
import com.pocketpal.app.ui.dai.DaiSettingsScreen
import com.pocketpal.app.ui.dai.DaiSettingsViewModel
import com.pocketpal.app.ui.dai.DaiSpeaker
import com.pocketpal.app.ui.dai.DaiVoiceScreen
import com.pocketpal.app.ui.dai.DaiWelcomeScreen
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel

/**
 * Módulo 6 · Dai (Daikoku): bienvenida, chat, voz, configuración, factura con la cámara (OCR) y el menú ⋮ (acciones, alertas, metas, educación).
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.daiGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    context: Context,
    profileVm: ProfileViewModel,
    preferencesVm: PreferencesViewModel,
    movementsVm: MovementsViewModel,
    budgetsVm: BudgetsViewModel,
    daiChatVm: DaiChatViewModel,
    daiInsightsVm: DaiInsightsViewModel,
    daiReceiptVm: DaiReceiptViewModel,
    daiSettingsVm: DaiSettingsViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()
    fun askDai(text: String) = actions.askDai(text)
    fun openDaiMenu(option: DaiMenuOption) = actions.openDaiMenu(option)

    // Módulo 6 · Flujo 1: bienvenida de Dai → chat.
    composable(Routes.Dai) {
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        DaiWelcomeScreen(
            userName = (profileState as? ProfileViewModel.UiState.Content)?.data?.fullName.orEmpty(),
            onBack = ::back,
            onOption = { prompt ->
                daiChatVm.send(prompt)
                nav.navigate(Routes.DaiChat)
            },
            onTalk = { nav.navigate(Routes.DaiChat) },
            onVoice = { nav.navigate(Routes.DaiVoice) },
        )
    }
    composable(Routes.DaiChat) {
        LaunchedEffect(Unit) { daiChatVm.open() }
        val chatState by daiChatVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val daiSettings by daiSettingsVm.settings.collectAsStateWithLifecycle(initialValue = com.pocketpal.app.data.dai.DaiSettings())
        // Flujo 4: si "Voz" está activada, Dai lee en voz alta cada respuesta nueva.
        val lastDai = chatState.messages.lastOrNull { !it.isUser }
        DaiSpeaker(
            enabled = daiSettings.voiceReplies,
            loaded = chatState.loaded,
            messageId = lastDai?.id,
            text = lastDai?.text,
        )
        DaiChatScreen(
            state = chatState,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onSend = daiChatVm::send,
            onRetry = daiChatVm::retry,
            cardActions = DaiCardActions(
                onOpenVideo = { query -> openVideoSearch(context, query) },
                onCompleteData = { nav.navigate(Routes.DaiMissingData) },
            ),
            headerActions = { DaiMenuButton(onSelect = ::openDaiMenu, options = DaiMenuOption.entries) },
            onMic = { nav.navigate(Routes.DaiVoice) },
            onCamera = {
                daiReceiptVm.start()
                nav.navigate(Routes.DaiCamera)
            },
        )
    }

    // Módulo 6 · Flujo 4: modo voz → lo que se dijo se manda al chat.
    composable(Routes.DaiVoice) {
        fun toChat() = nav.navigate(Routes.DaiChat) {
            popUpTo(Routes.DaiVoice) { inclusive = true }
            launchSingleTop = true
        }
        DaiVoiceScreen(
            onBack = ::back,
            onHeard = { text ->
                daiChatVm.send(text)
                toChat()
            },
            onKeyboard = ::toChat,
        )
    }
    // Módulo 6 · Flujo 4: configuración de Dai y borrar historial.
    composable(Routes.DaiSettings) {
        val daiSettings by daiSettingsVm.settings.collectAsStateWithLifecycle(initialValue = com.pocketpal.app.data.dai.DaiSettings())
        DaiSettingsScreen(
            voiceReplies = daiSettings.voiceReplies,
            serverUrl = daiSettings.serverUrl,
            onBack = ::back,
            onVoiceRepliesChange = daiSettingsVm::setVoiceReplies,
            onServerUrlChange = daiSettingsVm::setServerUrl,
            onCameraSettings = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null),
                    )
                )
            },
            onClearHistory = { nav.navigate(Routes.DaiClearHistory) },
        )
    }
    composable(Routes.DaiClearHistory) {
        val deleting by daiSettingsVm.deleting.collectAsStateWithLifecycle()
        val error by daiSettingsVm.error.collectAsStateWithLifecycle()
        val event by daiSettingsVm.event.collectAsStateWithLifecycle()
        LaunchedEffect(event) {
            if (event is DaiSettingsViewModel.Event.HistoryCleared) {
                daiSettingsVm.consumeEvent()
                daiChatVm.clearLocal()
                // De vuelta al chat, ya vacío ("¿En qué puedo ayudarte hoy?").
                if (!nav.popBackStack(Routes.DaiChat, inclusive = false)) nav.popBackStack()
            }
        }
        DaiClearHistoryScreen(
            deleting = deleting,
            error = error,
            onBack = {
                daiSettingsVm.consumeError()
                nav.popBackStack()
            },
            onConfirm = daiSettingsVm::clearHistory,
        )
    }

    // Módulo 6 · Flujo 3: factura con la cámara → OCR (ML Kit) → aprobar → guardar.
    composable(Routes.DaiCamera) {
        val receipt by daiReceiptVm.state.collectAsStateWithLifecycle()
        // Cuando ya hay datos (leídos o "Manual"), se abre "Confirmar".
        LaunchedEffect(receipt.stage) {
            if (receipt.stage == DaiReceiptViewModel.Stage.Review) nav.navigate(Routes.DaiReceiptReview)
        }
        DaiCameraScreen(
            state = receipt,
            onBack = ::back,
            onReading = daiReceiptVm::onReading,
            onTextRecognized = daiReceiptVm::onTextRecognized,
            onReadFailed = daiReceiptVm::onReadFailed,
            onManual = daiReceiptVm::startManual,
            onRetake = daiReceiptVm::retake,
            onPhoto = daiReceiptVm::onPhoto,
        )
    }
    composable(Routes.DaiReceiptReview) {
        val receipt by daiReceiptVm.state.collectAsStateWithLifecycle()
        val event by daiReceiptVm.event.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        val prefs by preferencesVm.state.collectAsStateWithLifecycle()
        // Atrás = volver a la cámara para tomar otra foto (sin guardar nada).
        fun backToCamera() {
            daiReceiptVm.retake()
            nav.popBackStack()
        }
        androidx.activity.compose.BackHandler(onBack = ::backToCamera)
        LaunchedEffect(event) {
            if (event is DaiReceiptViewModel.Event.Saved) {
                daiReceiptVm.consumeEvent()
                // Movimientos se abre en el mes de la factura (puede ser de otro mes).
                movementsVm.showDate(receipt.draft.date)
                // El chat vuelve a leer el historial: ahí está la respuesta de Dai.
                daiChatVm.reload()
                budgetsVm.refresh()
                nav.popBackStack(Routes.DaiChat, inclusive = false)
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

    // Módulo 6 · Flujo 2: menú ⋮ de Dai.
    composable(Routes.DaiQuickActions) {
        DaiQuickActionsScreen(
            onBack = ::back,
            onAsk = ::askDai,
            onAction = { action ->
                when (action) {
                    DaiQuickAction.Analyze -> askDai("¿En qué estoy gastando más?")
                    DaiQuickAction.ShouldKnow -> nav.navigate(Routes.DaiAlerts)
                    DaiQuickAction.Goals -> nav.navigate(Routes.DaiGoals)
                    DaiQuickAction.Advice -> askDai("Dame un consejo")
                    DaiQuickAction.Learn -> nav.navigate(Routes.DaiLearn)
                }
            },
        )
    }
    composable(Routes.DaiAlerts) {
        LaunchedEffect(Unit) { daiInsightsVm.load() }
        val insights by daiInsightsVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        DaiAlertsScreen(
            state = insights,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onAsk = ::askDai,
            onOpenVideo = { query -> openVideoSearch(context, query) },
            onCompleteData = { nav.navigate(Routes.Budgets) },
            onRetry = daiInsightsVm::load,
        )
    }
    composable(Routes.DaiGoals) {
        LaunchedEffect(Unit) { daiInsightsVm.load() }
        val insights by daiInsightsVm.state.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        DaiGoalsScreen(
            state = insights,
            currency = profileCurrency(profileState),
            onBack = ::back,
            onAsk = ::askDai,
            onAddGoal = { nav.navigate(Routes.GoalNew) },
            onSeeGoals = { nav.navigate(Routes.Goals) },
            onRetry = daiInsightsVm::load,
        )
    }
    composable(Routes.DaiLearn) {
        val topic by daiInsightsVm.topic.collectAsStateWithLifecycle()
        val profileState by profileVm.state.collectAsStateWithLifecycle()
        DaiEducationScreen(
            topic = topic,
            lesson = daiInsightsVm.lesson(),
            currency = profileCurrency(profileState),
            onBack = ::back,
            onAsk = ::askDai,
            onSelectTopic = daiInsightsVm::selectTopic,
            onOpenVideo = { query -> openVideoSearch(context, query) },
            onApplyToGoal = { nav.navigate(Routes.Goals) },
        )
    }
    composable(Routes.DaiMissingData) {
        LaunchedEffect(Unit) { daiInsightsVm.load() }
        val insights by daiInsightsVm.state.collectAsStateWithLifecycle()
        DaiMissingDataScreen(
            missing = (insights as? DaiInsightsViewModel.UiState.Content)?.data?.missing,
            onBack = ::back,
            onAsk = ::askDai,
            onCompleteData = { nav.navigate(Routes.Budgets) },
        )
    }
}
