package com.pocketpal.app.navigation

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pocketpal.app.auth.*
import com.pocketpal.app.ui.auth.*
import com.pocketpal.app.ui.onboarding.*

/**
 * Módulo 1 · Autenticación: splash, bienvenida, login, registro, OTP, recuperación de contraseña, Google, tutorial y permisos.
 *
 * Las rutas están en [Routes]; [PocketNavHost] arma la app juntando todos los grafos.
 */
internal fun NavGraphBuilder.authGraph(
    nav: NavHostController,
    actions: PocketNavActions,
    vm: AuthViewModel,
    authState: State<AuthState>,
    otpExpiresAtState: State<Long?>,
) {
    // Atajos para que cada pantalla use los mismos nombres de siempre.
    val state by authState
    val otpExpiresAt by otpExpiresAtState
    fun back() = actions.back()
    fun go(route: String) = actions.go(route)
    fun goToPermission(startIndex: Int) = actions.goToPermission(startIndex)
    fun afterAuth() = actions.afterAuth()
    fun startGoogle() = actions.startGoogle()

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
}
