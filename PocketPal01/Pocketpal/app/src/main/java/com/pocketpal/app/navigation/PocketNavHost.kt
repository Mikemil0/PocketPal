package com.pocketpal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketpal.app.auth.*
import com.pocketpal.app.ui.screens.*

object Routes {
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
}

@Composable
fun PocketNavHost(vm: AuthViewModel, nav: NavHostController = rememberNavController()) {
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

    NavHost(nav, startDestination = Routes.Welcome) {
        composable(Routes.Welcome) {
            WelcomeScreen(
                onGoogle = { nav.navigate(Routes.Google); startGoogle() },
                onRegister = { nav.navigate(Routes.Register) },
                onLogin = { nav.navigate(Routes.Login) },
            )
        }
        composable(Routes.Login) {
            LoginScreen(state, vm::login, ::back, { go(Routes.ForgotEmail) }, { go(Routes.Register) })
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
            val name by vm.displayName.collectAsStateWithLifecycle()
            HomeScreen(name = name, onSignOut = vm::signOut)
        }
    }
}
