package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.ava.utils.getRandomMacAddressString
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class VoiceSatelliteSettings(
    val name: String,
    val serverPort: Int,
    val macAddress: String,
    val wakeWord: String,
    val stopWord: String
)

// The voice satellite uses a mac address as a unique identifier.
// The use of the actual mac address on Android is discouraged/not available
// depending on the Android version.
// Instead a random string of bytes should be generated and persisted to the settings.
// The default value below should only used to detect when a random value hasn't been
// generated and persisted yet and should be replaced with a random value when it is.
val DEFAULT_MAC_ADDRESS = "00:00:00:00:00:00"

private val DEFAULT = VoiceSatelliteSettings(
    name = "Android Voice Assistant",
    serverPort = 6053,
    macAddress = DEFAULT_MAC_ADDRESS,
    wakeWord = "okay_nabu",
    stopWord = "stop"
)

val Context.voiceSatelliteSettingsStore: DataStore<VoiceSatelliteSettings> by dataStore(
    fileName = "voice_satellite_settings.json",
    serializer = SettingsSerializer(VoiceSatelliteSettings.serializer(), DEFAULT),
    corruptionHandler = defaultCorruptionHandler(DEFAULT)
)

class VoiceSatelliteSettingsStore(dataStore: DataStore<VoiceSatelliteSettings>) :
    SettingsStoreImpl<VoiceSatelliteSettings>(dataStore, DEFAULT) {

    val wakeWord =
        SettingState(getFlow().map { it.wakeWord }) { value -> update { it.copy(wakeWord = value) } }
    val stopWord =
        SettingState(getFlow().map { it.stopWord }) { value -> update { it.copy(stopWord = value) } }

    suspend fun saveName(name: String) =
        update { it.copy(name = name) }

    suspend fun saveServerPort(serverPort: Int) =
        update { it.copy(serverPort = serverPort) }

    suspend fun ensureMacAddressIsSet() {
        update {
            if (it.macAddress == DEFAULT_MAC_ADDRESS) it.copy(macAddress = getRandomMacAddressString()) else it
        }
    }
}