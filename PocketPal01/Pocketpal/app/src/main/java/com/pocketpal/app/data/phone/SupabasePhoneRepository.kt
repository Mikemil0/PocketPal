package com.pocketpal.app.data.phone

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth

/**
 * Teléfono en Supabase (RF-B3, §8.4): `updateUser { phone }` envía el SMS con el
 * código y `verifyPhoneOtp` (tipo `PHONE_CHANGE`) lo confirma. El teléfono verificado
 * queda en `auth.users.phone` (RN-06).
 */
class SupabasePhoneRepository(private val supabase: SupabaseClient) : PhoneRepository {

    /**
     * Teléfono verificado o `null` (RN-06). `""` se normaliza a `null`: Supabase
     * devuelve cadena vacía cuando no hay teléfono (§14, fila 86).
     */
    override fun currentPhone(): String? =
        supabase.auth.currentUserOrNull()?.phone?.takeIf { it.isNotBlank() }

    override suspend fun sendVerificationCode(e164: String) {
        supabase.auth.updateUser { phone = e164 }
    }

    override suspend fun verifyCode(e164: String, code: String) {
        supabase.auth.verifyPhoneOtp(type = OtpType.Phone.PHONE_CHANGE, phone = e164, token = code)
    }
}
