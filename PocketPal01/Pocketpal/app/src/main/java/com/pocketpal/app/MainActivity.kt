package com.pocketpal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pocketpal.app.auth.AuthViewModel
import com.pocketpal.app.di.AppContainer
import com.pocketpal.app.navigation.PocketNavHost
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketTheme {
                val vm: AuthViewModel = viewModel(
                    factory = viewModelFactory { initializer { AuthViewModel(container.supabase) } }
                )
                PocketNavHost(vm)
            }
        }
    }
}
