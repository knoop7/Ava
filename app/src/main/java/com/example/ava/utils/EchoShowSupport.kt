package com.example.ava.utils

import android.content.Context
import android.os.Build

object EchoShowSupport {
    
    private var _isEchoShowDevice: Boolean? = null
    
    private val ECHO_SHOW_CODENAMES = listOf("crown", "checkers", "cronos")
    
    fun isEchoShowDevice(): Boolean {
        if (_isEchoShowDevice != null) return _isEchoShowDevice!!
        
        _isEchoShowDevice = try {
            val modelLower = (Build.MODEL ?: "").lowercase()
            val boardLower = (Build.BOARD ?: "").lowercase()
            val deviceLower = (Build.DEVICE ?: "").lowercase()
            
            ECHO_SHOW_CODENAMES.any { codename ->
                modelLower.contains(codename) ||
                boardLower.contains(codename) ||
                deviceLower.contains(codename)
            } ||
            modelLower.contains("amazon") ||
            (modelLower.contains("echo") && modelLower.contains("show"))
        } catch (e: Exception) {
            false
        }
        
        return _isEchoShowDevice!!
    }
    
    fun getMinBrightness(): Int {
        return if (isEchoShowDevice()) 10 else 0
    }
    
    fun grantOverlayPermissionIfNeeded(context: Context): Boolean {
        if (!isEchoShowDevice()) return false
        
        return runCatching {
            val process = Runtime.getRuntime().exec("su")
            java.io.DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow\n")
                os.writeBytes("exit\n")
            }
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}
