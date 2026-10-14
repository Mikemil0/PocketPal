package com.pocketpal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pocketpal.app.auth.AuthViewModel
import com.pocketpal.app.auth.SupabaseProvider
import com.pocketpal.app.navigation.PocketNavHost
import com.pocketpal.app.ui.theme.PocketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketTheme {
                val vm: AuthViewModel = viewModel(
                    factory = viewModelFactory { initializer { AuthViewModel(SupabaseProvider.client) } }
                )
                PocketNavHost(vm)
            }
        }
    }
}
