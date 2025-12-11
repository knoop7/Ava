package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSettings(
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val enableWakeSound: Boolean = true,
    val wakeSound: String = "asset:///sounds/wake_word_triggered.flac",
    val timerFinishedSound: String = "asset:///sounds/timer_finished.flac",
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
}