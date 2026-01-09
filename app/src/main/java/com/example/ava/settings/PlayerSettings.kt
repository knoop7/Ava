package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettings(
    val volume: Float = 0.1f,
    val muted: Boolean = false,
    val enableWakeSound: Boolean = true,
    val enableScreenOff: Boolean = true,  
    val wakeSound: String = "asset:///sounds/wake_word_triggered.wav",
    val timerFinishedSound: String = "asset:///sounds/timer_finished.flac",
    val enableContinuousConversation: Boolean = false,  
    val enableFloatingWindow: Boolean = false,  
    val enableVinylCover: Boolean = false,  
    val enableDreamClock: Boolean = false,  
    val enableWeatherOverlay: Boolean = false,  
    val weatherCity: String = "",  
    val enableAutoRestart: Boolean = false,  
)

val Context.playerSettingsStore: DataStore<PlayerSettings> by dataStore(
    fileName = "player_settings.json",
    serializer = SettingsSerializer(PlayerSettings.serializer(), PlayerSettings()),
    corruptionHandler = defaultCorruptionHandler(PlayerSettings())
)

class PlayerSettingsStore(dataStore: DataStore<PlayerSettings>) :
    SettingsStoreImpl<PlayerSettings>(dataStore, PlayerSettings()) {
    val volume =
        SettingState(getFlow().map { it.volume }) { value -> update { it.copy(volume = value) } }
    val muted =
        SettingState(getFlow().map { it.muted }) { value -> update { it.copy(muted = value) } }
    val enableWakeSound = SettingState(getFlow().map { it.enableWakeSound }) { value ->
        update {
            it.copy(enableWakeSound = value)
        }
    }
    val enableScreenOff = SettingState(getFlow().map { it.enableScreenOff }) { value ->
        update {
            it.copy(enableScreenOff = value)
        }
    }
    val wakeSound =
        SettingState(getFlow().map { it.wakeSound }) { value -> update { it.copy(wakeSound = value) } }
    val timerFinishedSound =
        SettingState(getFlow().map { it.timerFinishedSound }) { value ->
            update {
                it.copy(
                    timerFinishedSound = value
                )
            }
        }
    val enableContinuousConversation = SettingState(getFlow().map { it.enableContinuousConversation }) { value ->
        update { it.copy(enableContinuousConversation = value) }
    }
    val enableFloatingWindow = SettingState(getFlow().map { it.enableFloatingWindow }) { value ->
        update { it.copy(enableFloatingWindow = value) }
    }
    val enableVinylCover = SettingState(getFlow().map { it.enableVinylCover }) { value ->
        update { it.copy(enableVinylCover = value) }
    }
    val enableDreamClock = SettingState(getFlow().map { it.enableDreamClock }) { value ->
        update { it.copy(enableDreamClock = value) }
    }
    val enableWeatherOverlay = SettingState(getFlow().map { it.enableWeatherOverlay }) { value ->
        update { it.copy(enableWeatherOverlay = value) }
    }
    val weatherCity = SettingState(getFlow().map { it.weatherCity }) { value ->
        update { it.copy(weatherCity = value) }
    }
    val enableAutoRestart = SettingState(getFlow().map { it.enableAutoRestart }) { value ->
        update { it.copy(enableAutoRestart = value) }
    }
}