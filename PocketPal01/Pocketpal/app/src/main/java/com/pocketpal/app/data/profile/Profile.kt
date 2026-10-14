package com.pocketpal.app.data.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fila de `public.profiles` (1 a 1 con `auth.users`).
 *
 * Se guardan **códigos neutros** (`es`, `COP`, `DD/MM/YYYY`, `light`…, RN-11); la UI es
 * la que los traduce y formatea según el idioma y las preferencias (RF-A3).
 */
@Serializable
data class Profile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("country") val country: String = "CO",
    @SerialName("currency") val currency: String = "COP",
    @SerialName("language") val language: String = "es",
    @SerialName("date_format") val dateFormat: String = "DD/MM/YYYY",
    @SerialName("week_start") val weekStart: Int = 0,
    @SerialName("theme") val theme: String = "system",
    @SerialName("avatar_path") val avatarPath: String? = null,
)

/**
 * Contadores que muestra "Mi perfil" ("Gastos registrados" y "Metas activas").
 *
 * Vienen de las tablas `movements` y `goals`, que **no existen hasta el Módulo 3**,
 * así que esta fase devuelve 0 (§6, "Dependencia con módulos futuros").
 */
data class ProfileStats(
    val registeredExpenses: Int = 0,
    val activeGoals: Int = 0,
)
