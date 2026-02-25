package com.example.ava.wakelocks

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

class WifiWakeLock {
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock

    fun create(context: Context, tag: String) {
        
        if (::wakeLock.isInitialized && ::wifiLock.isInitialized) {
            Log.d(TAG, "WifiWakeLock already initialized, skipping")
            return
        }

        
        wakeLock = (context.getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag::Wakelock")

        
        
        
        
        
        @Suppress("DEPRECATION")
        val wifiLockType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        else WifiManager.WIFI_MODE_FULL_HIGH_PERF

        wifiLock = (context.getSystemService(WIFI_SERVICE) as WifiManager).createWifiLock(
            wifiLockType,
            "$tag::WifiLock"
        )
    }

    private val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L
    
    fun acquire() {
        check(::wakeLock.isInitialized && ::wifiLock.isInitialized) {
            "acquire called before create"
        }
        wakeLock.acquire(WAKELOCK_TIMEOUT_MS)
        wifiLock.acquire()
        Log.d(TAG, "Acquired wake locks with ${WAKELOCK_TIMEOUT_MS}ms timeout")
    }
    
    fun renewIfNeeded() {
        if (!::wakeLock.isInitialized || !::wifiLock.isInitialized) return
        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS)
            if (!wifiLock.isHeld) {
                wifiLock.acquire()
            }
            Log.d(TAG, "Renewed wake locks")
        }
    }

    fun release() {
        if (!::wakeLock.isInitialized || !::wifiLock.isInitialized) {
             return
        }
        if (wakeLock.isHeld)
            wakeLock.release()
        if (wifiLock.isHeld)
            wifiLock.release()
        Log.d(TAG, "Released wake locks")
    }

    companion object {
        private const val TAG = "WifiWakeLock"
    }
}