package com.pocketpal.app.ui.profile

import com.pocketpal.app.auth.ErrorType
import com.pocketpal.app.data.phone.PhoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Fase 7 (RF-B3 / RN-06 / D-15) sobre [PhoneViewModel]: validación del número,
 * envío del código, verificación y reenvío.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhoneViewModelTest {

    private class FakePhone : PhoneRepository {
        var phone: String? = null
        var failSend: Exception? = null
        var failVerify = false
        var sendCount = 0
        override fun currentPhone(): String? = phone
        override suspend fun sendVerificationCode(e164: String) {
            failSend?.let { throw it }
            sendCount++
        }
        override suspend fun verifyCode(e164: String, code: String) {
            if (failVerify) throw Exception("otp invalido")
            phone = e164
        }
    }

    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(main) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `numero invalido no envia el codigo`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setNumber("123")
        vm.sendCode()
        assertEquals(ErrorType.InvalidPhone, vm.formError.value)
        assertEquals(0, repo.sendCount)
    }

    @Test
    fun `envio valido arma el E164 y pasa al paso 2`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setPrefix("+57")
        vm.setNumber("300 123 4567")
        vm.sendCode()
        assertEquals(PhoneViewModel.Event.CodeSent, vm.event.value)
        assertEquals(PhoneViewModel.Step.CODE, vm.state.value.step)
        assertTrue(vm.codeExpiresAt.value > System.currentTimeMillis()) // D-15
    }

    @Test
    fun `sin proveedor sms se muestra SmsUnavailable`() = runTest {
        val repo = FakePhone().apply { failSend = Exception("sin sms") }
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        assertEquals(ErrorType.SmsUnavailable, vm.error.value)
    }

    @Test
    fun `sin red se muestra NoNetwork`() = runTest {
        val repo = FakePhone().apply { failSend = java.io.IOException("sin red") }
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        assertEquals(ErrorType.NoNetwork, vm.error.value)
    }

    @Test
    fun `codigo incompleto no verifica`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        vm.setCode("123")
        vm.verifyCode()
        assertEquals(ErrorType.OtpIncomplete, vm.formError.value)
        assertEquals(null, repo.phone)
    }

    @Test
    fun `codigo correcto guarda el telefono verificado`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setPrefix("+57")
        vm.setNumber("3001234567")
        vm.sendCode()
        vm.setCode("123456")
        vm.verifyCode()
        assertEquals(PhoneViewModel.Event.Verified, vm.event.value)
        assertEquals("+573001234567", vm.state.value.currentPhone)
        assertEquals("+573001234567", repo.phone) // RN-06: guardado tras verificar
    }

    @Test
    fun `codigo incorrecto muestra InvalidOtp`() = runTest {
        val repo = FakePhone().apply { failVerify = true }
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        vm.setCode("000000")
        vm.verifyCode()
        assertEquals(ErrorType.InvalidOtp, vm.formError.value)
    }

    @Test
    fun `reenviar reinicia el temporizador`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        val first = vm.codeExpiresAt.value
        vm.resend()
        assertEquals(PhoneViewModel.Event.CodeResent, vm.event.value)
        assertTrue(vm.codeExpiresAt.value >= first)
        assertEquals(2, repo.sendCount)
    }

    @Test
    fun `ese no es tu numero vuelve al paso 1`() = runTest {
        val repo = FakePhone()
        val vm = PhoneViewModel(repo)
        vm.setNumber("3001234567")
        vm.sendCode()
        vm.backToNumber()
        assertEquals(PhoneViewModel.Step.NUMBER, vm.state.value.step)
    }
}
