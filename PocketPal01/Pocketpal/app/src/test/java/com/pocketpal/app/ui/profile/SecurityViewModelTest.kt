package com.pocketpal.app.ui.profile

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.device.DeviceRepository
import com.pocketpal.app.data.device.DeviceSession
import com.pocketpal.app.data.security.SecurityRepository
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
 * Fase 4 (RF-B4 / RF-B5 / RN-04 / RN-05) sobre [SecurityViewModel]: validación de
 * reglas, reautenticación, cierre de las demás sesiones (D-12) y revocación de un
 * dispositivo (D-01).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private class FakeSecurity : SecurityRepository {
        var hasPw = true
        var reauthOk = true
        var changed: String? = null
        var marked = false
        var failChange = false
        override fun hasPassword(): Boolean = hasPw
        override fun currentEmail(): String? = "u@test.com"
        override suspend fun reauthenticate(email: String, password: String): Boolean = reauthOk
        override suspend fun changePassword(newPassword: String) {
            if (failChange) throw java.io.IOException("sin red")
            changed = newPassword
        }
        override suspend fun markPasswordChanged(): String {
            marked = true
            return "2026-10-14T12:00:00Z"
        }
        override suspend fun passwordChangedAt(): String? =
            if (marked) "2026-10-14T12:00:00Z" else null
    }

    private class FakeDevices : DeviceRepository {
        var currentSid = "sid-1"
        val devices = mutableListOf<DeviceSession>()
        var revokeCalls = 0
        var revokeAllCalls = 0
        var registerCalls = 0
        override fun currentSessionId(): String? = currentSid
        override suspend fun registerCurrentDevice() { registerCalls++ }
        override suspend fun list(): List<DeviceSession> = devices.toList()
        override suspend fun revoke(deviceId: String) {
            revokeCalls++
            devices.removeAll { it.id == deviceId }
        }
        override suspend fun revokeAllOthers() {
            revokeAllCalls++
            devices.removeAll { it.sessionId != currentSid }
        }
    }

    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(main) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun device(id: String, sid: String) = DeviceSession(
        id = id,
        deviceName = "Device $id",
        platform = "android",
        lastActiveAt = "2026-10-14T12:00:00Z",
        sessionId = sid,
    )

    @Test
    fun `marca el dispositivo actual y lista los demas`() = runTest {
        val repo = FakeDevices().apply {
            devices += device("d1", "sid-1")
            devices += device("d2", "sid-2")
        }
        val vm = SecurityViewModel(FakeSecurity(), repo)
        val data = (vm.state.value as SecurityViewModel.UiState.Content).data
        assertEquals(2, data.devices.size)
        assertTrue(data.devices.first { it.id == "d1" }.isCurrent)
        assertFalse(data.devices.first { it.id == "d2" }.isCurrent)
    }

    @Test
    fun `cuentas solo de google no tienen contraseña que cambiar`() = runTest {
        val security = FakeSecurity().apply { hasPw = false }
        val vm = SecurityViewModel(security, FakeDevices())
        assertFalse((vm.state.value as SecurityViewModel.UiState.Content).data.hasPassword)
    }

    @Test
    fun `rn-04 valida las tres reglas y la contrasena repetida`() = runTest {
        val security = FakeSecurity()
        val vm = SecurityViewModel(security, FakeDevices())

        vm.changePassword(current = "Secret123", new = "abc", confirm = "abc")
        assertEquals(ErrorType.PasswordTooShort, vm.formError.value)

        vm.changePassword(current = "Secret123", new = "abcdefgh", confirm = "abcdefgh")
        assertEquals(ErrorType.PasswordNeedsUpperAndDigit, vm.formError.value)

        vm.changePassword(current = "Secret123", new = "Secret124", confirm = "Secret125")
        assertEquals(ErrorType.PasswordMismatch, vm.formError.value)

        vm.changePassword(current = "Secret123", new = "Secret123", confirm = "Secret123")
        assertEquals(ErrorType.NewPasswordSameAsCurrent, vm.formError.value)

        assertEquals(null, security.changed)
    }

    @Test
    fun `cambio valido cambia la contrasena, marca la fecha y cierra las demas sesiones`() = runTest {
        val security = FakeSecurity()
        val devices = FakeDevices()
        val vm = SecurityViewModel(security, devices)
        vm.changePassword(current = "Secret123", new = "NewPass12", confirm = "NewPass12")
        assertEquals("NewPass12", security.changed)
        assertTrue(security.marked)
        assertEquals(1, devices.revokeAllCalls) // D-12
        assertEquals(SecurityViewModel.Event.PasswordChanged, vm.event.value)
    }

    @Test
    fun `contrasena actual incorrecta no cambia nada`() = runTest {
        val security = FakeSecurity().apply { reauthOk = false }
        val vm = SecurityViewModel(security, FakeDevices())
        vm.changePassword(current = "Wrong123", new = "NewPass12", confirm = "NewPass12")
        assertEquals(ErrorType.CurrentPasswordIncorrect, vm.formError.value)
        assertEquals(null, security.changed)
    }

    @Test
    fun `error de red al cambiar emite NoNetwork`() = runTest {
        val security = FakeSecurity().apply { failChange = true }
        val vm = SecurityViewModel(security, FakeDevices())
        vm.changePassword(current = "Secret123", new = "NewPass12", confirm = "NewPass12")
        assertEquals(ErrorType.NoNetwork, vm.error.value)
    }

    @Test
    fun `revocar un dispositivo llama al repositorio y refresca`() = runTest {
        val devices = FakeDevices().apply {
            devices += device("d1", "sid-1")
            devices += device("d2", "sid-2")
        }
        val vm = SecurityViewModel(FakeSecurity(), devices)
        vm.revokeDevice("d2")
        assertEquals(1, devices.revokeCalls)
        val list = (vm.state.value as SecurityViewModel.UiState.Content).data.devices
        assertEquals(1, list.size)
        assertEquals("d1", list.first().id)
    }

    @Test
    fun `registrar el dispositivo actual es mejor esfuerzo`() = runTest {
        val devices = FakeDevices()
        val vm = SecurityViewModel(FakeSecurity(), devices)
        vm.registerCurrentDevice()
        assertEquals(1, devices.registerCalls)
    }
}
