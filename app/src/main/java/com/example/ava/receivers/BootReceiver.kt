package com.example.ava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.settings.playerSettingsStore
import com.example.ava.utils.KeepAliveHelper
import com.example.ava.utils.RootHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.samsung.android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "com.huawei.intent.action.QUICKBOOT_POWERON",
            "com.vivo.intent.action.QUICKBOOT_POWERON",
            "com.oppo.intent.action.QUICKBOOT_POWERON" -> {
                handleBootComplete(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                
                if (intent.data?.schemeSpecificPart == context.packageName) {
                    handleBootComplete(context)
                }
            }
        }
    }
    
    private fun handleBootComplete(context: Context) {
        val pendingResult = goAsync()
        
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val settings = context.playerSettingsStore.data.first()
                
                if (settings.enableAutoRestart) {
                    val delayMs = getBootDelayForManufacturer()
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            startServiceWithRetry(context, 3)
                        } finally {
                            pendingResult.finish()
                        }
                    }, delayMs)
                } else {
                    pendingResult.finish()
                }
            } catch (e: Exception) {
                pendingResult.finish()
            } finally {
                scope.cancel()
            }
        }
    }
    
    private fun getBootDelayForManufacturer(): Long {
        return when (KeepAliveHelper.getManufacturer()) {
            KeepAliveHelper.Manufacturer.HUAWEI,
            KeepAliveHelper.Manufacturer.HONOR -> 8000L
            KeepAliveHelper.Manufacturer.VIVO,
            KeepAliveHelper.Manufacturer.IQOO -> 6000L
            KeepAliveHelper.Manufacturer.OPPO,
            KeepAliveHelper.Manufacturer.REALME -> 5000L
            KeepAliveHelper.Manufacturer.XIAOMI -> 4000L
            else -> 3000L
        }
    }
    
    private fun startServiceWithRetry(context: Context, maxRetries: Int) {
        var retryCount = 0
        
        fun tryStart() {
            val packageName = context.packageName
            val serviceName = "com.example.ava.services.VoiceSatelliteService"
            
            val rootSuccess = RootHelper.startServiceWithRoot(packageName, serviceName)
            
            if (!rootSuccess) {
                try {
                    val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            tryStart()
                        }, 2000L * retryCount)
                    }
                }
            }
        }
        
        tryStart()
    }
}
