package com.example.ava.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ava.utils.getRandomMacAddressString
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

val Context.voiceSatelliteDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_assistant_settings")

data class VoiceSatelliteSettings(
    val name: String,
    val serverPort: Int,
    val macAddress: String,
    val wakeWord: String,
    val stopWord: String,
    val playWakeSound: Boolean,
    val wakeSound: String,
    val timerFinishedSound: String
)

object VoiceSatellitePreferenceKeys{
    val NAME = stringPreferencesKey("name")
    val SERVER_PORT = intPreferencesKey("server_port")
    val MAC_ADDRESS = stringPreferencesKey("mac_address")
    val WAKE_WORD = stringPreferencesKey("wake_word")
    val STOP_WORD = stringPreferencesKey("stop_word")
    val PLAY_WAKE_SOUND = booleanPreferencesKey("play_wake_sound")
    val WAKE_SOUND = stringPreferencesKey("wake_sound")
    val TIMER_FINISHED_SOUND = stringPreferencesKey("timer_finished_sound")
}

class VoiceAssistantPreferencesStore(context: Context) {
    private val dataStore = context.voiceSatelliteDataStore

    fun getSettingsFlow() =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Error reading preferences, returning defaults", exception)
                    emit(emptyPreferences())
                } else throw exception
            }
            .filter { checkSaveDefaultSettings(it) }
            .map { createSettingsOrDefault(it) }
            .onEach { Log.d(TAG, "Loaded settings: $it") }

    suspend fun getSettings() = getSettingsFlow().first()

    private suspend fun checkSaveDefaultSettings(currentPreferences: Preferences): Boolean {
        if (currentPreferences[VoiceSatellitePreferenceKeys.NAME] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.SERVER_PORT] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.MAC_ADDRESS] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.WAKE_WORD] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.STOP_WORD] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.PLAY_WAKE_SOUND] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.WAKE_SOUND] != null
            && currentPreferences[VoiceSatellitePreferenceKeys.TIMER_FINISHED_SOUND] != null
        ) return true

        save(createSettingsOrDefault(currentPreferences))
        return false
    }

    private fun createSettingsOrDefault(preferences: Preferences) = VoiceSatelliteSettings(
        name = preferences[VoiceSatellitePreferenceKeys.NAME] ?: DEFAULT_NAME,
        serverPort = preferences[VoiceSatellitePreferenceKeys.SERVER_PORT] ?: DEFAULT_SERVER_PORT,
        macAddress = preferences[VoiceSatellitePreferenceKeys.MAC_ADDRESS] ?: DEFAULT_MAC_ADDRESS,
        wakeWord = preferences[VoiceSatellitePreferenceKeys.WAKE_WORD] ?: DEFAULT_WAKE_WORD,
        stopWord = preferences[VoiceSatellitePreferenceKeys.STOP_WORD] ?: DEFAULT_STOP_WORD,
        playWakeSound = preferences[VoiceSatellitePreferenceKeys.PLAY_WAKE_SOUND]
            ?: DEFAULT_PLAY_WAKE_SOUND,
        wakeSound = preferences[VoiceSatellitePreferenceKeys.WAKE_SOUND] ?: DEFAULT_WAKE_SOUND,
        timerFinishedSound = preferences[VoiceSatellitePreferenceKeys.TIMER_FINISHED_SOUND]
            ?: DEFAULT_TIMER_FINISHED_SOUND
    )

    suspend fun save(voiceSatelliteSettings: VoiceSatelliteSettings) {
        try {
            dataStore.edit { preferences ->
                preferences[VoiceSatellitePreferenceKeys.NAME] =
                    voiceSatelliteSettings.name
                preferences[VoiceSatellitePreferenceKeys.SERVER_PORT] =
                    voiceSatelliteSettings.serverPort
                preferences[VoiceSatellitePreferenceKeys.MAC_ADDRESS] =
                    voiceSatelliteSettings.macAddress
                preferences[VoiceSatellitePreferenceKeys.WAKE_WORD] =
                    voiceSatelliteSettings.wakeWord
                preferences[VoiceSatellitePreferenceKeys.STOP_WORD] =
                    voiceSatelliteSettings.stopWord
                preferences[VoiceSatellitePreferenceKeys.PLAY_WAKE_SOUND] =
                    voiceSatelliteSettings.playWakeSound
                preferences[VoiceSatellitePreferenceKeys.WAKE_SOUND] =
                    voiceSatelliteSettings.wakeSound
                preferences[VoiceSatellitePreferenceKeys.TIMER_FINISHED_SOUND] =
                    voiceSatelliteSettings.timerFinishedSound
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving preferences", e)
        }
    }

    suspend fun saveServerName(value: String) =
        saveSetting(VoiceSatellitePreferenceKeys.NAME, value)

    suspend fun saveServerPort(value: Int) =
        saveSetting(VoiceSatellitePreferenceKeys.SERVER_PORT, value)

    suspend fun savePlayWakeSound(value: Boolean) =
        saveSetting(VoiceSatellitePreferenceKeys.PLAY_WAKE_SOUND, value)

    suspend fun saveWakeWord(value: String) =
        saveSetting(VoiceSatellitePreferenceKeys.WAKE_WORD, value)

    private suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        try {
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving preference $key", e)
        }
    }

    companion object {
        private const val TAG = "VoiceAssistantPreferences"
        private const val DEFAULT_NAME = "Android Voice Assistant"
        private const val DEFAULT_SERVER_PORT = 6053
        private val DEFAULT_MAC_ADDRESS = getRandomMacAddressString()
        private const val DEFAULT_WAKE_WORD = "okay_nabu"
        private const val DEFAULT_STOP_WORD = "stop"
        private const val DEFAULT_PLAY_WAKE_SOUND = true
        private const val DEFAULT_WAKE_SOUND = "asset:///sounds/wake_word_triggered.flac"
        private const val DEFAULT_TIMER_FINISHED_SOUND = "asset:///sounds/timer_finished.flac"
    }
}