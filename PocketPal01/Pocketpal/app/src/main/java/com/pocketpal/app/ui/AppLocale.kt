package com.pocketpal.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * RF-A3: **única fuente de verdad** del idioma de la app. La usa el selector del Welcome
 * y la usará la pantalla "Idioma" de Preferencias generales (Fase 5).
 *
 * `setLanguage` funciona en todas las API gracias a `AppCompatDelegate.setApplicationLocales`
 * (exige que `MainActivity` extienda `AppCompatActivity`):
 * - **Android 13+ (API 33):** la preferencia la guarda el **sistema** (`LocaleManager`):
 *   persiste al cerrar y reabrir la app y aparece en Ajustes > Apps > PocketPal > Idioma.
 * - **Android 12 o menor (API ≤ 32):** el sistema no tiene esa API, así que el Manifest
 *   declara `AppLocalesMetadataHolderService` con `autoStoreLocales=true`; AppCompat guarda
 *   la lista en sus SharedPreferences y la reaplica sola al arrancar.
 *
 * `tag` nulo o vacío = **Automático**: sigue al idioma del dispositivo (lista vacía).
 * Los códigos guardados son neutros (`es`, `en`…, RN-11). La sincronización con la cuenta
 * llega con las preferencias generales (Fase 5, RNF-06).
 */
object AppLocale {

    /** Aplica (o con `tag` nulo, restablece al sistema) el idioma de la app en ejecución. */
    fun setLanguage(tag: String?) {
        val locales = if (tag.isNullOrBlank()) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** Lo que eligió el usuario: `null` = Automático (idioma del sistema), o `es`/`en`… */
    fun savedTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty()) null else locales.get(0)?.language
    }

    /** Código en mayúsculas del idioma **efectivo** (`ES`, `EN`…) para el botón del Welcome. */
    fun currentLanguageCode(): String =
        Locale.getDefault().language.take(2).uppercase(Locale.ROOT)
}
