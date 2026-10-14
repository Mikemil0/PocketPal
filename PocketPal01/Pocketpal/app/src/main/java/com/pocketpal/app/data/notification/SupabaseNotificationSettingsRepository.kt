package com.pocketpal.app.data.notification

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Preferencias de notificación en Supabase (tabla `notification_settings` de la
 * Fase 2, con RLS "own notification settings"). El trigger `handle_new_user` crea
 * la fila al registrarse y la migración hizo el backfill de las cuentas existentes;
 * si por lo que sea falta la fila, se crea al guardar.
 */
class SupabaseNotificationSettingsRepository(
    private val supabase: SupabaseClient,
) : NotificationSettingsRepository {

    override suspend fun get(): NotificationSettings {
        val userId = currentUserId() ?: return NotificationSettings()
        return fetch(userId) ?: NotificationSettings()
    }

    override suspend fun save(settings: NotificationSettings) {
        val userId = currentUserId() ?: return
        val values = buildJsonObject {
            put("user_id", userId)
            put("daily_reminder", settings.dailyReminder)
            put("weekly_summary", settings.weeklySummary)
            put("budget_alert", settings.budgetAlert)
            put("budget_threshold", settings.budgetThreshold)
            put("goals_progress", settings.goalsProgress)
            put("channel_email", settings.channelEmail)
            put("channel_sms", settings.channelSms)
        }
        if (fetch(userId) == null) {
            supabase.postgrest.from(TABLE).insert(values)
        } else {
            supabase.postgrest.from(TABLE).update(values) { filter { eq("user_id", userId) } }
        }
    }

    private suspend fun fetch(userId: String): NotificationSettings? =
        supabase.postgrest.from(TABLE)
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull()

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    companion object {
        const val TABLE = "notification_settings"
    }
}
