package com.example.ava.shizuku

import android.util.Log
import com.example.ava.IShellService
import kotlin.system.exitProcess

class ShellService : IShellService.Stub() {
    
    companion object {
        private const val TAG = "ShellService"
    }
    
    override fun executeCommand(command: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }
    
    @Suppress("BlockedPrivateApi")
    override fun setDisplayPower(mode: Int): Boolean {
        return try {
            
            if (mode == 2) {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent 224")).waitFor() 
            }
            
            val surfaceControlClass = Class.forName("android.view.SurfaceControl")
            
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                val classLoaderFactoryClass = Class.forName("com.android.internal.os.ClassLoaderFactory")
                val createClassLoaderMethod = classLoaderFactoryClass.getDeclaredMethod(
                    "createClassLoader",
                    String::class.java, String::class.java, String::class.java,
                    ClassLoader::class.java, Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType, String::class.java
                )
                val classLoader = createClassLoaderMethod.invoke(
                    null, "/system/framework/services.jar", null, null,
                    ClassLoader.getSystemClassLoader(), 0, true, null
                ) as ClassLoader
                val displayControlClass = classLoader.loadClass("com.android.server.display.DisplayControl")
                
                val loadLibraryMethod = Runtime::class.java.getDeclaredMethod("loadLibrary0", Class::class.java, String::class.java)
                loadLibraryMethod.isAccessible = true
                loadLibraryMethod.invoke(Runtime.getRuntime(), displayControlClass, "android_servers")
                
                val getPhysicalDisplayIdsMethod = displayControlClass.getMethod("getPhysicalDisplayIds")
                val getPhysicalDisplayTokenMethod = displayControlClass.getMethod("getPhysicalDisplayToken", Long::class.javaPrimitiveType)
                val setDisplayPowerModeMethod = surfaceControlClass.getMethod("setDisplayPowerMode", android.os.IBinder::class.java, Int::class.javaPrimitiveType)
                
                val displayIds = getPhysicalDisplayIdsMethod.invoke(null) as? LongArray
                displayIds?.forEach { displayId ->
                    val token = getPhysicalDisplayTokenMethod.invoke(null, displayId) as? android.os.IBinder
                    token?.let { setDisplayPowerModeMethod.invoke(null, it, mode) }
                }
            } else {
                val displayToken = if (android.os.Build.VERSION.SDK_INT >= 29) {
                    surfaceControlClass.getMethod("getInternalDisplayToken").invoke(null)
                } else {
                    surfaceControlClass.getMethod("getBuiltInDisplay", Int::class.javaPrimitiveType).invoke(null, 0)
                } ?: return false
                
                surfaceControlClass.getMethod("setDisplayPowerMode", android.os.IBinder::class.java, Int::class.javaPrimitiveType)
                    .invoke(null, displayToken, mode)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun destroy() {
        Log.d(TAG, "ShellService destroy called")
        exitProcess(0)
    }
}
