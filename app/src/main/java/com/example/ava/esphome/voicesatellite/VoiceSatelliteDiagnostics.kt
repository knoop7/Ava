package com.example.ava.esphome.voicesatellite

import android.content.Context
import com.example.ava.R
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.ButtonEntity
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.esphome.entities.ServiceArg
import com.example.ava.esphome.entities.ServiceEntity
import com.example.ava.esphome.entities.TextSensorEntity
import com.example.ava.utils.IntentLauncher
import com.example.ava.settings.ExperimentalSettings
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceSatelliteDiagnostics(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice,
    private val experimentalSettingsStore: ExperimentalSettingsStore
) {
    private var diagnosticSensorManager: com.example.ava.sensors.DiagnosticSensorManager? = null
    private var diagnosticUpdateJob: Job? = null
    
    private var wifiSignalEntity: SensorEntity? = null
    private var deviceIpEntity: TextSensorEntity? = null
    private var storageFreeEntity: SensorEntity? = null
    private var memoryUsageEntity: SensorEntity? = null
    private var deviceUptimeEntity: TextSensorEntity? = null
    private var batteryLevelEntity: SensorEntity? = null
    private var batteryVoltageEntity: SensorEntity? = null
    private var chargingStatusEntity: TextSensorEntity? = null
    private var intentLauncherStatusEntity: TextSensorEntity? = null

    companion object {
        private const val TAG = "VoiceSatelliteDiagnostics"
    }

    fun init(settings: ExperimentalSettings) {
        diagnosticSensorManager = com.example.ava.sensors.DiagnosticSensorManager(context, scope)
        diagnosticSensorManager?.start()
        
        addDiagnosticEntities(settings)
        
        diagnosticUpdateJob?.cancel()
        diagnosticUpdateJob = scope.launch {
            updateEntities(settings)
            while (isActive) {
                delay(5_000)
                updateEntities(settings)
            }
        }
    }
    
    private fun updateEntities(settings: ExperimentalSettings) {
        val manager = diagnosticSensorManager ?: return
        if (settings.diagnosticWifiEnabled) wifiSignalEntity?.updateState(manager.wifiSignal.value.toFloat())
        if (settings.diagnosticIpEnabled) deviceIpEntity?.updateState(manager.deviceIp.value)
        if (settings.diagnosticStorageEnabled) storageFreeEntity?.updateState(manager.storageFree.value)
        if (settings.diagnosticMemoryEnabled) memoryUsageEntity?.updateState(manager.memoryUsage.value)
        if (settings.diagnosticUptimeEnabled) deviceUptimeEntity?.updateState(manager.uptime.value)
        if (settings.diagnosticBatteryLevelEnabled) batteryLevelEntity?.updateState(manager.batteryLevel.value.toFloat())
        if (settings.diagnosticBatteryVoltageEnabled) batteryVoltageEntity?.updateState(manager.batteryVoltage.value)
        if (settings.diagnosticChargingStatusEnabled) chargingStatusEntity?.updateState(manager.chargingStatus.value)
    }
    
    private fun addDiagnosticEntities(settings: ExperimentalSettings) {
        if (settings.diagnosticWifiEnabled) {
            wifiSignalEntity = SensorEntity(
                key = "wifi_signal".hashCode(),
                name = context.getString(R.string.entity_wifi_signal),
                objectId = "wifi_signal",
                icon = "mdi:wifi",
                unitOfMeasurement = "dBm",
                accuracyDecimals = 0,
                deviceClass = "signal_strength",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(wifiSignalEntity!!)
        }
        if (settings.diagnosticIpEnabled) {
            deviceIpEntity = TextSensorEntity(
                key = "device_ip".hashCode(),
                name = context.getString(R.string.entity_device_ip),
                objectId = "device_ip",
                icon = "mdi:ip-network",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(deviceIpEntity!!)
        }
        if (settings.diagnosticStorageEnabled) {
            storageFreeEntity = SensorEntity(
                key = "storage_free".hashCode(),
                name = context.getString(R.string.entity_storage_free),
                objectId = "storage_free",
                icon = "mdi:harddisk",
                unitOfMeasurement = "GB",
                accuracyDecimals = 1,
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(storageFreeEntity!!)
        }
        if (settings.diagnosticMemoryEnabled) {
            memoryUsageEntity = SensorEntity(
                key = "memory_usage".hashCode(),
                name = context.getString(R.string.entity_memory_usage),
                objectId = "memory_usage",
                icon = "mdi:memory",
                unitOfMeasurement = "%",
                accuracyDecimals = 0,
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(memoryUsageEntity!!)
        }
        if (settings.diagnosticUptimeEnabled) {
            deviceUptimeEntity = TextSensorEntity(
                key = "device_uptime".hashCode(),
                name = context.getString(R.string.entity_device_uptime),
                objectId = "device_uptime",
                icon = "mdi:clock-outline",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(deviceUptimeEntity!!)
        }
        if (settings.diagnosticBatteryLevelEnabled) {
            batteryLevelEntity = SensorEntity(
                key = "battery_level".hashCode(),
                name = context.getString(R.string.entity_battery_level),
                objectId = "battery_level",
                icon = "mdi:battery",
                unitOfMeasurement = "%",
                accuracyDecimals = 0,
                deviceClass = "battery",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(batteryLevelEntity!!)
        }
        if (settings.diagnosticBatteryVoltageEnabled) {
            batteryVoltageEntity = SensorEntity(
                key = "battery_voltage".hashCode(),
                name = context.getString(R.string.entity_battery_voltage),
                objectId = "battery_voltage",
                icon = "mdi:flash",
                unitOfMeasurement = "V",
                accuracyDecimals = 2,
                deviceClass = "voltage",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(batteryVoltageEntity!!)
        }
        if (settings.diagnosticChargingStatusEnabled) {
            chargingStatusEntity = TextSensorEntity(
                key = "charging_status".hashCode(),
                name = context.getString(R.string.entity_charging_status),
                objectId = "charging_status",
                icon = "mdi:power-plug",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(chargingStatusEntity!!)
        }
        if (settings.diagnosticKillAppEnabled) {
            device.addEntity(ButtonEntity(
                key = "kill_app".hashCode(),
                name = context.getString(R.string.entity_kill_app),
                objectId = "kill_app",
                icon = "mdi:close-circle",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            ) {
                android.os.Process.killProcess(android.os.Process.myPid())
            })
        }
        if (settings.diagnosticRebootEnabled) {
            device.addEntity(ButtonEntity(
                key = "reboot_device".hashCode(),
                name = context.getString(R.string.entity_reboot_device),
                objectId = "reboot_device",
                icon = "mdi:restart",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            ) {
                try {
                    com.example.ava.utils.ShizukuUtils.rebootDevice()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to reboot device", e)
                }
            })
        }
        if (settings.intentLauncherEnabled && settings.intentLauncherHaDisplayEnabled) {
            intentLauncherStatusEntity = TextSensorEntity(
                key = "intent_launcher_status".hashCode(),
                name = context.getString(R.string.entity_intent_launcher_status),
                objectId = "intent_launcher_status",
                icon = "mdi:rocket-launch",
                entityCategory = EntityCategory.ENTITY_CATEGORY_DIAGNOSTIC
            )
            device.addEntity(intentLauncherStatusEntity!!)
            intentLauncherStatusEntity?.updateState("idle")
            
            device.addEntity(ServiceEntity(
                key = "launch_intent".hashCode(),
                name = "launch_intent",
                args = listOf(ServiceArg("intent_uri")),
                description = "Open Settings: intent:#Intent;action=android.settings.SETTINGS;end",
                onExecute = { args ->
                    val uri = args["intent_uri"] as? String ?: ""
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    intentLauncherStatusEntity?.updateState("[$timestamp] Launching: $uri")
                    val result = IntentLauncher.launch(context, uri)
                    val status = if (result.success) {
                        "[$timestamp] SUCCESS: ${result.message}"
                    } else {
                        "[$timestamp] FAILED: ${result.message}"
                    }
                    intentLauncherStatusEntity?.updateState(status)
                }
            ))
        }
    }
    
    fun stop() {
        diagnosticUpdateJob?.cancel()
        diagnosticUpdateJob = null
        diagnosticSensorManager?.stop()
        diagnosticSensorManager = null
    }
}
