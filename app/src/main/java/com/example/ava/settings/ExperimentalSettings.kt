package com.example.ava.settings

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream


enum class CameraPosition {
    BACK,   
    FRONT   
}


enum class CameraMode {
    SNAPSHOT,  
    VIDEO      
}


@Serializable
data class ExperimentalSettings(
    val cameraEnabled: Boolean = false,           
    val cameraMode: String = CameraMode.SNAPSHOT.name,  
    val cameraPosition: String = CameraPosition.FRONT.name,
    val imageSize: Int = 500,  
    val videoFps: Int = 2,        
    val videoResolution: Int = 240,
    val personDetectionEnabled: Boolean = false,
    val faceBoxEnabled: Boolean = true,
    
    val environmentSensorEnabled: Boolean = false,
    val sensorUpdateInterval: Int = 35,  
    
    val proximitySensorEnabled: Boolean = false,
    val proximitySendToHass: Boolean = false,  
    val proximityWakeScreen: Boolean = true,  
    val proximityAwayDelay: Int = 30,  
    val proximityAutoUnlock: Boolean = false,
    
    val screenBrightnessEnabled: Boolean = false,
    
    val forceOrientationEnabled: Boolean = false,
    val forceOrientationMode: String = "portrait",  
    
    val displaySizeEnabled: Boolean = false,
    val displaySizeScale: Float = 1.0f,  
    
    val diagnosticSensorEnabled: Boolean = false,
    val diagnosticWifiEnabled: Boolean = false,
    val diagnosticIpEnabled: Boolean = false,
    val diagnosticStorageEnabled: Boolean = false,
    val diagnosticMemoryEnabled: Boolean = false,
    val diagnosticUptimeEnabled: Boolean = false,
    val diagnosticKillAppEnabled: Boolean = false,
    val diagnosticRebootEnabled: Boolean = false,
    val diagnosticBatteryLevelEnabled: Boolean = false,
    val diagnosticBatteryVoltageEnabled: Boolean = false,
    val diagnosticChargingStatusEnabled: Boolean = false,
    
    val intentLauncherEnabled: Boolean = false,
    val intentLauncherHaDisplayEnabled: Boolean = false,
    
    val microphoneVolume: Float = 1.0f,
    val multiDeviceArbiterEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = ExperimentalSettings()
    }
}


object ExperimentalSettingsSerializer : Serializer<ExperimentalSettings> {
    override val defaultValue = ExperimentalSettings.DEFAULT
    
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): ExperimentalSettings {
        return try {
            json.decodeFromString(
                ExperimentalSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            
            ExperimentalSettings.DEFAULT
        }
    }

    override suspend fun writeTo(t: ExperimentalSettings, output: OutputStream) {
        output.write(json.encodeToString(ExperimentalSettings.serializer(), t).encodeToByteArray())
    }
}

val Context.experimentalSettingsDataStore: DataStore<ExperimentalSettings> by dataStore(
    fileName = "experimental_settings.json",
    serializer = ExperimentalSettingsSerializer
)


