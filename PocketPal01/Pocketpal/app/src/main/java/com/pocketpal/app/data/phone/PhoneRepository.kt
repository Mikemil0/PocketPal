package com.pocketpal.app.data.phone

/**
 * Contrato del teléfono (RNF-02, RF-B3 / RN-06). El número **solo cuenta como
 * guardado después de verificarlo**: vive en `auth.users.phone`.
 */
interface PhoneRepository {

    /** Teléfono verificado actual (E.164) o `null` si no hay (RN-06). */
    fun currentPhone(): String?

    /**
     * Envía el código SMS al número (E.164) con `updateUser { phone }`.
     * Lanza la excepción de auth si el SMS no está disponible (D-02: sin proveedor
     * o número de prueba, la pantalla muestra "SMS no disponible").
     */
    suspend fun sendVerificationCode(e164: String)

    /** Confirma el código de 6 dígitos (`verifyPhoneOtp`, tipo cambio de teléfono). */
    suspend fun verifyCode(e164: String, code: String)
}
