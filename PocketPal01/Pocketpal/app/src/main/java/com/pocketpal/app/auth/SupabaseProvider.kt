package com.pocketpal.app.auth

import com.pocketpal.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Cliente único de Supabase (RF-A2).
 *
 * Las claves NO viven en el código: se leen de `local.properties`
 * (ignorado por Git) y llegan aquí mediante `BuildConfig`.
 * Si falta un valor, la app falla al iniciar con un mensaje que indica cuál.
 */
object SupabaseProvider {

    private fun required(name: String, value: String): String {
        check(value.isNotBlank()) {
            "Falta la clave '$name'. Copia local.properties.example a local.properties " +
                "(carpeta PocketPal01/Pocketpal) y rellénala. Ver README.md, sección 'Configuración'."
        }
        return value
    }

    val SUPABASE_URL = required("SUPABASE_URL", BuildConfig.SUPABASE_URL)
    val SUPABASE_KEY = required("SUPABASE_KEY", BuildConfig.SUPABASE_KEY)
    val GOOGLE_WEB_CLIENT_ID = required("GOOGLE_WEB_CLIENT_ID", BuildConfig.GOOGLE_WEB_CLIENT_ID)

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            install(Auth)
            // Fase 2: lectura de las tablas con RLS (profiles, categories, .).
            install(Postgrest)
            // Fase 3: foto de perfil en el bucket `avatars` (D-09). Sin este install,
            // `supabase.storage` lanza "Plugin storage not installed".
            install(Storage)
            // Fase 4: Edge Function revoke-device (D-01 opción B, sesiones activas)
            install(Functions)
        }
    }
}
