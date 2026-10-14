package com.pocketpal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pocketpal.app.auth.*
import com.pocketpal.app.data.category.CategoryKind
import com.pocketpal.app.ui.profile.CategoriesScreen
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.CategoryFormScreen
import com.pocketpal.app.ui.profile.EditProfileScreen
import com.pocketpal.app.ui.profile.ProfileHomeScreen
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

    fun categoryNew(kind: CategoryKind) = "profile/category/new/${kind.wire}"
    fun categoryDetail(id: String) = "profile/category/$id"
}

@Composable
fun PocketNavHost(
    vm: AuthViewModel,
    profileVm: ProfileViewModel,
    categoriesVm: CategoriesViewModel,
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
            LaunchedEffect(Unit) { profileVm.refresh() }
            val profileState by profileVm.state.collectAsStateWithLifecycle()
            val data = (profileState as? ProfileViewModel.UiState.Content)?.data
            HomeScreen(
                name = data?.fullName.orEmpty(),
                email = data?.email.orEmpty(),
                avatarUrl = data?.avatarUrl,
                onOpenProfile = { nav.navigate(Routes.Profile) },
                onSignOut = vm::signOut,
            )
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
                    onOpenCategories = { nav.navigate(Routes.ProfileCategories) },
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
