package com.example.ava.utils

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import java.io.File


object DeviceCapabilities {
    
    private const val TAG = "DeviceCapabilities"
    
    
    private var _hasCamera: Boolean? = null
    private var _hasLightSensor: Boolean? = null
    private var _hasProximitySensor: Boolean? = null
    private var _hasTemperatureSensor: Boolean? = null
    private var _hasHumiditySensor: Boolean? = null
    private var _hasPressureSensor: Boolean? = null
    private var _isA64Device: Boolean? = null
    
    
    fun isA64Device(): Boolean {
        if (_isA64Device != null) return _isA64Device!!
        
        _isA64Device = try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            val cpuInfoLower = cpuInfo.lowercase()
            val modelLower = (Build.MODEL ?: "").lowercase()
            val boardLower = (Build.BOARD ?: "").lowercase()
            val hardwareLower = (Build.HARDWARE ?: "").lowercase()
            cpuInfoLower.contains("a64") ||
            cpuInfoLower.contains("sun50i") ||
            cpuInfoLower.contains("allwinner") ||
            modelLower.contains("a64") ||
            modelLower.contains("ococci") ||
            boardLower.contains("a64") ||
            boardLower.contains("sun50i") ||
            hardwareLower.contains("a64") ||
            hardwareLower.contains("sun50i") ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cpuinfo", e)
            false
        }
        return _isA64Device!!
    }
    
    
    fun hasCamera(context: Context): Boolean {
        
        try {
            val devDir = File("/dev")
            if (devDir.exists()) {
                val videoDevices = devDir.listFiles { file -> file.name.startsWith("video") }
                if (videoDevices != null && videoDevices.isNotEmpty()) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check /dev/video*", e)
        }
        
        
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isNotEmpty()) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check CameraManager", e)
        }
        
        
        try {
            val pm = context.packageManager
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check PackageManager features", e)
        }
        
        return false
    }
    
    
    fun hasFrontCamera(context: Context): Boolean {
        
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check front camera via CameraManager", e)
        }
        
        
        try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check FEATURE_CAMERA_FRONT", e)
        }
        
        return false
    }
    
    
    fun hasBackCamera(context: Context): Boolean {
        
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check back camera via CameraManager", e)
        }
        
        
        try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check FEATURE_CAMERA", e)
        }
        
        return false
    }
    
    
    fun hasLightSensor(context: Context): Boolean {
        if (_hasLightSensor != null) return _hasLightSensor!!
        
        _hasLightSensor = try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check light sensor", e)
            false
        }
        return _hasLightSensor!!
    }
    
    
    fun hasProximitySensor(context: Context): Boolean {
        if (_hasProximitySensor != null) return _hasProximitySensor!!
        
        _hasProximitySensor = try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check proximity sensor", e)
            false
        }
        return _hasProximitySensor!!
    }
    
    
    fun hasTemperatureSensor(context: Context): Boolean {
        if (_hasTemperatureSensor != null) return _hasTemperatureSensor!!
        
        _hasTemperatureSensor = try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check temperature sensor", e)
            false
        }
        return _hasTemperatureSensor!!
    }
    
    
    fun hasHumiditySensor(context: Context): Boolean {
        if (_hasHumiditySensor != null) return _hasHumiditySensor!!
        
        _hasHumiditySensor = try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check humidity sensor", e)
            false
        }
        return _hasHumiditySensor!!
    }
    
    
    fun hasPressureSensor(context: Context): Boolean {
        if (_hasPressureSensor != null) return _hasPressureSensor!!
        
        _hasPressureSensor = try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check pressure sensor", e)
            false
        }
        return _hasPressureSensor!!
    }
    
    
    fun hasAnyEnvironmentSensor(context: Context): Boolean {
        return hasLightSensor(context) || 
               hasTemperatureSensor(context) || 
               hasHumiditySensor(context) || 
               hasPressureSensor(context)
    }
    
    
    fun clearCache() {
        _hasCamera = null
        _hasLightSensor = null
        _hasProximitySensor = null
        _hasTemperatureSensor = null
        _hasHumiditySensor = null
        _hasPressureSensor = null
        _isA64Device = null
    }
}