class ExperimentalSettingsStore(context: Context) : SettingsStoreImpl<ExperimentalSettings>(
    context.experimentalSettingsDataStore,
    ExperimentalSettings.DEFAULT
) {
    private val appContext = context.applicationContext
    
    
    fun hasCamera(): Boolean = com.example.ava.utils.DeviceCapabilities.hasCamera(appContext)
    
    
    fun hasBackCamera(): Boolean = com.example.ava.utils.DeviceCapabilities.hasBackCamera(appContext)
    
    
    fun hasFrontCamera(): Boolean = com.example.ava.utils.DeviceCapabilities.hasFrontCamera(appContext)
    
    
    val cameraEnabled: Flow<Boolean> = getFlow().map { it.cameraEnabled }
    val cameraMode: Flow<CameraMode> = getFlow().map {
        try {
            CameraMode.valueOf(it.cameraMode)
        } catch (e: Exception) {
            CameraMode.SNAPSHOT
        }
    }
    val cameraPosition: Flow<CameraPosition> = getFlow().map { 
        try {
            CameraPosition.valueOf(it.cameraPosition)
        } catch (e: Exception) {
            CameraPosition.FRONT
        }
    }
    val imageSize: Flow<Int> = getFlow().map { it.imageSize }
    val videoFps: Flow<Int> = getFlow().map { it.videoFps }
    val videoResolution: Flow<Int> = getFlow().map { it.videoResolution }
    
    suspend fun setCameraEnabled(enabled: Boolean) {
        update { it.copy(cameraEnabled = enabled) }
    }
    
    suspend fun setCameraMode(mode: CameraMode) {
        update { it.copy(cameraMode = mode.name) }
    }
    
    suspend fun setCameraPosition(position: CameraPosition) {
        update { it.copy(cameraPosition = position.name) }
    }
    
    suspend fun setImageSize(size: Int) {
        
        update { it.copy(imageSize = if (size == 0) 0 else size.coerceIn(100, 2000)) }
    }
    
    suspend fun setVideoFps(fps: Int) {
        update { it.copy(videoFps = fps.coerceIn(1, 15)) }
    }
    
    suspend fun setVideoResolution(resolution: Int) {
        update { it.copy(videoResolution = resolution.coerceIn(240, 720)) }
    }
    
    val personDetectionEnabled: Flow<Boolean> = getFlow().map { it.personDetectionEnabled }
    
    suspend fun setPersonDetectionEnabled(enabled: Boolean) {
        update { it.copy(personDetectionEnabled = enabled) }
    }
    
    val faceBoxEnabled: Flow<Boolean> = getFlow().map { it.faceBoxEnabled }
    
    suspend fun setFaceBoxEnabled(enabled: Boolean) {
        update { it.copy(faceBoxEnabled = enabled) }
    }
    
    val environmentSensorEnabled: Flow<Boolean> = getFlow().map { it.environmentSensorEnabled }
    val sensorUpdateInterval: Flow<Int> = getFlow().map { it.sensorUpdateInterval }
    
    suspend fun setEnvironmentSensorEnabled(enabled: Boolean) {
        update { it.copy(environmentSensorEnabled = enabled) }
    }
    
    suspend fun setSensorUpdateInterval(interval: Int) {
        update { it.copy(sensorUpdateInterval = interval.coerceIn(10, 60)) }
    }
    
    
    val proximitySensorEnabled: Flow<Boolean> = getFlow().map { it.proximitySensorEnabled }
    val proximitySendToHass: Flow<Boolean> = getFlow().map { it.proximitySendToHass }
    val proximityWakeScreen: Flow<Boolean> = getFlow().map { it.proximityWakeScreen }
    val proximityAwayDelay: Flow<Int> = getFlow().map { it.proximityAwayDelay }
    val proximityAutoUnlock: Flow<Boolean> = getFlow().map { it.proximityAutoUnlock }
    
    suspend fun setProximitySensorEnabled(enabled: Boolean) {
        update { it.copy(proximitySensorEnabled = enabled) }
    }
    
    suspend fun setProximitySendToHass(enabled: Boolean) {
        update { it.copy(proximitySendToHass = enabled) }
    }
    
    suspend fun setProximityWakeScreen(enabled: Boolean) {
        update { it.copy(proximityWakeScreen = enabled) }
    }
    
    suspend fun setProximityAwayDelay(delay: Int) {
        update { it.copy(proximityAwayDelay = delay.coerceIn(10, 120)) }
    }
    
    suspend fun setProximityAutoUnlock(enabled: Boolean) {
        update { it.copy(proximityAutoUnlock = enabled) }
    }
    
    
    val screenBrightnessEnabled: Flow<Boolean> = getFlow().map { it.screenBrightnessEnabled }
    
    suspend fun setScreenBrightnessEnabled(enabled: Boolean) {
        update { it.copy(screenBrightnessEnabled = enabled) }
    }
    
    
    val forceOrientationEnabled: Flow<Boolean> = getFlow().map { it.forceOrientationEnabled }
    val forceOrientationMode: Flow<String> = getFlow().map { it.forceOrientationMode }
    
    suspend fun setForceOrientationEnabled(enabled: Boolean) {
        update { it.copy(forceOrientationEnabled = enabled) }
    }
    
    suspend fun setForceOrientationMode(mode: String) {
        update { it.copy(forceOrientationMode = mode) }
    }
    
    
    val displaySizeEnabled: Flow<Boolean> = getFlow().map { it.displaySizeEnabled }
    val displaySizeScale: Flow<Float> = getFlow().map { it.displaySizeScale }
    
    suspend fun setDisplaySizeEnabled(enabled: Boolean) {
        update { it.copy(displaySizeEnabled = enabled) }
    }
    
    suspend fun setDisplaySizeScale(scale: Float) {
        update { it.copy(displaySizeScale = scale.coerceIn(0.5f, 1.0f)) }
    }
    
    
    val diagnosticSensorEnabled: Flow<Boolean> = getFlow().map { it.diagnosticSensorEnabled }
    val diagnosticWifiEnabled: Flow<Boolean> = getFlow().map { it.diagnosticWifiEnabled }
    val diagnosticIpEnabled: Flow<Boolean> = getFlow().map { it.diagnosticIpEnabled }
    val diagnosticStorageEnabled: Flow<Boolean> = getFlow().map { it.diagnosticStorageEnabled }
    val diagnosticMemoryEnabled: Flow<Boolean> = getFlow().map { it.diagnosticMemoryEnabled }
    val diagnosticUptimeEnabled: Flow<Boolean> = getFlow().map { it.diagnosticUptimeEnabled }
    
    suspend fun setDiagnosticSensorEnabled(enabled: Boolean) {
        update { it.copy(diagnosticSensorEnabled = enabled) }
    }
    
    suspend fun setDiagnosticWifiEnabled(enabled: Boolean) {
        update { it.copy(diagnosticWifiEnabled = enabled) }
    }
    
    suspend fun setDiagnosticIpEnabled(enabled: Boolean) {
        update { it.copy(diagnosticIpEnabled = enabled) }
    }
    
    suspend fun setDiagnosticStorageEnabled(enabled: Boolean) {
        update { it.copy(diagnosticStorageEnabled = enabled) }
    }
    
    suspend fun setDiagnosticMemoryEnabled(enabled: Boolean) {
        update { it.copy(diagnosticMemoryEnabled = enabled) }
    }
    
    suspend fun setDiagnosticUptimeEnabled(enabled: Boolean) {
        update { it.copy(diagnosticUptimeEnabled = enabled) }
    }
    
    val diagnosticKillAppEnabled: Flow<Boolean> = getFlow().map { it.diagnosticKillAppEnabled }
    val diagnosticRebootEnabled: Flow<Boolean> = getFlow().map { it.diagnosticRebootEnabled }
    
    suspend fun setDiagnosticKillAppEnabled(enabled: Boolean) {
        update { it.copy(diagnosticKillAppEnabled = enabled) }
    }
    
    suspend fun setDiagnosticRebootEnabled(enabled: Boolean) {
        update { it.copy(diagnosticRebootEnabled = enabled) }
    }
    
    val diagnosticBatteryLevelEnabled: Flow<Boolean> = getFlow().map { it.diagnosticBatteryLevelEnabled }
    val diagnosticBatteryVoltageEnabled: Flow<Boolean> = getFlow().map { it.diagnosticBatteryVoltageEnabled }
    
    suspend fun setDiagnosticBatteryLevelEnabled(enabled: Boolean) {
        update { it.copy(diagnosticBatteryLevelEnabled = enabled) }
    }
    
    suspend fun setDiagnosticBatteryVoltageEnabled(enabled: Boolean) {
        update { it.copy(diagnosticBatteryVoltageEnabled = enabled) }
    }
    
    val diagnosticChargingStatusEnabled: Flow<Boolean> = getFlow().map { it.diagnosticChargingStatusEnabled }
    
    suspend fun setDiagnosticChargingStatusEnabled(enabled: Boolean) {
        update { it.copy(diagnosticChargingStatusEnabled = enabled) }
    }
    
    val intentLauncherEnabled: Flow<Boolean> = getFlow().map { it.intentLauncherEnabled }
    val intentLauncherHaDisplayEnabled: Flow<Boolean> = getFlow().map { it.intentLauncherHaDisplayEnabled }
    
    suspend fun setIntentLauncherEnabled(enabled: Boolean) {
        update { it.copy(intentLauncherEnabled = enabled) }
    }
    
    suspend fun setIntentLauncherHaDisplayEnabled(enabled: Boolean) {
        update { it.copy(intentLauncherHaDisplayEnabled = enabled) }
    }
    
    val microphoneVolume: Flow<Float> = getFlow().map { it.microphoneVolume }
    
    suspend fun setMicrophoneVolume(volume: Float) {
        update { it.copy(microphoneVolume = volume.coerceIn(0.0f, 2.0f)) }
    }
    
    val multiDeviceArbiterEnabled: Flow<Boolean> = getFlow().map { it.multiDeviceArbiterEnabled }
    
    suspend fun setMultiDeviceArbiterEnabled(enabled: Boolean) {
        update { it.copy(multiDeviceArbiterEnabled = enabled) }
    }
}
