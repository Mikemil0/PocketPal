package com.pocketpal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketpal.app.BuildConfig
import com.pocketpal.app.auth.*
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.ui.goals.AddContributionScreen
import com.pocketpal.app.ui.goals.GoalCreatedScreen
import com.pocketpal.app.ui.goals.GoalDetailScreen
import com.pocketpal.app.ui.goals.GoalDetailViewModel
import com.pocketpal.app.ui.goals.GoalFormScreen
import com.pocketpal.app.ui.goals.GoalFormViewModel
import com.pocketpal.app.ui.goals.GoalsScreen
import com.pocketpal.app.ui.goals.GoalsViewModel
import com.pocketpal.app.ui.profile.AboutLicensesScreen
import com.pocketpal.app.ui.profile.AboutScreen
import com.pocketpal.app.ui.profile.CategoriesScreen
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.CategoryFormScreen
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
import com.pocketpal.app.ui.profile.SecurityScreen
import com.pocketpal.app.ui.profile.SecurityViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.screens.*

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Login = "login"
    const val Register = "register"
    const val OtpSignup = "otp_signup"
    const val ForgotEmail = "forgot_email"
    const val ForgotOtp = "forgot_otp"
    const val ForgotNew = "forgot_new"
    const val ForgotDone = "forgot_done"
    const val PermNotif = "perm_notif"
    const val PermCamera = "perm_camera"
    const val PermMic = "perm_mic"
    const val Google = "google"
    const val NoWifi = "no_wifi"
    const val SessionExpired = "session_expired"
    const val Home = "home"
    const val AccountCreated = "account_created"
    const val Tutorial = "tutorial"
    const val ProfileGraph = "profile_graph"
    const val Profile = "profile"
    const val ProfileEdit = "profile/edit"
    const val ProfileCategories = "profile/categories"
    const val ProfileCategoryNew = "profile/category/new/{kind}"
    const val ProfileCategoryDetail = "profile/category/{id}"

    // Fase 8 (§7.5): exportar, ayuda y acerca de
    const val ProfileExport = "profile/export"
    const val ProfileHelp = "profile/help"
    const val ProfileHelpDetail = "profile/help/{index}"
    const val ProfileAbout = "profile/about"
    const val ProfileAboutLicenses = "profile/about/licenses"

    // Fase 5 (§7.5): notificaciones y preferencias generales
    const val ProfileNotifications = "profile/notifications"
    const val ProfilePreferences = "profile/preferences"
    const val ProfileCurrency = "profile/currency"
    const val ProfileLanguage = "profile/language"
    const val ProfileDateFormat = "profile/date-format"

    // Fase 4 y 7 (§7.5): seguridad y teléfono
    const val ProfileSecurity = "profile/security"
    const val ProfilePassword = "profile/password"
    const val ProfilePhone = "profile/phone"
    const val ProfilePhoneCode = "profile/phone/code"

    // Módulo 3 · Metas
    const val Goals = "goals"
    const val GoalNew = "goals/create"
    const val GoalCreated = "goals/created"
    const val GoalDetail = "goals/detail/{id}"
    const val GoalContribute = "goals/detail/{id}/contribute"

    fun categoryNew(kind: CategoryKind) = "profile/category/new/${kind.wire}"
    fun categoryDetail(id: String) = "profile/category/$id"
    fun helpDetail(index: Int) = "profile/help/$index"
    fun goalDetail(id: String) = "goals/detail/$id"
    fun goalContribute(id: String) = "goals/detail/$id/contribute"
}

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
    nav: NavHostController = rememberNavController(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val otpExpiresAt by vm.otpExpiresAt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun back() { vm.consume(); nav.popBackStack() }
    fun go(route: String) { vm.consume(); nav.navigate(route) }
    fun goHome() { vm.consume(); nav.navigate(Routes.Home) { popUpTo(0) } }

    // Permisos: solo se muestra la pantalla de los que todavía NO están concedidos.
    val permissionSteps = listOf(
        Routes.PermNotif to AppPermissions.notifications,
        Routes.PermCamera to AppPermissions.camera,
        Routes.PermMic to AppPermissions.microphone,
    )
    fun goToPermission(startIndex: Int) {
        // La pantalla de la que venimos ya se mostró en este teléfono: no se repite.
        if (startIndex > 0) AppPermissions.markPrompted(context, permissionSteps[startIndex - 1].first)
        val next = (startIndex until permissionSteps.size).firstOrNull {
            val (route, permission) = permissionSteps[it]
            !AppPermissions.isGranted(context, permission) && !AppPermissions.wasPrompted(context, route)
        }
        if (next == null) {
            if (!vm.isOnboardingDone()) vm.completeOnboarding()
            goHome()
        } else {
            nav.navigate(permissionSteps[next].first)
        }
    }

    /**
     * Después de iniciar sesión:
     * - Cuenta nueva: tutorial (solo se ve una vez por cuenta) y luego permisos.
     * - Cuenta que ya vio el tutorial: sin tutorial, pero pide los permisos que falten en ESTE teléfono.
     */
    fun afterAuth() {
        vm.consume()
        // §8.5: al entrar con sesión se registra o actualiza este dispositivo.
        securityVm.registerCurrentDevice()
        if (vm.isOnboardingDone()) goToPermission(0) else nav.navigate(Routes.Tutorial)
    }
    fun startGoogle() = vm.signInWithGoogle { getGoogleIdToken(context, SupabaseProvider.GOOGLE_WEB_CLIENT_ID) }

    // Un solo lugar decide la navegación a partir del estado del ViewModel.
    LaunchedEffect(state) {
        when (val s = state) {
            is AuthState.Success<*> -> when (s.step) {
                AuthStep.LoggedIn -> afterAuth()
                AuthStep.SignupOtpSent -> go(Routes.OtpSignup)
                AuthStep.EmailVerified -> go(Routes.AccountCreated)
                AuthStep.RecoveryCodeSent -> go(Routes.ForgotOtp)
                AuthStep.RecoveryCodeVerified -> go(Routes.ForgotNew)
                AuthStep.PasswordUpdated -> go(Routes.ForgotDone)
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
        // RF-A4 / D-13: el splash decide a dónde entrar según la sesión guardada
        // y la casilla "Recordarme" (sin marcar, la sesión no se restaura).
        composable(Routes.Splash) {
            SplashScreen(
                resolveSession = { vm.restoreSession() },
                onDecided = { toHome ->
                    vm.consume()
                    nav.navigate(if (toHome) Routes.Home else Routes.Welcome) { popUpTo(0) }
                },
            )
        }
        composable(Routes.Welcome) {
            WelcomeScreen(
                onGoogle = { nav.navigate(Routes.Google); startGoogle() },
                onRegister = { nav.navigate(Routes.Register) },
                onLogin = { nav.navigate(Routes.Login) },
            )
        }
        composable(Routes.Login) {
            val rememberMe by vm.rememberMe.collectAsStateWithLifecycle()
            LoginScreen(state, vm::login, ::back, { go(Routes.ForgotEmail) }, { go(Routes.Register) }, rememberMe)
        }
        composable(Routes.Register) {
            RegisterScreen(state, vm::register, ::back, { go(Routes.Login) })
        }
        composable(Routes.OtpSignup) {
            OtpScreen(OtpMode.Signup, vm.pendingEmail, state, otpExpiresAt,
                onVerify = { vm.verifyOtp(OtpMode.Signup, it) },
                onResend = { vm.resendOtp(OtpMode.Signup) }, onBack = ::back)
        }
        composable(Routes.ForgotEmail) {
            ForgotEmailScreen(state, vm::sendRecoveryCode, ::back, { go(Routes.Login) })
        }
        composable(Routes.ForgotOtp) {
            OtpScreen(OtpMode.Recovery, vm.pendingEmail, state, otpExpiresAt,
                onVerify = { vm.verifyOtp(OtpMode.Recovery, it) },
                onResend = { vm.resendOtp(OtpMode.Recovery) }, onBack = ::back)
        }
        composable(Routes.ForgotNew) { NewPasswordScreen(state, vm::updatePassword) }
        composable(Routes.ForgotDone) {
            PasswordUpdatedScreen(onLogin = {
                vm.consume()
                nav.navigate(Routes.Login) { popUpTo(Routes.Welcome) }
            })
        }
        composable(Routes.PermNotif) { NotificationPermissionScreen { goToPermission(1) } }
        composable(Routes.PermCamera) { CameraPermissionScreen { goToPermission(2) } }
        composable(Routes.PermMic) { MicrophonePermissionScreen { goToPermission(3) } }
        composable(Routes.Google) {
            GoogleAuthScreen(
                state,
                onContinue = ::afterAuth,
                onRetry = ::startGoogle,
                onUseEmail = { vm.consume(); nav.navigate(Routes.Login) { popUpTo(Routes.Welcome) } },
            )
        }
        composable(Routes.NoWifi) { NoWifiScreen(onRetry = ::back) }
        composable(Routes.SessionExpired) {
            SessionExpiredScreen(
                email = vm.lastKnownEmail,
                onLogin = { vm.consume(); nav.navigate(Routes.Login) { popUpTo(0) } },
                onOtherAccount = { vm.consume(); nav.navigate(Routes.Welcome) { popUpTo(0) } },
            )
        }
        composable(Routes.AccountCreated) { AccountCreatedScreen(onContinue = { nav.navigate(Routes.Tutorial) }) }
        composable(Routes.Tutorial) { TutorialScreen(onFinish = { goToPermission(0) }) }
        composable(Routes.Home) {
            LaunchedEffect(Unit) {
                profileVm.refresh()
                securityVm.registerCurrentDevice()
                goalsVm.refresh()
            }
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            val goalsState by goalsVm.state.collectAsStateWithLifecycle()
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
            )
        }

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
}

/** Moneda del perfil para formatear montos (RN-09); COP mientras no cargue. */
@Composable
private fun profileCurrency(state: ProfileViewModel.UiState): String =
    (state as? ProfileViewModel.UiState.Content)?.data?.currency ?: "COP"

/** Formulario crear/editar: resuelve la fila inicial y las de su tipo (RN-02). */
@Composable
private fun CategoryFormRoute(
    categoryId: String?,
    requestedKind: CategoryKind,
    categoriesVm: CategoriesViewModel,
    profileVm: ProfileViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    // Carga defensiva: si se entra sin lista (deep link), el formulario la pide.
    LaunchedEffect(Unit) { categoriesVm.refresh() }
    val categoriesState by categoriesVm.state.collectAsStateWithLifecycle()
    val saving by categoriesVm.saving.collectAsStateWithLifecycle()
    val error by categoriesVm.error.collectAsStateWithLifecycle()
    val event by categoriesVm.event.collectAsStateWithLifecycle()
    val profileState by profileVm.state.collectAsStateWithLifecycle()
    val initial = categoriesVm.find(categoryId.orEmpty())
    val kindForList = initial?.kind ?: requestedKind
    val existing = (categoriesState as? CategoriesViewModel.UiState.Content)?.data?.let {
        if (kindForList == CategoryKind.Income) it.incomes else it.expenses
    } ?: emptyList()
    CategoryFormScreen(
        categoryId = categoryId,
        requestedKind = requestedKind,
        initial = initial,
        existing = existing,
        currency = profileCurrency(profileState),
        saving = saving,
        error = error,
        event = event,
        onBack = onBack,
        onSave = categoriesVm::save,
        onDelete = categoriesVm::delete,
        onErrorShown = categoriesVm::consumeError,
        onEventConsumed = categoriesVm::consumeEvent,
        onDone = onDone,
    )
}
