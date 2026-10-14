package com.pocketpal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pocketpal.app.auth.AuthViewModel
import com.pocketpal.app.di.AppContainer
import com.pocketpal.app.navigation.PocketNavHost
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.theme.PocketTheme

/**
 * RF-A3: extiende `AppCompatActivity` (que a su vez es `ComponentActivity`, así `setContent`
 * sigue funcionando) porque `AppCompatDelegate.setApplicationLocales(...)` lo exige por debajo
 * de API 33 para cambiar el idioma en ejecución.
 */
class MainActivity : AppCompatActivity() {

    /** Contenedor de dependencias: se crea una sola vez por actividad (§7.4, DI manual). */
    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // RF-A4 / D-16: splash del sistema; installSplashScreen() aplica
        // postSplashScreenTheme (Theme.Pocketpal) antes de super.onCreate().
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketTheme {
                val vm: AuthViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AuthViewModel(container.supabase, container.settingsRepository) }
                    }
                )
                // §7.2: una sola instancia compartida entre el Home y las pantallas
                // del perfil (mismo ViewModelStore de la actividad que el AppContainer).
                val profileVm: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ProfileViewModel(container.profileRepository) }
                    }
                )
                PocketNavHost(vm, profileVm)
            }
        }
    }
}
