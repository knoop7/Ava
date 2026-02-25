package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class ScreensaverSettings(
    val enabled: Boolean = false,
    val visible: Boolean = true,
    val enableHaDisplay: Boolean = false,
    val screensaverUrl: String = "https://flipflow.neverup.cn/clock.html",
    val screensaverUrlVisible: Boolean = false,
    val xiaomiWallpaperEnabled: Boolean = false,
    val timeoutSeconds: Int = 300,
    val darkOffEnabled: Boolean = false,
    val backgroundPauseEnabled: Boolean = false,
    val motionOnEnabled: Boolean = false
)

val Context.screensaverSettingsStore: DataStore<ScreensaverSettings> by dataStore(
    fileName = "screensaver_settings.json",
    serializer = SettingsSerializer(ScreensaverSettings.serializer(), ScreensaverSettings()),
    corruptionHandler = defaultCorruptionHandler(ScreensaverSettings())
)

class ScreensaverSettingsStore(dataStore: DataStore<ScreensaverSettings>) :
    SettingsStoreImpl<ScreensaverSettings>(dataStore, ScreensaverSettings()) {
    val enabled =
        SettingState(getFlow().map { it.enabled }) { value -> update { it.copy(enabled = value) } }
    val visible =
        SettingState(getFlow().map { it.visible }) { value -> update { it.copy(visible = value) } }
    val enableHaDisplay =
        SettingState(getFlow().map { it.enableHaDisplay }) { value -> update { it.copy(enableHaDisplay = value) } }
    val screensaverUrl =
        SettingState(getFlow().map { it.screensaverUrl }) { value -> update { it.copy(screensaverUrl = value) } }
    val timeoutSeconds =
        SettingState(getFlow().map { it.timeoutSeconds }) { value ->
            update { it.copy(timeoutSeconds = value.coerceIn(10, 3600)) }
        }
    val darkOffEnabled =
        SettingState(getFlow().map { it.darkOffEnabled }) { value -> update { it.copy(darkOffEnabled = value) } }
    val backgroundPauseEnabled =
        SettingState(getFlow().map { it.backgroundPauseEnabled }) { value ->
            update { it.copy(backgroundPauseEnabled = value) }
        }
    val motionOnEnabled =
        SettingState(getFlow().map { it.motionOnEnabled }) { value -> update { it.copy(motionOnEnabled = value) } }
    val screensaverUrlVisible =
        SettingState(getFlow().map { it.screensaverUrlVisible }) { value -> update { it.copy(screensaverUrlVisible = value) } }
    val xiaomiWallpaperEnabled =
        SettingState(getFlow().map { it.xiaomiWallpaperEnabled }) { value -> update { it.copy(xiaomiWallpaperEnabled = value) } }
}
