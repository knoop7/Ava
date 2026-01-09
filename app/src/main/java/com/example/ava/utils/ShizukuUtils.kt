package com.example.ava.utils

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.example.ava.IShellService
import com.example.ava.shizuku.ShellService
import rikka.shizuku.Shizuku

private const val TAG = "ShizukuUtils"
private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

object ShizukuUtils {
    
    private var shellService: IShellService? = null
    private var serviceConnected = false
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.example.ava", ShellService::class.java.name)
    ).daemon(false).processNameSuffix("shell").version(1)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shellService = IShellService.Stub.asInterface(service)
            serviceConnected = true
            Log.d(TAG, "ShellService connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            serviceConnected = false
            Log.d(TAG, "ShellService disconnected")
        }
    }
    
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            bindShellService()
        }
        Log.d(TAG, "Permission result: granted=${grantResult == PackageManager.PERMISSION_GRANTED}")
    }
    
    private var pendingPackageName: String? = null
    
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (isShizukuPermissionGranted()) {
            bindShellService()
            pendingPackageName?.let { pkg ->
                Thread { grantBluetoothPermissions(pkg) }.start()
            }
        }
    }
    
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        serviceConnected = false
        shellService = null
    }
    
    fun init(packageName: String? = null) {
        pendingPackageName = packageName
        try {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            
            if (isShizukuPermissionGranted()) {
                bindShellService()
                packageName?.let { pkg ->
                    Thread { grantBluetoothPermissions(pkg) }.start()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to init Shizuku listeners", e)
        }
    }
    
    fun cleanup() {
        try {
            unbindShellService()
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup Shizuku listeners", e)
        }
    }
    
    private fun bindShellService() {
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind ShellService", e)
        }
    }
    
    private fun unbindShellService() {
        try {
            if (serviceConnected) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unbind ShellService", e)
        }
    }
    
    @Suppress("DEPRECATION")
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getInstalledApplications(0).any { 
                it.packageName.contains("shizuku", ignoreCase = true) 
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    fun isShizukuPermissionGranted(): Boolean {
        return try {
            isShizukuRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    fun requestPermission(requestCode: Int) {
        try {
            if (isShizukuRunning() && !isShizukuPermissionGranted()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request Shizuku permission", e)
        }
    }
    
    fun executeCommand(command: String): Pair<Int, String> {
        if (!isShizukuPermissionGranted()) {
            return Pair(-1, "Shizuku permission not granted")
        }
        
        var service = shellService
        if (service == null) {
            bindShellService()
            
            repeat(10) {
                Thread.sleep(100)
                service = shellService
                if (service != null) return@repeat
            }
        }
        
        service = shellService
        if (service == null) {
            Log.w(TAG, "ShellService not connected after waiting")
            return Pair(-1, "ShellService not connected")
        }
        
        return try {
            val exitCode = service.executeCommand(command)
            Pair(exitCode, "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            Pair(-1, e.message ?: "Unknown error")
        }
    }
    
    fun grantLocationPermission(packageName: String): Boolean {
        if (!isShizukuPermissionGranted()) return false
        
        val (code1, _) = executeCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
        val (code2, _) = executeCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        executeCommand("settings put secure location_mode 3")
        
        return code1 == 0 && code2 == 0
    }
    
    fun grantBluetoothPermissions(packageName: String): Boolean {
        if (!isShizukuPermissionGranted()) {
            Log.w(TAG, "grantBluetoothPermissions: Shizuku permission not granted")
            return false
        }
        
        
        val r1 = executeCommand("pm grant $packageName android.permission.BLUETOOTH_SCAN")
        val r2 = executeCommand("pm grant $packageName android.permission.BLUETOOTH_CONNECT")
        val r3 = executeCommand("pm grant $packageName android.permission.BLUETOOTH_ADVERTISE")
        
        
        val r4 = executeCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
        val r5 = executeCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        
        
        executeCommand("settings put secure location_mode 3")
        
        Log.d(TAG, "grantBluetoothPermissions: SCAN=${r1.first}, CONNECT=${r2.first}, ADVERTISE=${r3.first}, COARSE=${r4.first}, FINE=${r5.first}")
        
        return r5.first == 0
    }
    
    fun executeDisplayToggle(dexPath: String, mode: Int): Boolean {
        if (!isShizukuPermissionGranted()) return false
        
        val cmd = "CLASSPATH=$dexPath app_process / DisplayToggle $mode"
        val (exitCode, _) = executeCommand(cmd)
        return exitCode == 0
    }
    
    fun setDisplayPower(mode: Int): Boolean {
        if (!isShizukuPermissionGranted()) return false
        
        var service = shellService
        if (service == null) {
            bindShellService()
            repeat(10) {
                Thread.sleep(100)
                service = shellService
                if (service != null) return@repeat
            }
        }
        
        service = shellService ?: return false
        
        return try {
            service.setDisplayPower(mode)
        } catch (e: Exception) {
            false
        }
    }
    
    fun rebootDevice(): Boolean {
        if (!isShizukuPermissionGranted()) return false
        
        val (exitCode, _) = executeCommand("reboot")
        return exitCode == 0
    }
}
