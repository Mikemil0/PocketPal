package com.pocketpal.app.data.dai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Un archivo de DataStore propio de Dai (separado de las preferencias generales). */
private val Context.daiDataStore by preferencesDataStore(name = "dai_settings")

/**
 * Preferencias de Dai con **DataStore** (unidad 6 del curso: persistencia local).
 * DataStore guarda pares clave-valor en un archivo del teléfono, de forma asíncrona
 * (con corrutinas y Flow), y reemplaza a SharedPreferences.
 */
class DataStoreDaiSettings(private val context: Context) : DaiSettingsRepository {

    override val settings: Flow<DaiSettings> = context.daiDataStore.data.map { prefs ->
        DaiSettings(voiceReplies = prefs[KEY_VOICE_REPLIES] ?: false)
    }

    override suspend fun setVoiceReplies(enabled: Boolean) {
        context.daiDataStore.edit { it[KEY_VOICE_REPLIES] = enabled }
    }

    private companion object {
        val KEY_VOICE_REPLIES = booleanPreferencesKey("voice_replies")
    }
}
