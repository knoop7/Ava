package com.example.ava.sensors

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

class DiagnosticSensorManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DiagnosticSensorManager"
        private const val UPDATE_INTERVAL_MS = 35_000L
        private const val UPTIME_INTERVAL_MS = 300_000L
        private const val SMOOTHING_SAMPLES = 3
    }
    
    private val _wifiSignal = MutableStateFlow(0)
    val wifiSignal: StateFlow<Int> = _wifiSignal
    
    private val _deviceIp = MutableStateFlow("")
    val deviceIp: StateFlow<String> = _deviceIp
    
    private val _storageFree = MutableStateFlow(0f)
    val storageFree: StateFlow<Float> = _storageFree
    
    private val _memoryUsage = MutableStateFlow(0f)
    val memoryUsage: StateFlow<Float> = _memoryUsage
    
    private val _uptime = MutableStateFlow("")
    val uptime: StateFlow<String> = _uptime
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel
    
    private val _batteryVoltage = MutableStateFlow(0f)
    val batteryVoltage: StateFlow<Float> = _batteryVoltage
    
    private val _chargingStatus = MutableStateFlow("None")
    val chargingStatus: StateFlow<String> = _chargingStatus
    
    private var updateJob: Job? = null
    private val wifiSamples = mutableListOf<Int>()
    private val memorySamples = mutableListOf<Float>()
    
    private var uptimeJob: Job? = null
    
    fun start() {
        updateJob?.cancel()
        uptimeJob?.cancel()
        
        Log.d(TAG, "start() scope.isActive=${scope.isActive}")
        updateAllSensors()
        updateUptime()
        Log.d(TAG, "initial: battery=${_batteryLevel.value}% voltage=${_batteryVoltage.value}V status=${_chargingStatus.value}")
        
        updateJob = scope.launch {
            Log.d(TAG, "updateJob launched")
            while (isActive) {
                delay(UPDATE_INTERVAL_MS)
                Log.d(TAG, "updateJob tick")
                updateAllSensors()
            }
        }
        
        uptimeJob = scope.launch {
            Log.d(TAG, "uptimeJob launched")
            while (isActive) {
                delay(UPTIME_INTERVAL_MS)
                updateUptime()
            }
        }
    }
    
    fun stop() {
        updateJob?.cancel()
        uptimeJob?.cancel()
        updateJob = null
        uptimeJob = null
    }
    
    private fun updateAllSensors() {
        updateWifiSignal()
        updateDeviceIp()
        updateStorageFree()
        updateMemoryUsage()
        updateBattery()
    }
    
    private fun updateBattery() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return
        
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level >= 0) {
            _batteryLevel.value = level
        }
        
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let {
            val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            if (voltage > 0) {
                _batteryVoltage.value = voltage / 1000f
            }
            
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            _chargingStatus.value = when {
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL -> {
                    when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                        else -> "Charging"
                    }
                }
                else -> "None"
            }
        }
    }
    
    private fun updateWifiSignal() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            var rssi = -100
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                rssi = capabilities.signalStrength
                if (rssi == Int.MIN_VALUE) rssi = -100
            }
            
            if (rssi == -100) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                rssi = wifiInfo?.rssi ?: -100
            }
            
            wifiSamples.add(rssi)
            if (wifiSamples.size > SMOOTHING_SAMPLES) {
                wifiSamples.removeAt(0)
            }
            _wifiSignal.value = wifiSamples.average().toInt()
        } catch (e: Exception) {
            _wifiSignal.value = -100
        }
    }
    
    private fun updateDeviceIp() {
        try {
            val ip = getLocalIpAddress()
            _deviceIp.value = ip ?: "Unknown"
        } catch (e: Exception) {
            _deviceIp.value = "Unknown"
        }
    }
    
    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) {
                
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            
        }
        return null
    }
    
    private fun updateStorageFree() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val availableGb = availableBytes / (1024f * 1024f * 1024f)
            _storageFree.value = (availableGb * 10).toInt() / 10f
        } catch (e: Exception) {
            _storageFree.value = 0f
        }
    }
    
    private fun updateMemoryUsage() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val usedMemory = memInfo.totalMem - memInfo.availMem
            val usedGb = usedMemory / (1024f * 1024f * 1024f)
            
            memorySamples.add(usedGb)
            if (memorySamples.size > SMOOTHING_SAMPLES) {
                memorySamples.removeAt(0)
            }
            _memoryUsage.value = (memorySamples.average() * 10).toInt() / 10f
        } catch (e: Exception) {
            _memoryUsage.value = 0f
        }
    }
    
    private fun updateUptime() {
        val uptimeMs = SystemClock.elapsedRealtime()
        val totalSeconds = uptimeMs / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        _uptime.value = String.format("%d:%02d:%02d", days, hours, minutes)
    }
    
}
