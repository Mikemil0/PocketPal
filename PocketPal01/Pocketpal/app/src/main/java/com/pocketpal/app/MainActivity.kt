package com.pocketpal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pocketpal.app.auth.AuthViewModel
import com.pocketpal.app.data.settings.UserPreferences
import com.pocketpal.app.di.AppContainer
import com.pocketpal.app.ui.budgets.BudgetDetailViewModel
import com.pocketpal.app.ui.budgets.BudgetFormViewModel
import com.pocketpal.app.ui.budgets.BudgetsViewModel
import com.pocketpal.app.ui.goals.GoalDetailViewModel
import com.pocketpal.app.ui.goals.GoalFormViewModel
import com.pocketpal.app.ui.goals.GoalsViewModel
import com.pocketpal.app.ui.movements.MovementCalendarViewModel
import com.pocketpal.app.ui.movements.MovementCategoriesViewModel
import com.pocketpal.app.ui.movements.MovementDetailViewModel
import com.pocketpal.app.ui.movements.MovementSearchViewModel
import com.pocketpal.app.ui.movements.MovementsViewModel
import com.pocketpal.app.navigation.PocketNavHost
import com.pocketpal.app.ui.profile.CategoriesViewModel
import com.pocketpal.app.ui.profile.ExportViewModel
import com.pocketpal.app.ui.profile.NotificationSettingsViewModel
import com.pocketpal.app.ui.profile.PhoneViewModel
import com.pocketpal.app.ui.profile.PreferencesViewModel
import com.pocketpal.app.ui.profile.ProfileViewModel
import com.pocketpal.app.ui.profile.SecurityViewModel
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
            // Fase 5 (D-03): la apariencia vive en DataStore (RNF-06) y se aplica al
            // instante en toda la app ("light", "dark" o "system" = Automático).
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            PocketTheme(theme = settings.theme) {
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
                // Fase 6: un ViewModel por funcionalidad (§7.2); también con scope
                // de actividad para compartirlo entre la lista y el formulario (fila 32).
                val categoriesVm: CategoriesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { CategoriesViewModel(container.categoryRepository) }
                    }
                )
                // Fase 8: exportar datos (§7.2: rango, formato y estado vacío)
                val exportVm: ExportViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ExportViewModel() }
                    }
                )
                // Fase 5: preferencias generales (§7.2); scope de actividad para que
                // el tema y el idioma compartan estado entre pantallas.
                val preferencesVm: PreferencesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PreferencesViewModel(container.settingsRepository, container.profileRepository)
                        }
                    }
                )
                // Fase 5: notificaciones (§7.2), solo preferencias (RF-B6).
                val notificationsVm: NotificationSettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { NotificationSettingsViewModel(container.notificationRepository) }
                    }
                )
                // Fase 4: seguridad (§7.2: cambiar contraseña y sesiones activas).
                val securityVm: SecurityViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { SecurityViewModel(container.securityRepository, container.deviceRepository) }
                    }
                )
                // Fase 7: teléfono (§7.2: número, envío y verificación del código SMS).
                val phoneVm: PhoneViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { PhoneViewModel(container.phoneRepository) }
                    }
                )
                // Módulo 3 · Flujo 1: lista de metas; scope de actividad para compartirla
                // entre el Home (tarjetas de metas) y la pantalla "Metas".
                val goalsVm: GoalsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GoalsViewModel(container.goalRepository) }
                    }
                )
                // Módulo 3 · Flujo 2: formulario de meta (crear) y pantalla "Meta creada".
                val goalFormVm: GoalFormViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GoalFormViewModel(container.goalRepository) }
                    }
                )
                // Módulo 3 · Flujo 3: detalle de la meta y aportes (compartido con "Agregar aporte").
                val goalDetailVm: GoalDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GoalDetailViewModel(container.goalRepository) }
                    }
                )
                // Módulo 4 · Flujo 1: lista de movimientos; compartido con "Movimientos recientes" del inicio.
                val movementsVm: MovementsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { MovementsViewModel(container.movementRepository) }
                    }
                )
                // Módulo 4 · Flujo 2: búsqueda de movimientos.
                val movementSearchVm: MovementSearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { MovementSearchViewModel(container.movementRepository) }
                    }
                )
                // Módulo 4 · Flujo 3: gastos por categoría y calendario de fechas.
                val movementCategoriesVm: MovementCategoriesViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { MovementCategoriesViewModel(container.movementRepository) }
                    }
                )
                val movementCalendarVm: MovementCalendarViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { MovementCalendarViewModel(container.movementRepository) }
                    }
                )
                // Módulo 4 · Flujo 4: detalle del movimiento (normal y recurrente).
                val movementDetailVm: MovementDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { MovementDetailViewModel(container.movementRepository) }
                    }
                )
                // Módulo 5 · Flujo 1: presupuestos; compartido con la tarjeta del inicio. El período
                // semanal empieza el día elegido en Preferencias → "Inicio de semana".
                val budgetsVm: BudgetsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            BudgetsViewModel(container.budgetRepository, weekStart = { preferencesVm.state.value.weekStart })
                        }
                    }
                )
                // Módulo 5 · Flujo 2: formulario, "Elegir categoría" y "Presupuesto creado" comparten este ViewModel.
                val budgetFormVm: BudgetFormViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            BudgetFormViewModel(container.budgetRepository, weekStart = { preferencesVm.state.value.weekStart })
                        }
                    }
                )
                // Módulo 5 · Flujo 3: detalle del presupuesto y su historial.
                val budgetDetailVm: BudgetDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            BudgetDetailViewModel(container.budgetRepository, weekStart = { preferencesVm.state.value.weekStart })
                        }
                    }
                )
                PocketNavHost(
                    vm, profileVm, categoriesVm, exportVm,
                    preferencesVm, notificationsVm, securityVm, phoneVm,
                    goalsVm = goalsVm,
                    goalFormVm = goalFormVm,
                    goalDetailVm = goalDetailVm,
                    movementsVm = movementsVm,
                    movementSearchVm = movementSearchVm,
                    movementCategoriesVm = movementCategoriesVm,
                    movementCalendarVm = movementCalendarVm,
                    movementDetailVm = movementDetailVm,
                    budgetsVm = budgetsVm,
                    budgetFormVm = budgetFormVm,
                    budgetDetailVm = budgetDetailVm,
                )
            }
        }
    }
}
