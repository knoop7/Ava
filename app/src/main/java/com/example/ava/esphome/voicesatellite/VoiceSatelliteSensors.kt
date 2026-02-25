package com.example.ava.esphome.voicesatellite

import android.content.Context
import android.util.Log
import com.example.ava.R
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.SensorEntity
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VoiceSatelliteSensors(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice,
    private val experimentalSettingsStore: ExperimentalSettingsStore
) {
    private var environmentSensorManager: com.example.ava.sensor.EnvironmentSensorManager? = null
    private var lightSensorEntity: SensorEntity? = null
    private var magneticSensorEntity: SensorEntity? = null
    private var sensorUpdateJob: Job? = null

    companion object {
        private const val TAG = "VoiceSatelliteSensors"
    }

    fun init() {
        environmentSensorManager = com.example.ava.sensor.EnvironmentSensorManager(context)
        val sensorManager = environmentSensorManager ?: return
        
        if (sensorManager.hasLightSensor) {
            lightSensorEntity = SensorEntity(
                key = 20,
                name = context.getString(R.string.entity_light_sensor),
                objectId = "light_sensor",
                icon = "mdi:brightness-6",
                unitOfMeasurement = "lx",
                accuracyDecimals = 0,
                deviceClass = "illuminance",
                entityCategory = EntityCategory.ENTITY_CATEGORY_NONE
            )
            lightSensorEntity?.let { device.addEntity(it) }
        }
        
        if (sensorManager.hasMagneticSensor) {
            magneticSensorEntity = SensorEntity(
                key = 21,
                name = context.getString(R.string.entity_magnetic_sensor),
                objectId = "magnetic_sensor",
                icon = "mdi:magnet",
                unitOfMeasurement = "μT",
                accuracyDecimals = 1,
                entityCategory = EntityCategory.ENTITY_CATEGORY_NONE
            )
            magneticSensorEntity?.let { device.addEntity(it) }
        }
        
        sensorManager.startListening()
        startSensorUpdateLoop()
    }
    
    private fun startSensorUpdateLoop() {
        sensorUpdateJob?.cancel()
        sensorUpdateJob = scope.launch {
            delay(500)
            updateSensorValuesFiltered()
            
            while (true) {
                val settings = experimentalSettingsStore.get()
                val intervalMs = settings.sensorUpdateInterval.coerceIn(10, 60) * 1000L
                delay(intervalMs)
                updateSensorValues()
            }
        }
    }
    
    private suspend fun updateSensorValuesFiltered() {
        val manager = environmentSensorManager ?: return
        
        val lightSamples = mutableListOf<Float>()
        val magneticSamples = mutableListOf<Float>()
        
        repeat(3) {
            lightSamples.add(manager.lightLevel.value)
            magneticSamples.add(manager.magneticField.value)
            delay(100)
        }
        
        lightSensorEntity?.updateState(lightSamples.sorted()[1])
        magneticSensorEntity?.updateState(magneticSamples.sorted()[1])
    }
    
    private fun updateSensorValues() {
        environmentSensorManager?.let { manager ->
            lightSensorEntity?.updateState(manager.lightLevel.value)
            magneticSensorEntity?.updateState(manager.magneticField.value)
        }
    }
    
    fun stop() {
        sensorUpdateJob?.cancel()
        sensorUpdateJob = null
        environmentSensorManager?.stopListening()
        environmentSensorManager = null
    }
}
