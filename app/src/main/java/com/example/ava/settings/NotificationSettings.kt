package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val sceneDisplayDuration: Int = 5000,  
    val customSceneUrl: String = "",  
    val soundEnabled: Boolean = false,  
    val soundUri: String = ""  
)

val Context.notificationSettingsStore: DataStore<NotificationSettings> by dataStore(
    fileName = "notification_settings.json",
    serializer = SettingsSerializer(NotificationSettings.serializer(), NotificationSettings()),
    corruptionHandler = defaultCorruptionHandler(NotificationSettings())
)

class NotificationSettingsStore(dataStore: DataStore<NotificationSettings>) :
    SettingsStoreImpl<NotificationSettings>(dataStore, NotificationSettings()) {
    val sceneDisplayDuration =
        SettingState(getFlow().map { it.sceneDisplayDuration }) { value -> update { it.copy(sceneDisplayDuration = value) } }
    
    val customSceneUrl =
        SettingState(getFlow().map { it.customSceneUrl }) { value -> update { it.copy(customSceneUrl = value) } }
    
    val soundEnabled =
        SettingState(getFlow().map { it.soundEnabled }) { value -> update { it.copy(soundEnabled = value) } }
    
    val soundUri =
        SettingState(getFlow().map { it.soundUri }) { value -> update { it.copy(soundUri = value) } }
}
