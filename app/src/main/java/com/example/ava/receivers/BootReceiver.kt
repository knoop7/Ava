package com.example.ava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.settings.playerSettingsStore
import com.example.ava.utils.RootHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
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
        
        try {
            
            val settings = runBlocking {
                context.playerSettingsStore.data.first()
            }
            
            if (settings.enableAutoRestart) {
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        
                        val packageName = context.packageName
                        val serviceName = "com.example.ava.services.VoiceSatelliteService"
                        
                        if (RootHelper.startServiceWithRoot(packageName, serviceName)) {
                            
                        } else {
                            
                            val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }, 2000)
            } else {
                pendingResult.finish()
            }
        } catch (e: Exception) {
            pendingResult.finish()
        }
    }
}
