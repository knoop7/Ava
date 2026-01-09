package com.example.ava.utils

import android.os.Build
import android.util.Log


object GpioAecController {
    private const val TAG = "GpioAecController"
    
    
    private const val HOLI_GPIO_KEY = "holi.gpio144"
    
    
    private const val OCOCCI_GPIO_KEY = "persist.sys.gpio116"
    
    private var deviceType: DeviceType? = null
    
    private enum class DeviceType {
        OCOCCI_A64,  
        HOLI,        
        UNSUPPORTED  
    }
    
    
    private fun detectDeviceType(): DeviceType {
        if (deviceType == null) {
            val model = Build.MODEL
            val device = Build.DEVICE
            
            deviceType = when {
                
                model.contains("QUAD-CORE A64", ignoreCase = true) && 
                model.contains("ococci", ignoreCase = true) -> {
                    Log.d(TAG, "Detected Ococci A64 device: $model")
                    DeviceType.OCOCCI_A64
                }
                
                SystemPropertiesProxy.get(HOLI_GPIO_KEY, "").isNotEmpty() -> {
                    Log.d(TAG, "Detected Holi device: $model")
                    DeviceType.HOLI
                }
                
                else -> {
                    Log.d(TAG, "Unsupported device for GPIO AEC: $model")
                    DeviceType.UNSUPPORTED
                }
            }
        }
        return deviceType!!
    }
    
    
    fun isSupported(): Boolean {
        return detectDeviceType() != DeviceType.UNSUPPORTED
    }
    
    
    fun activateAEC(): Boolean {
        return when (detectDeviceType()) {
            DeviceType.OCOCCI_A64 -> {
                val success = activateAecForOcocci()
                Log.d(TAG, "AEC activated for Ococci: $success")
                success
            }
            DeviceType.HOLI -> {
                val success = setGpio(HOLI_GPIO_KEY, "0")  
                Log.d(TAG, "AEC activated for Holi: $success")
                success
            }
            DeviceType.UNSUPPORTED -> {
                
                false
            }
        }
    }
    
    
    fun activateBeamforming(): Boolean {
        return when (detectDeviceType()) {
            DeviceType.OCOCCI_A64 -> {
                val success = deactivateAecForOcocci()
                Log.d(TAG, "Beamforming activated for Ococci: $success")
                success
            }
            DeviceType.HOLI -> {
                val success = setGpio(HOLI_GPIO_KEY, "1")  
                Log.d(TAG, "Beamforming activated for Holi: $success")
                success
            }
            DeviceType.UNSUPPORTED -> {
                
                false
            }
        }
    }
    
    
    private fun activateAecForOcocci(): Boolean {
        if (RootUtils.isRootAvailable()) {
            return activateAecViaRoot()
        }
        if (ShizukuUtils.isShizukuPermissionGranted()) {
            return activateAecViaShizuku()
        }
        return setGpio(OCOCCI_GPIO_KEY, "1")
    }
    
    
    private fun deactivateAecForOcocci(): Boolean {
        if (RootUtils.isRootAvailable()) {
            return deactivateAecViaRoot()
        }
        if (ShizukuUtils.isShizukuPermissionGranted()) {
            return deactivateAecViaShizuku()
        }
        return setGpio(OCOCCI_GPIO_KEY, "0")
    }
    
    private fun setGpio(key: String, value: String): Boolean {
        val currentValue = SystemPropertiesProxy.get(key, "")
        if (currentValue == value) {
            return true  
        }
        return SystemPropertiesProxy.set(key, value)
    }
    
    
    private fun activateAecViaRoot(): Boolean {
        return try {
            
            Runtime.getRuntime().exec("su -c echo 1 > /sys/class/gpio/gpio116/value").waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate AEC via Root", e)
            false
        }
    }
    
    
    private fun deactivateAecViaRoot(): Boolean {
        return try {
            Runtime.getRuntime().exec("su -c echo 0 > /sys/class/gpio/gpio116/value").waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate AEC via Root", e)
            false
        }
    }
    
    
    private fun activateAecViaShizuku(): Boolean {
        return try {
            val (exitCode, _) = ShizukuUtils.executeCommand("echo 1 > /sys/class/gpio/gpio116/value")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate AEC via Shizuku", e)
            false
        }
    }
    
    
    private fun deactivateAecViaShizuku(): Boolean {
        return try {
            val (exitCode, _) = ShizukuUtils.executeCommand("echo 0 > /sys/class/gpio/gpio116/value")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deactivate AEC via Shizuku", e)
            false
        }
    }
    
    
    fun isAecActive(): Boolean {
        return when (detectDeviceType()) {
            DeviceType.OCOCCI_A64 -> SystemPropertiesProxy.get(OCOCCI_GPIO_KEY, "0") == "1"
            DeviceType.HOLI -> SystemPropertiesProxy.get(HOLI_GPIO_KEY, "1") == "0"
            DeviceType.UNSUPPORTED -> false
        }
    }
}
