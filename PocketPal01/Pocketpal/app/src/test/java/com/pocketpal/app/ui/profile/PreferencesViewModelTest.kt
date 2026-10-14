package com.pocketpal.app.ui.profile

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.profile.Profile
import com.pocketpal.app.data.profile.ProfileRepository
import com.pocketpal.app.data.profile.ProfileStats
import com.pocketpal.app.data.settings.SettingsRepository
import com.pocketpal.app.data.settings.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fase 5 (RF-B8 / RNF-06) sobre [PreferencesViewModel]: cada cambio se guarda en
 * DataStore y en la cuenta, y al abrir **manda la cuenta** (decisión 3-A), con la
 * excepción del idioma "Automático" (preferencia del dispositivo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    private class FakeSettings : SettingsRepository {
        val stored = MutableStateFlow(UserPreferences())
        override val settings: Flow<UserPreferences> get() = stored
        override suspend fun setLanguage(tag: String) { stored.value = stored.value.copy(language = tag) }
        override suspend fun setTheme(theme: String) { stored.value = stored.value.copy(theme = theme) }
        override suspend fun setCurrency(currency: String) { stored.value = stored.value.copy(currency = currency) }
        override suspend fun setDateFormat(format: String) { stored.value = stored.value.copy(dateFormat = format) }
        override suspend fun setWeekStart(day: Int) { stored.value = stored.value.copy(weekStart = day) }
        override suspend fun setRememberMe(remember: Boolean) { stored.value = stored.value.copy(rememberMe = remember) }
        override suspend fun isRememberMe(): Boolean = stored.value.rememberMe
    }

    private class FakeProfileRepository : ProfileRepository {
        var profile: Profile? = null
        var failSave = false
        var lastSavedLanguage: String? = null
        override suspend fun getMyProfile(): Profile? = profile
        override suspend fun getStats() = ProfileStats()
        override fun currentEmail(): String? = "user@test.com"
        override fun currentPhone(): String? = null
        override suspend fun savePersonalData(fullName: String, country: String, currency: String): Profile? = profile
        override suspend fun savePreferences(
            language: String,
            theme: String,
            currency: String,
            dateFormat: String,
            weekStart: Int,
        ): Profile? {
            if (failSave) throw java.io.IOException("sin red")
            lastSavedLanguage = language
            profile = profile?.copy(
                language = if (language.isBlank()) profile?.language ?: "es" else language,
                theme = theme,
                currency = currency,
                dateFormat = dateFormat,
                weekStart = weekStart,
            )
            return profile
        }
        override suspend fun updateAvatar(imageBytes: ByteArray): Profile? = profile
        override suspend fun removeAvatar(): Profile? = profile
        override fun avatarUrl(avatarPath: String?): String? = null
    }

    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(main) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun profile(
        language: String = "es",
        theme: String = "system",
        currency: String = "COP",
        dateFormat: String = "DD/MM/YYYY",
        weekStart: Int = 0,
    ) = Profile(id = "u1", fullName = "Test", language = language, theme = theme, currency = currency, dateFormat = dateFormat, weekStart = weekStart)

    @Test
    fun `cambiar tema lo guarda local y en la cuenta`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply { profile = profile() }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        vm.setTheme("dark")
        assertEquals("dark", vm.state.value.theme)
        assertEquals("dark", settings.stored.value.theme)
        assertEquals("dark", repo.profile?.theme)
    }

    @Test
    fun `la cuenta manda al abrir la app`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply {
            profile = profile(theme = "dark", currency = "EUR", weekStart = 1)
        }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        val state = vm.state.first { it.theme == "dark" }
        assertEquals("EUR", state.currency)
        assertEquals(1, state.weekStart)
        assertEquals("dark", settings.stored.value.theme)
    }

    @Test
    fun `idioma automatico del dispositivo no lo arrastra la cuenta`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply { profile = profile(language = "fr") }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        vm.state.first { it.theme == "system" }
        assertEquals("", vm.state.value.language)
    }

    @Test
    fun `idioma explicito si lo arrastra la cuenta`() = runTest {
        val settings = FakeSettings()
        settings.stored.value = UserPreferences(language = "es")
        val repo = FakeProfileRepository().apply { profile = profile(language = "pt") }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        val state = vm.state.first { it.language == "pt" }
        assertEquals("pt", state.language)
        assertEquals("pt", settings.stored.value.language)
    }

    @Test
    fun `guardar idioma explicito lo escribe en la cuenta y aplica el locale`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply { profile = profile() }
        var applied: String? = null
        val vm = PreferencesViewModel(settings, repo, applyLanguage = { applied = it })
        vm.setLanguage("en")
        assertEquals("en", vm.state.value.language)
        assertEquals("en", settings.stored.value.language)
        assertEquals("en", repo.lastSavedLanguage)
        assertEquals("en", applied)
    }

    @Test
    fun `guardar idioma automatico no lo escribe en la cuenta`() = runTest {
        val settings = FakeSettings()
        settings.stored.value = UserPreferences(language = "en")
        val repo = FakeProfileRepository().apply { profile = profile(language = "en") }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        vm.setLanguage("")
        assertEquals("", vm.state.value.language)
        assertEquals("", repo.lastSavedLanguage)
    }

    @Test
    fun `error de red al guardar no cierra y emite el tipo de error`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply { profile = profile(); failSave = true }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        vm.setCurrency("USD")
        assertEquals(ErrorType.NoNetwork, vm.error.value)
        // El cambio local sí aplica (RNF-06: instantáneo).
        assertEquals("USD", vm.state.value.currency)
        assertEquals("USD", settings.stored.value.currency)
    }

    @Test
    fun `formato de fecha e inicio de semana se guardan`() = runTest {
        val settings = FakeSettings()
        val repo = FakeProfileRepository().apply { profile = profile() }
        val vm = PreferencesViewModel(settings, repo, applyLanguage = {})
        vm.setDateFormat("YYYY-MM-DD")
        vm.setWeekStart(3)
        assertEquals("YYYY-MM-DD", vm.state.value.dateFormat)
        assertEquals(3, vm.state.value.weekStart)
        assertEquals("YYYY-MM-DD", repo.profile?.dateFormat)
        assertEquals(3, repo.profile?.weekStart)
    }

    @Test
    fun `estado inicial con valores neutros`() = runTest {
        val vm = PreferencesViewModel(FakeSettings(), FakeProfileRepository(), applyLanguage = {})
        val s = vm.state.value
        assertEquals("", s.language)
        assertEquals("system", s.theme)
        assertEquals("COP", s.currency)
        assertEquals("DD/MM/YYYY", s.dateFormat)
        assertEquals(0, s.weekStart)
        assertTrue(true)
    }
}
