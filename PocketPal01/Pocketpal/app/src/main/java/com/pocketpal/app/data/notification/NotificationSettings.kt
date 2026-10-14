package com.pocketpal.app.data.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fila de `public.notification_settings` (RF-B6). Valores **códigos neutros**
 * (RN-11): la UI traduce y formatea. `budget_threshold` es un entero 50–100 (RN-08).
 */
@Serializable
data class NotificationSettings(
    @SerialName("daily_reminder") val dailyReminder: Boolean = false,
    @SerialName("weekly_summary") val weeklySummary: Boolean = false,
    @SerialName("budget_alert") val budgetAlert: Boolean = false,
    @SerialName("budget_threshold") val budgetThreshold: Int = 80,
    @SerialName("goals_progress") val goalsProgress: Boolean = false,
    @SerialName("channel_email") val channelEmail: Boolean = true,
    @SerialName("channel_sms") val channelSms: Boolean = false,
)

/**
 * Contrato de las preferencias de notificación (RNF-02): las ViewModels dependen de
 * esta interfaz, no de Supabase. En este módulo solo se **guardan preferencias**:
 * el envío real de recordatorios es de un módulo posterior (RF-B6).
 */
interface NotificationSettingsRepository {

    /** Preferencias del usuario con sesión; valores por defecto si no hay fila. */
    suspend fun get(): NotificationSettings

    /** Guarda el estado completo (auto-guardado al cambiar un interruptor, §8.7). */
    suspend fun save(settings: NotificationSettings)
}
