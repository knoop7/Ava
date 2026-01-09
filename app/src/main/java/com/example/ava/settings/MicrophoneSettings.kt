package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class MicrophoneSettings(
    val wakeWord: String = "okay_nabu",
    val stopWord: String = "stop",
    val muted: Boolean = false
)

val Context.microphoneSettingsStore: DataStore<MicrophoneSettings> by dataStore(
    fileName = "microphone_settings.json",
    serializer = SettingsSerializer(MicrophoneSettings.serializer(), MicrophoneSettings()),
    corruptionHandler = defaultCorruptionHandler(MicrophoneSettings())
)

class MicrophoneSettingsStore(dataStore: DataStore<MicrophoneSettings>) :
    SettingsStoreImpl<MicrophoneSettings>(dataStore, MicrophoneSettings()) {
    val wakeWord =
        SettingState(getFlow().map { it.wakeWord }) { value -> update { it.copy(wakeWord = value) } }
    val stopWord =
        SettingState(getFlow().map { it.stopWord }) { value -> update { it.copy(stopWord = value) } }
    val muted =
        SettingState(getFlow().map { it.muted }) { value -> update { it.copy(muted = value) } }
}