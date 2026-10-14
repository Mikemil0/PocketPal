package com.pocketpal.app.di

import android.content.Context
import com.pocketpal.app.auth.SupabaseProvider
import com.pocketpal.app.data.profile.ProfileRepository
import com.pocketpal.app.data.profile.SupabaseProfileRepository
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

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(UserPreferencesStore(context.applicationContext))
}
