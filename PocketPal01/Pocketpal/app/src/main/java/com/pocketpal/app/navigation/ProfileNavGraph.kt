package com.pocketpal.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.pocketpal.app.BuildConfig
import com.pocketpal.app.auth.*
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.ui.profile.AboutLicensesScreen
import com.pocketpal.app.ui.profile.AboutScreen
import com.pocketpal.app.ui.profile.CategoriesScreen
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.ChangePasswordScreen
import com.pocketpal.app.ui.profile.CurrencyScreen
import com.pocketpal.app.ui.profile.DateFormatScreen
import com.pocketpal.app.ui.profile.EditProfileScreen
import com.pocketpal.app.ui.profile.ExportScreen
import com.pocketpal.app.ui.profile.ExportViewModel
import com.pocketpal.app.ui.profile.HelpDetailScreen
import com.pocketpal.app.ui.profile.HelpScreen
import com.pocketpal.app.ui.profile.LanguageScreen
import com.pocketpal.app.ui.profile.NotificationSettingsViewModel
import com.pocketpal.app.ui.profile.NotificationsScreen
import com.pocketpal.app.ui.profile.PhoneCodeScreen
import com.pocketpal.app.ui.profile.PhoneScreen
import com.pocketpal.app.ui.profile.PhoneViewModel
import com.pocketpal.app.ui.profile.PreferencesScreen
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileHomeScreen
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.profile.SecurityScreen
import com.pocketpal.app.ui.profile.SecurityViewModel

