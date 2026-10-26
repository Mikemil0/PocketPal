package com.pocketpal.app.di

import android.content.Context
import android.os.Build
import com.pocketpal.app.auth.SupabaseProvider
import com.pocketpal.app.data.category.CategoryRepository
import com.pocketpal.app.data.category.SupabaseCategoryRepository
import com.pocketpal.app.data.device.DeviceRepository
import com.pocketpal.app.data.device.SupabaseDeviceRepository
import com.pocketpal.app.data.goal.GoalRepository
import com.pocketpal.app.data.goal.SupabaseGoalRepository
import com.pocketpal.app.data.movement.MovementRepository
import com.pocketpal.app.data.movement.SupabaseMovementRepository
import com.pocketpal.app.data.notification.NotificationSettingsRepository
import com.pocketpal.app.data.notification.SupabaseNotificationSettingsRepository
import com.pocketpal.app.data.phone.PhoneRepository
import com.pocketpal.app.data.phone.SupabasePhoneRepository
import com.pocketpal.app.data.profile.ProfileRepository
import com.pocketpal.app.data.profile.SupabaseProfileRepository
import com.pocketpal.app.data.security.SecurityRepository
import com.pocketpal.app.data.security.SupabaseSecurityRepository
import com.pocketpal.app.data.settings.DataStoreSettingsRepository
import com.pocketpal.app.data.settings.SettingsRepository
import com.pocketpal.app.data.settings.UserPreferencesStore
import io.github.jan.supabase.SupabaseClient

/**
 * Contenedor de dependencias con **inyección manual, sin Hilt** (decisión §7.4:
 * el proyecto tiene pocas dependencias y Hilt añadiría complejidad innecesaria).
 *
 * Crea el cliente Supabase y los repositorios **una sola vez** y los entrega a las
 * fábricas de ViewModel. Los repositorios se exponen como **interfaces** para poder
 * sustituirlos en pruebas (RNF-02).
 */
class AppContainer(context: Context) {

    val supabase: SupabaseClient = SupabaseProvider.client

    val profileRepository: ProfileRepository = SupabaseProfileRepository(supabase)

    /** Fase 6: categorías (RF-B7) sobre la tabla `categories` con RLS propia. */
    val categoryRepository: CategoryRepository = SupabaseCategoryRepository(supabase)

    /** Fase 5: preferencias de notificación (RF-B6) sobre `notification_settings`. */
    val notificationRepository: NotificationSettingsRepository =
        SupabaseNotificationSettingsRepository(supabase)

    /** Fase 4: cambio de contraseña (RF-B4, §8.6). */
    val securityRepository: SecurityRepository = SupabaseSecurityRepository(supabase)

    /** Fase 4: sesiones activas (RF-B5, D-01) sobre `user_devices`. */
    val deviceRepository: DeviceRepository = SupabaseDeviceRepository(
        supabase,
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
    )

    /** Fase 7: verificación de teléfono por SMS (RF-B3). */
    val phoneRepository: PhoneRepository = SupabasePhoneRepository(supabase)

    /** Módulo 3: metas de ahorro y sus aportes sobre `goals` y `goal_contributions`. */
    val goalRepository: GoalRepository = SupabaseGoalRepository(supabase)

    /** Módulo 4: movimientos (gastos e ingresos) sobre `movements`; solo lectura por ahora. */
    val movementRepository: MovementRepository = SupabaseMovementRepository(supabase)

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(UserPreferencesStore(context.applicationContext))
}
