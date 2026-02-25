package com.example.ava.settings

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.ava.utils.getRandomMacAddressString
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class VoiceSatelliteSettings(
    val name: String,
    val serverPort: Int,
    val macAddress: String,
    val haRemoteUrl: String = "",
    val haMediaPlayerEntity: String = ""
)

private fun generateUniquePort(): Int {
    return 6053 + Random.nextInt(1, 1000)
}







val DEFAULT_MAC_ADDRESS = "00:00:00:00:00:00"


val DEFAULT_DEVICE_NAME: String
    get() = "${(Build.MODEL ?: "Android").replace(" ", "_")}_voice_assistant"

private val DEFAULT = VoiceSatelliteSettings(
    name = DEFAULT_DEVICE_NAME,
    serverPort = 6053,
    macAddress = DEFAULT_MAC_ADDRESS
)

val Context.voiceSatelliteSettingsStore: DataStore<VoiceSatelliteSettings> by dataStore(
    fileName = "voice_satellite_settings.json",
    serializer = SettingsSerializer(VoiceSatelliteSettings.serializer(), DEFAULT),
    corruptionHandler = defaultCorruptionHandler(DEFAULT)
)

class VoiceSatelliteSettingsStore(dataStore: DataStore<VoiceSatelliteSettings>) :
    SettingsStoreImpl<VoiceSatelliteSettings>(dataStore, DEFAULT) {
    suspend fun saveName(name: String) =
        update { it.copy(name = name) }

    suspend fun saveServerPort(serverPort: Int) =
        update { it.copy(serverPort = serverPort) }

    suspend fun saveHaRemoteUrl(url: String) =
        update { it.copy(haRemoteUrl = url) }

    suspend fun saveHaMediaPlayerEntity(entity: String) =
        update { it.copy(haMediaPlayerEntity = entity) }
    
    suspend fun ensureMacAddressIsSet() {
        update {
            if (it.macAddress == DEFAULT_MAC_ADDRESS) {
                val newMac = getRandomMacAddressString()
                val macSuffix = newMac.takeLast(5).replace(":", "")
                val uniqueName = "${(Build.MODEL ?: "Android").replace(" ", "_")}_${macSuffix}_voice_assistant"
                val uniquePort = generateUniquePort()
                it.copy(macAddress = newMac, name = uniqueName, serverPort = uniquePort)
            } else if (it.serverPort == 6053) {
                val uniquePort = generateUniquePort()
                it.copy(serverPort = uniquePort)
            } else {
                it
            }
        }
    }
}