package com.pocketpal.app.ui.profile

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.notification.NotificationSettings
import com.pocketpal.app.data.notification.NotificationSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fase 5 (RF-B6 / RN-08) sobre [NotificationSettingsViewModel]: auto-guardado con
 * actualización optimista, umbral entre 50 y 100 y reversión si falla la red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private class FakeRepository : NotificationSettingsRepository {
        var stored = NotificationSettings()
        var failSave = false
        var saveCount = 0
        override suspend fun get(): NotificationSettings = stored
        override suspend fun save(settings: NotificationSettings) {
            if (failSave) throw java.io.IOException("sin red")
            saveCount++
            stored = settings
        }
    }

    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(main) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `carga las preferencias guardadas`() = runTest {
        val repo = FakeRepository().apply {
            stored = NotificationSettings(dailyReminder = true, budgetThreshold = 60)
        }
        val vm = NotificationSettingsViewModel(repo)
        val data = (vm.state.value as NotificationSettingsViewModel.UiState.Content).data
        assertTrue(data.dailyReminder)
        assertEquals(60, data.budgetThreshold)
    }

    @Test
    fun `un interruptor se guarda automatico`() = runTest {
        val repo = FakeRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.toggleDailyReminder()
        assertTrue((vm.state.value as NotificationSettingsViewModel.UiState.Content).data.dailyReminder)
        assertEquals(1, repo.saveCount)
        assertTrue(repo.stored.dailyReminder)
    }

    @Test
    fun `el umbral se limita entre 50 y 100`() = runTest {
        val repo = FakeRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.setBudgetThreshold(85)
        assertEquals(85, (vm.state.value as NotificationSettingsViewModel.UiState.Content).data.budgetThreshold)
        vm.setBudgetThreshold(10)
        assertEquals(50, (vm.state.value as NotificationSettingsViewModel.UiState.Content).data.budgetThreshold)
        vm.setBudgetThreshold(150)
        assertEquals(100, (vm.state.value as NotificationSettingsViewModel.UiState.Content).data.budgetThreshold)
    }

    @Test
    fun `por defecto el umbral es 80 y el canal correo activo`() = runTest {
        val vm = NotificationSettingsViewModel(FakeRepository())
        val data = (vm.state.value as NotificationSettingsViewModel.UiState.Content).data
        assertEquals(80, data.budgetThreshold)
        assertTrue(data.channelEmail)
        assertFalse(data.channelSms)
        assertFalse(data.budgetAlert)
    }

    @Test
    fun `si falla la red se revierte y se emite el error`() = runTest {
        val repo = FakeRepository().apply { failSave = true }
        val vm = NotificationSettingsViewModel(repo)
        vm.toggleWeeklySummary()
        // Revertido al valor anterior...
        assertFalse((vm.state.value as NotificationSettingsViewModel.UiState.Content).data.weeklySummary)
        // ...con el error para que la UI lo muestre (RNF-05).
        assertEquals(ErrorType.NoNetwork, vm.error.value)
    }

    @Test
    fun `los canales se alternan`() = runTest {
        val repo = FakeRepository()
        val vm = NotificationSettingsViewModel(repo)
        vm.toggleChannelEmail()
        assertFalse((vm.state.value as NotificationSettingsViewModel.UiState.Content).data.channelEmail)
        vm.toggleChannelSms()
        assertTrue((vm.state.value as NotificationSettingsViewModel.UiState.Content).data.channelSms)
    }
}
