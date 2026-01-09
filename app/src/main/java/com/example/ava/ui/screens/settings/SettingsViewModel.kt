package com.example.ava.ui.screens.settings

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.example.ava.R
import com.example.ava.microwakeword.AssetWakeWordProvider
import com.example.ava.microwakeword.WakeWordProvider
import com.example.ava.microwakeword.WakeWordWithId
import com.example.ava.settings.MicrophoneSettingsStore
import com.example.ava.settings.NotificationSettingsStore
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.ScreensaverSettingsStore
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.ava.settings.microphoneSettingsStore
import com.example.ava.settings.notificationSettingsStore
import com.example.ava.settings.playerSettingsStore
import com.example.ava.settings.screensaverSettingsStore
import com.example.ava.settings.voiceSatelliteSettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map

@Immutable
data class UIState(
    val serverName: String,
    val serverPort: Int
)

@Immutable
data class MicrophoneState(
    val wakeWord: WakeWordWithId,
    val wakeWords: List<WakeWordWithId>
)

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val satelliteSettingsStore =
        VoiceSatelliteSettingsStore(application.voiceSatelliteSettingsStore)
    private val microphoneSettingsStore =
        MicrophoneSettingsStore(application.microphoneSettingsStore)
    private val playerSettingsStore = PlayerSettingsStore(application.playerSettingsStore)
    private val notificationSettingsStore = NotificationSettingsStore(application.notificationSettingsStore)
    private val screensaverSettingsStore = ScreensaverSettingsStore(application.screensaverSettingsStore)
    private val experimentalSettingsStore = com.example.ava.settings.ExperimentalSettingsStore(application)
    private val wakeWordProvider: WakeWordProvider = AssetWakeWordProvider(application.assets)
    private val wakeWords = wakeWordProvider.getWakeWords()

    val satelliteSettingsState = satelliteSettingsStore.getFlow().map {
        UIState(
            serverName = it.name,
            serverPort = it.serverPort
        )
    }

    val microphoneSettingsState = microphoneSettingsStore.getFlow().map {
        MicrophoneState(
            wakeWord = wakeWords.firstOrNull { wakeWord ->
                wakeWord.id == it.wakeWord
            } ?: wakeWords.first(),
            wakeWords = wakeWords
        )
    }

    val playerSettingsState = playerSettingsStore.getFlow()
    
    val notificationSettingsState = notificationSettingsStore.getFlow()
    
    val experimentalSettingsState = experimentalSettingsStore.getFlow()

    val screensaverSettingsState = screensaverSettingsStore.getFlow()
    
    
    fun hasCamera() = experimentalSettingsStore.hasCamera()
    fun hasBackCamera() = experimentalSettingsStore.hasBackCamera()
    fun hasFrontCamera() = experimentalSettingsStore.hasFrontCamera()

    suspend fun saveServerName(name: String) {
        if (validateName(name).isNullOrBlank()) {
            satelliteSettingsStore.saveName(name)
        } else {
            Log.w(TAG, "Cannot save invalid server name: $name")
        }
    }

    suspend fun saveServerPort(port: Int?) {
        if (validatePort(port).isNullOrBlank()) {
            satelliteSettingsStore.saveServerPort(port!!)
        } else {
            Log.w(TAG, "Cannot save invalid server port: $port")
        }
    }

    suspend fun saveWakeWord(wakeWordId: String) {
        if (validateWakeWord(wakeWordId).isNullOrBlank()) {
            microphoneSettingsStore.wakeWord.set(wakeWordId)
        } else {
            Log.w(TAG, "Cannot save invalid wake word: $wakeWordId")
        }
    }

    suspend fun saveEnableWakeSound(enableWakeSound: Boolean) {
        playerSettingsStore.enableWakeSound.set(enableWakeSound)
    }

    suspend fun saveContinuousConversation(enabled: Boolean) {
        playerSettingsStore.enableContinuousConversation.set(enabled)
    }

    suspend fun saveFloatingWindow(enabled: Boolean) {
        playerSettingsStore.enableFloatingWindow.set(enabled)
    }
    
    suspend fun saveVinylCover(enabled: Boolean) {
        playerSettingsStore.enableVinylCover.set(enabled)
    }
    
    suspend fun saveDreamClock(enabled: Boolean) {
        playerSettingsStore.enableDreamClock.set(enabled)
    }
    
    suspend fun saveWeatherCity(city: String) {
        playerSettingsStore.weatherCity.set(city)
    }
    
    suspend fun saveWeatherOverlay(enabled: Boolean) {
        playerSettingsStore.enableWeatherOverlay.set(enabled)
    }
    
    suspend fun saveAutoRestart(enabled: Boolean) {
        playerSettingsStore.enableAutoRestart.set(enabled)
    }
    
    suspend fun saveSceneDisplayDuration(duration: Int) {
        notificationSettingsStore.sceneDisplayDuration.set(duration)
    }
    
    suspend fun saveCustomSceneUrl(url: String) {
        notificationSettingsStore.customSceneUrl.set(url)
    }
    
    suspend fun saveSoundEnabled(enabled: Boolean) {
        notificationSettingsStore.soundEnabled.set(enabled)
    }
    
    suspend fun saveSoundUri(uri: String) {
        notificationSettingsStore.soundUri.set(uri)
    }

    suspend fun saveScreensaverUrl(url: String) {
        screensaverSettingsStore.screensaverUrl.set(url)
    }

    suspend fun saveScreensaverEnabled(enabled: Boolean) {
        screensaverSettingsStore.enabled.set(enabled)
    }

    suspend fun saveScreensaverTimeout(timeoutSeconds: Int) {
        screensaverSettingsStore.timeoutSeconds.set(timeoutSeconds)
    }

    suspend fun saveScreensaverDarkOff(enabled: Boolean) {
        screensaverSettingsStore.darkOffEnabled.set(enabled)
    }

    suspend fun saveScreensaverBackgroundPause(enabled: Boolean) {
        screensaverSettingsStore.backgroundPauseEnabled.set(enabled)
    }

    suspend fun saveScreensaverMotionOn(enabled: Boolean) {
        screensaverSettingsStore.motionOnEnabled.set(enabled)
    }
    
    suspend fun saveScreensaverUrlVisible(visible: Boolean) {
        screensaverSettingsStore.screensaverUrlVisible.set(visible)
    }
    
    suspend fun saveXiaomiWallpaperEnabled(enabled: Boolean) {
        screensaverSettingsStore.xiaomiWallpaperEnabled.set(enabled)
    }
    
    suspend fun saveCameraEnabled(enabled: Boolean) {
        experimentalSettingsStore.setCameraEnabled(enabled)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    suspend fun saveCameraMode(mode: com.example.ava.settings.CameraMode) {
        experimentalSettingsStore.setCameraMode(mode)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    suspend fun saveCameraPosition(position: com.example.ava.settings.CameraPosition) {
        experimentalSettingsStore.setCameraPosition(position)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    suspend fun saveImageSize(size: Int) {
        experimentalSettingsStore.setImageSize(size)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    suspend fun saveVideoFps(fps: Int) {
        experimentalSettingsStore.setVideoFps(fps)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    suspend fun saveVideoResolution(resolution: Int) {
        experimentalSettingsStore.setVideoResolution(resolution)
        com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
    }
    
    
    suspend fun saveEnvironmentSensorEnabled(enabled: Boolean) {
        experimentalSettingsStore.setEnvironmentSensorEnabled(enabled)
    }
    
    suspend fun saveSensorUpdateInterval(interval: Int) {
        experimentalSettingsStore.setSensorUpdateInterval(interval)
    }
    
    
    suspend fun saveProximitySensorEnabled(enabled: Boolean) {
        experimentalSettingsStore.setProximitySensorEnabled(enabled)
    }
    
    suspend fun saveProximitySendToHass(enabled: Boolean) {
        experimentalSettingsStore.setProximitySendToHass(enabled)
    }
    
    suspend fun saveProximityWakeScreen(enabled: Boolean) {
        experimentalSettingsStore.setProximityWakeScreen(enabled)
    }
    
    suspend fun saveProximityAwayDelay(delay: Int) {
        experimentalSettingsStore.setProximityAwayDelay(delay)
    }
    
    suspend fun saveProximityAutoUnlock(enabled: Boolean) {
        experimentalSettingsStore.setProximityAutoUnlock(enabled)
    }
    
    
    suspend fun saveScreenBrightnessEnabled(enabled: Boolean) {
        experimentalSettingsStore.setScreenBrightnessEnabled(enabled)
    }
    
    
    suspend fun saveForceOrientationEnabled(enabled: Boolean) {
        experimentalSettingsStore.setForceOrientationEnabled(enabled)
    }
    
    suspend fun saveForceOrientationMode(mode: String) {
        experimentalSettingsStore.setForceOrientationMode(mode)
    }
    
    
    suspend fun saveDisplaySizeEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDisplaySizeEnabled(enabled)
    }
    
    suspend fun saveDisplaySizeScale(scale: Float) {
        experimentalSettingsStore.setDisplaySizeScale(scale)
    }
    
    
    suspend fun saveDiagnosticSensorEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticSensorEnabled(enabled)
    }
    
    suspend fun saveDiagnosticWifiEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticWifiEnabled(enabled)
    }
    
    suspend fun saveDiagnosticIpEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticIpEnabled(enabled)
    }
    
    suspend fun saveDiagnosticStorageEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticStorageEnabled(enabled)
    }
    
    suspend fun saveDiagnosticMemoryEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticMemoryEnabled(enabled)
    }
    
    suspend fun saveDiagnosticUptimeEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticUptimeEnabled(enabled)
    }
    
    suspend fun saveDiagnosticKillAppEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticKillAppEnabled(enabled)
    }
    
    suspend fun saveDiagnosticRebootEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticRebootEnabled(enabled)
    }
    
    suspend fun saveDiagnosticBatteryLevelEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticBatteryLevelEnabled(enabled)
    }
    
    suspend fun saveDiagnosticBatteryVoltageEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticBatteryVoltageEnabled(enabled)
    }
    
    suspend fun saveDiagnosticChargingStatusEnabled(enabled: Boolean) {
        experimentalSettingsStore.setDiagnosticChargingStatusEnabled(enabled)
    }
    
    fun validateName(name: String): String? =
        if (name.isBlank())
            application.getString(R.string.validation_voice_satellite_name_empty)
        else null


    fun validatePort(port: Int?): String? =
        if (port == null || port < 1 || port > 65535)
            application.getString(R.string.validation_voice_satellite_port_invalid)
        else null

    fun validateWakeWord(wakeWordId: String): String? {
        val wakeWordWithId = wakeWords.firstOrNull { it.id == wakeWordId }
        return if (wakeWordWithId == null)
            application.getString(R.string.validation_voice_satellite_wake_word_invalid)
        else
            null
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