/**
 * Módulo 2 · Perfil: datos personales, foto, teléfono, seguridad, notificaciones, categorías, preferencias, exportar, ayuda y acerca de.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.profileGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    vm: AuthViewModel,
    profileVm: ProfileViewModel,
    categoriesVm: CategoriesViewModel,
    exportVm: ExportViewModel,
    preferencesVm: PreferencesViewModel,
    notificationsVm: NotificationSettingsViewModel,
    securityVm: SecurityViewModel,
    phoneVm: PhoneViewModel,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    fun back() = actions.back()
    fun go(route: String) = actions.go(route)

    // §7.5: grafo anidado del módulo Perfil. ProfileViewModel es único y
    // compartido con el Home (§7.2) para que el saludo y "Mi perfil" coincidan.
    navigation(startDestination = Routes.Profile, route = Routes.ProfileGraph) {
        composable(Routes.Profile) {
            LaunchedEffect(Unit) { profileVm.refresh() }
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            val error by profileVm.error.collectAsStateWithLifecycle()
            ProfileHomeScreen(
                state = profileState,
                error = error,
                onBack = ::back,
                onOpenPersonalData = { nav.navigate(Routes.ProfileEdit) },
                onOpenSecurity = { nav.navigate(Routes.ProfileSecurity) },
                onOpenCategories = { nav.navigate(Routes.ProfileCategories) },
                onOpenExport = { nav.navigate(Routes.ProfileExport) },
                onOpenHelp = { nav.navigate(Routes.ProfileHelp) },
                onOpenAbout = { nav.navigate(Routes.ProfileAbout) },
                onOpenNotifications = { nav.navigate(Routes.ProfileNotifications) },
                onOpenPreferences = { nav.navigate(Routes.ProfilePreferences) },
                onSignOut = vm::signOut,
                onAvatarBytes = profileVm::setAvatar,
                onRemoveAvatar = profileVm::removeAvatar,
                onErrorShown = profileVm::consumeError,
            )
        }
        composable(Routes.ProfileEdit) {
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            val error by profileVm.error.collectAsStateWithLifecycle()
            val saving by profileVm.saving.collectAsStateWithLifecycle()
            val event by profileVm.event.collectAsStateWithLifecycle()
            EditProfileScreen(
                state = profileState,
                error = error,
                saving = saving,
                event = event,
                onBack = ::back,
                onSave = profileVm::savePersonalData,
                phone = (profileState as? ProfileViewModel.UiState.Content)?.data?.phone,
                onOpenPhone = { nav.navigate(Routes.ProfilePhone) },
                onAvatarBytes = profileVm::setAvatar,
                onRemoveAvatar = profileVm::removeAvatar,
                onErrorShown = profileVm::consumeError,
                onEventConsumed = profileVm::consumeEvent,
            )
        }

        // Fase 6 (RF-B7): categorías con pestañas, crear/editar/eliminar (§7.5).
        composable(Routes.ProfileCategories) {
            LaunchedEffect(Unit) { categoriesVm.refresh() }
            val categoriesState by categoriesVm.state.collectAsStateWithLifecycle()
            val categoriesError by categoriesVm.error.collectAsStateWithLifecycle()
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            CategoriesScreen(
                state = categoriesState,
                error = categoriesError,
                currency = profileCurrency(profileState),
                onBack = ::back,
                onSelectKind = categoriesVm::selectKind,
                onMove = categoriesVm::move,
                onCreate = { nav.navigate(Routes.categoryNew(it)) },
                onOpen = { nav.navigate(Routes.categoryDetail(it)) },
                onErrorShown = categoriesVm::consumeError,
            )
        }
        composable(
            Routes.ProfileCategoryNew,
            arguments = listOf(navArgument("kind") { type = NavType.StringType }),
        ) { entry ->
            CategoryFormRoute(
                categoryId = null,
                requestedKind = CategoryKind.fromWire(entry.arguments?.getString("kind").orEmpty()),
                categoriesVm = categoriesVm,
                profileVm = profileVm,
                onBack = ::back,
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            Routes.ProfileCategoryDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            CategoryFormRoute(
                categoryId = entry.arguments?.getString("id").orEmpty(),
                requestedKind = CategoryKind.Expense,
                categoriesVm = categoriesVm,
                profileVm = profileVm,
                onBack = ::back,
                onDone = { nav.popBackStack() },
            )
        }

        // Fase 8 (RF-B9 / D-05): exportar datos, solo UI y estados.
        composable(Routes.ProfileExport) {
            val exportState by exportVm.state.collectAsStateWithLifecycle()
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            ExportScreen(
                state = exportState,
                email = (profileState as? ProfileViewModel.UiState.Content)?.data?.email.orEmpty(),
                onBack = ::back,
                onSelectRange = exportVm::selectRange,
                onSelectFormat = exportVm::selectFormat,
                onCustomRange = exportVm::setCustomRange,
            )
        }

        // Fase 8 (RF-B10): ayuda y soporte (acordeón) y detalle de pregunta.
        composable(Routes.ProfileHelp) {
            HelpScreen(
                supportEmail = BuildConfig.SUPPORT_EMAIL,
                onBack = ::back,
                onOpenDetail = { nav.navigate(Routes.helpDetail(it)) },
            )
        }
        composable(
            Routes.ProfileHelpDetail,
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { entry ->
            // Anterior/Siguiente cambian el índice local: un solo destino en la pila.
            var index by rememberSaveable { mutableStateOf(entry.arguments?.getInt("index") ?: 0) }
            HelpDetailScreen(
                index = index,
                supportEmail = BuildConfig.SUPPORT_EMAIL,
                onBack = ::back,
                onPrev = { index = it },
                onNext = { index = it },
            )
        }

        // Fase 8 (RF-B11): acerca de y sus licencias.
        composable(Routes.ProfileAbout) {
            AboutScreen(
                version = BuildConfig.VERSION_NAME,
                termsUrl = BuildConfig.TERMS_URL,
                privacyUrl = BuildConfig.PRIVACY_URL,
                facebookUrl = BuildConfig.FACEBOOK_URL,
                instagramUrl = BuildConfig.INSTAGRAM_URL,
                xUrl = BuildConfig.X_URL,
                onBack = ::back,
                onOpenLicenses = { nav.navigate(Routes.ProfileAboutLicenses) },
            )
        }
        composable(Routes.ProfileAboutLicenses) {
            AboutLicensesScreen(onBack = ::back)
        }

        // Fase 5 (RF-B6): notificaciones, solo preferencias (sin envíos reales).
        composable(Routes.ProfileNotifications) {
            LaunchedEffect(Unit) { notificationsVm.refresh() }
            val nState by notificationsVm.state.collectAsStateWithLifecycle()
            val nError by notificationsVm.error.collectAsStateWithLifecycle()
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            NotificationsScreen(
                state = nState,
                error = nError,
                smsEnabled = !(profileState as? ProfileViewModel.UiState.Content)?.data?.phone.isNullOrBlank(),
                onBack = ::back,
                onToggleDailyReminder = notificationsVm::toggleDailyReminder,
                onToggleWeeklySummary = notificationsVm::toggleWeeklySummary,
                onToggleBudgetAlert = notificationsVm::toggleBudgetAlert,
                onToggleGoalsProgress = notificationsVm::toggleGoalsProgress,
                onToggleChannelEmail = notificationsVm::toggleChannelEmail,
                onToggleChannelSms = notificationsVm::toggleChannelSms,
                onThresholdChange = notificationsVm::setBudgetThreshold,
                onErrorShown = notificationsVm::consumeError,
            )
        }

        // Fase 5 (RF-B8): preferencias generales y sus selectores.
        composable(Routes.ProfilePreferences) {
            val pState by preferencesVm.state.collectAsStateWithLifecycle()
            val pError by preferencesVm.error.collectAsStateWithLifecycle()
            PreferencesScreen(
                state = pState,
                error = pError,
                onBack = ::back,
                onOpenCurrency = { nav.navigate(Routes.ProfileCurrency) },
                onOpenLanguage = { nav.navigate(Routes.ProfileLanguage) },
                onOpenDateFormat = { nav.navigate(Routes.ProfileDateFormat) },
                onThemeChange = preferencesVm::setTheme,
                onWeekStartChange = preferencesVm::setWeekStart,
                onErrorShown = preferencesVm::consumeError,
            )
        }
        composable(Routes.ProfileCurrency) {
            val pState by preferencesVm.state.collectAsStateWithLifecycle()
            CurrencyScreen(
                current = pState.currency,
                onBack = ::back,
                onSave = { preferencesVm.setCurrency(it); nav.popBackStack() },
            )
        }
        composable(Routes.ProfileLanguage) {
            val pState by preferencesVm.state.collectAsStateWithLifecycle()
            LanguageScreen(
                current = pState.language,
                onBack = ::back,
                onSave = { preferencesVm.setLanguage(it); nav.popBackStack() },
            )
        }
        composable(Routes.ProfileDateFormat) {
            val pState by preferencesVm.state.collectAsStateWithLifecycle()
            DateFormatScreen(
                current = pState.dateFormat,
                onBack = ::back,
                onSave = { preferencesVm.setDateFormat(it); nav.popBackStack() },
            )
        }

        // Fase 4 (RF-B4 / RF-B5): seguridad, cambiar contraseña y sesiones activas.
        composable(Routes.ProfileSecurity) {
            LaunchedEffect(Unit) { securityVm.refresh() }
            val sState by securityVm.state.collectAsStateWithLifecycle()
            val sError by securityVm.error.collectAsStateWithLifecycle()
            SecurityScreen(
                state = sState,
                error = sError,
                onBack = ::back,
                onChangePassword = { nav.navigate(Routes.ProfilePassword) },
                onRevokeDevice = securityVm::revokeDevice,
                onErrorShown = securityVm::consumeError,
            )
        }
        composable(Routes.ProfilePassword) {
            val saving by securityVm.saving.collectAsStateWithLifecycle()
            val formError by securityVm.formError.collectAsStateWithLifecycle()
            val pError by securityVm.error.collectAsStateWithLifecycle()
            val event by securityVm.event.collectAsStateWithLifecycle()
            LaunchedEffect(event) {
                if (event == SecurityViewModel.Event.PasswordChanged) {
                    securityVm.consumeEvent()
                    securityVm.refresh()
                    nav.popBackStack()
                }
            }
            ChangePasswordScreen(
                saving = saving,
                formError = formError,
                error = pError,
                onBack = ::back,
                onChangePassword = securityVm::changePassword,
                onForgotPassword = { go(Routes.ForgotEmail) },
                onFormErrorShown = securityVm::consumeFormError,
                onErrorShown = securityVm::consumeError,
            )
        }

        // Fase 7 (RF-B3 / RN-06): teléfono con verificación por SMS.
        composable(Routes.ProfilePhone) {
            val pState by phoneVm.state.collectAsStateWithLifecycle()
            val pError by phoneVm.error.collectAsStateWithLifecycle()
            val fError by phoneVm.formError.collectAsStateWithLifecycle()
            val event by phoneVm.event.collectAsStateWithLifecycle()
            LaunchedEffect(event) {
                if (event == PhoneViewModel.Event.CodeSent) {
                    phoneVm.consumeEvent()
                    nav.navigate(Routes.ProfilePhoneCode)
                } else if (event != null) {
                    phoneVm.consumeEvent()
                }
            }
            PhoneScreen(
                prefix = pState.prefix,
                number = pState.number,
                sending = pState.sending,
                formError = fError,
                error = pError,
                onBack = ::back,
                onPrefixChange = phoneVm::setPrefix,
                onNumberChange = phoneVm::setNumber,
                onNext = phoneVm::sendCode,
                onFormErrorShown = phoneVm::consumeFormError,
                onErrorShown = phoneVm::consumeError,
            )
        }
        composable(Routes.ProfilePhoneCode) {
            val pState by phoneVm.state.collectAsStateWithLifecycle()
            val expires by phoneVm.codeExpiresAt.collectAsStateWithLifecycle()
            val pError by phoneVm.error.collectAsStateWithLifecycle()
            val fError by phoneVm.formError.collectAsStateWithLifecycle()
            val event by phoneVm.event.collectAsStateWithLifecycle()
            LaunchedEffect(event) {
                when (event) {
                    PhoneViewModel.Event.Verified -> {
                        phoneVm.consumeEvent()
                        profileVm.refresh()
                        nav.popBackStack()
                    }
                    PhoneViewModel.Event.CodeResent -> phoneVm.consumeEvent()
                    else -> Unit
                }
            }
            PhoneCodeScreen(
                prefix = pState.prefix,
                number = pState.number,
                code = pState.code,
                codeExpiresAt = expires,
                sending = pState.sending,
                verifying = pState.verifying,
                formError = fError,
                error = pError,
                onBack = ::back,
                onCodeChange = phoneVm::setCode,
                onVerify = phoneVm::verifyCode,
                onResend = phoneVm::resend,
                onNotYourNumber = { phoneVm.backToNumber(); nav.popBackStack() },
                onFormErrorShown = phoneVm::consumeFormError,
                onErrorShown = phoneVm::consumeError,
            )
        }
    }
}
