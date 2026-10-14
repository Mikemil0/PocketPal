package com.pocketpal.app.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de preferencias para las ViewModels (RNF-02): permite probarlas con datos
 * simulados. La fuente local es DataStore; la sincronización con la cuenta
 * (`public.profiles`) se conecta en la Fase 5 (RNF-06).
 */
interface SettingsRepository {

    val settings: Flow<UserPreferences>

    suspend fun setLanguage(tag: String)
    suspend fun setTheme(theme: String)
    suspend fun setCurrency(currency: String)
    suspend fun setDateFormat(format: String)
    suspend fun setWeekStart(day: Int)

    /** D-13: persiste la casilla "Recordarme" del login. */
    suspend fun setRememberMe(remember: Boolean)

    /** Lectura puntual para el splash: ¿hay que restaurar la sesión al abrir? */
    suspend fun isRememberMe(): Boolean
}

/** Implementación real: DataStore (preferencias locales del dispositivo). */
class DataStoreSettingsRepository(private val store: UserPreferencesStore) : SettingsRepository {

    override val settings: Flow<UserPreferences> get() = store.preferences

    override suspend fun setLanguage(tag: String) = store.setLanguage(tag)
    override suspend fun setTheme(theme: String) = store.setTheme(theme)
    override suspend fun setCurrency(currency: String) = store.setCurrency(currency)
    override suspend fun setDateFormat(format: String) = store.setDateFormat(format)
    override suspend fun setWeekStart(day: Int) = store.setWeekStart(day)
    override suspend fun setRememberMe(remember: Boolean) = store.setRememberMe(remember)
    override suspend fun isRememberMe(): Boolean = store.rememberMe()
}
