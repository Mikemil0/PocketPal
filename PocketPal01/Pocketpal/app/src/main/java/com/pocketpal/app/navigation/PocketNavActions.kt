package com.pocketpal.app.navigation

import android.content.Context
import androidx.navigation.NavHostController
import com.pocketpal.app.auth.AuthViewModel
import com.pocketpal.app.auth.SupabaseProvider
import com.pocketpal.app.auth.getGoogleIdToken
import com.pocketpal.app.ui.dai.DaiChatViewModel
import com.pocketpal.app.ui.dai.DaiMenuOption
import com.pocketpal.app.ui.profile.SecurityViewModel

/**
 * Acciones de navegación que usan varios grafos (volver, ir al inicio, el orden de los
 * permisos, preguntarle a Dai…). Antes eran funciones locales dentro de [PocketNavHost];
 * ahora viven aquí para que cada grafo de módulo las pueda usar.
 */
class PocketNavActions(
    private val nav: NavHostController,
    private val context: Context,
    private val authVm: AuthViewModel,
    private val securityVm: SecurityViewModel,
    private val daiChatVm: DaiChatViewModel,
) {

    fun back() {
        authVm.consume()
        nav.popBackStack()
    }

    fun go(route: String) {
        authVm.consume()
        nav.navigate(route)
    }

    /** Al inicio, borrando todo lo anterior (no se puede volver atrás al login). */
    fun goHome() {
        authVm.consume()
        nav.navigate(Routes.Home) { popUpTo(0) }
    }

    // ---------------- Dai ----------------

    /**
     * Módulo 6 · Flujo 2: lo que se escribe o se toca en las pantallas del menú de Dai se
     * manda al chat y se vuelve a él.
     */
    fun askDai(text: String) {
        daiChatVm.send(text)
        if (!nav.popBackStack(Routes.DaiChat, inclusive = false)) nav.navigate(Routes.DaiChat)
    }

    fun openDaiMenu(option: DaiMenuOption) {
        when (option) {
            DaiMenuOption.QuickActions -> nav.navigate(Routes.DaiQuickActions)
            DaiMenuOption.Alerts -> nav.navigate(Routes.DaiAlerts)
            DaiMenuOption.Goals -> nav.navigate(Routes.DaiGoals)
            DaiMenuOption.Education -> nav.navigate(Routes.DaiLearn)
            DaiMenuOption.Settings -> nav.navigate(Routes.DaiSettings)
        }
    }

    // ---------------- Inicio de sesión y permisos ----------------

    /** Permisos: solo se muestra la pantalla de los que todavía NO están concedidos. */
    private val permissionSteps = listOf(
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
            if (!authVm.isOnboardingDone()) authVm.completeOnboarding()
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
        authVm.consume()
        // §8.5: al entrar con sesión se registra o actualiza este dispositivo.
        securityVm.registerCurrentDevice()
        if (authVm.isOnboardingDone()) goToPermission(0) else nav.navigate(Routes.Tutorial)
    }

    fun startGoogle() = authVm.signInWithGoogle { getGoogleIdToken(context, SupabaseProvider.GOOGLE_WEB_CLIENT_ID) }
}
