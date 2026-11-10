package com.pocketpal.app.ui.dai

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.dai.DaiRole
import com.pocketpal.app.data.dai.DaiSettings
import com.pocketpal.app.data.dai.DaiSettingsRepository
import com.pocketpal.app.data.dai.DaiVoice
import com.pocketpal.app.data.dai.FakeDaiRepository
import com.pocketpal.app.data.dai.VoiceProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Flujo 4 · Configuración de Dai, borrar historial y errores del modo voz. */
@OptIn(ExperimentalCoroutinesApi::class)
class DaiSettingsViewModelTest {

    /** Preferencias en memoria (en la app es DataStore). */
    private class FakeDaiSettings : DaiSettingsRepository {
        val state = MutableStateFlow(DaiSettings())
        override val settings: StateFlow<DaiSettings> = state
        override suspend fun setVoiceReplies(enabled: Boolean) {
            state.value = state.value.copy(voiceReplies = enabled)
        }
        override suspend fun setServerUrl(url: String) {
            state.value = state.value.copy(serverUrl = url)
        }
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeDaiRepository
    private lateinit var settings: FakeDaiSettings

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeDaiRepository().apply {
            addCategory("comida", "Comida")
            addExpense("Mercado", 50_000.0, LocalDate.of(2026, 10, 2), "comida")
        }
        settings = FakeDaiSettings()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `la voz de Dai se guarda en las preferencias`() = runTest(dispatcher) {
        val vm = DaiSettingsViewModel(repository, settings)
        assertFalse(settings.state.value.voiceReplies)
        vm.setVoiceReplies(true)
        advanceUntilIdle()
        assertTrue(settings.state.value.voiceReplies)
        vm.setVoiceReplies(false)
        advanceUntilIdle()
        assertFalse(settings.state.value.voiceReplies)
    }

    @Test
    fun `borrar historial borra solo la conversacion`() = runTest(dispatcher) {
        repository.save(DaiRole.User, "hola")
        repository.save(DaiRole.Dai, "¡Hola!")
        val vm = DaiSettingsViewModel(repository, settings)
        vm.clearHistory()
        advanceUntilIdle()
        assertTrue(repository.messages.isEmpty())
        assertEquals(1, repository.movements.size)      // los movimientos no se tocan
        assertEquals(DaiSettingsViewModel.Event.HistoryCleared, vm.event.value)
        assertFalse(vm.deleting.value)
    }

    @Test
    fun `borrar historial sin internet muestra el error`() = runTest(dispatcher) {
        repository.save(DaiRole.User, "hola")
        repository.failNextWithNetwork = true
        val vm = DaiSettingsViewModel(repository, settings)
        vm.clearHistory()
        advanceUntilIdle()
        assertEquals(ErrorType.NoNetwork, vm.error.value)
        assertEquals(1, repository.messages.size)
        assertNull(vm.event.value)
    }

    @Test
    fun `errores del reconocimiento de voz`() {
        assertEquals(VoiceProblem.NotUnderstood, DaiVoice.problemFor(DaiVoice.ERROR_NO_MATCH))
        assertEquals(VoiceProblem.NotUnderstood, DaiVoice.problemFor(DaiVoice.ERROR_SPEECH_TIMEOUT))
        assertEquals(VoiceProblem.Connection, DaiVoice.problemFor(DaiVoice.ERROR_NETWORK))
        assertEquals(VoiceProblem.Connection, DaiVoice.problemFor(DaiVoice.ERROR_SERVER))
        assertEquals(VoiceProblem.Permission, DaiVoice.problemFor(DaiVoice.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(VoiceProblem.Unavailable, DaiVoice.problemFor(DaiVoice.ERROR_RECOGNIZER_BUSY))
    }

    @Test
    fun `resultado de la voz limpio`() {
        assertEquals("Gasté 45 mil pesos en mercado", DaiVoice.bestResult(listOf("", " gasté 45 mil pesos en mercado ")))
        assertNull(DaiVoice.bestResult(emptyList()))
        assertNull(DaiVoice.bestResult(null))
    }

    @Test
    fun `lo que se dice por voz lo entiende el chat`() = runTest(dispatcher) {
        val chat = DaiChatViewModel(repository, today = { LocalDate.of(2026, 10, 27) }, zone = { FakeDaiRepository.ZONE }, replyDelayMs = 0)
        chat.send(DaiVoice.bestResult(listOf("por qué este mes no me alcanza"))!!)
        advanceUntilIdle()
        assertTrue(chat.state.value.messages.last().text.contains("Comida es tu mayor categoría"))
    }
}
