package com.example.ava.utils

import android.content.Context
import java.io.File

object RootUtils {
    private var rootAvailable: Boolean? = null
    private var cachedBacklightBrightness: Int = -1
    private var detectedBacklightPath: String? = null
    @Volatile private var targetScreenState: Boolean? = null
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()

    fun isRootAvailable(): Boolean = rootAvailable ?: runCatching {
        Runtime.getRuntime().exec("su -c ls").waitFor() == 0
    }.getOrDefault(false).also { rootAvailable = it }

    fun requestRootPermission() = takeIf { isRootAvailable() }?.let {
        runCatching { Runtime.getRuntime().exec("su").waitFor() }
    }
    
    fun setProcessHighPriority(pid: Int) = takeIf { isRootAvailable() }?.runCatching {
        Runtime.getRuntime().exec("su -c renice -20 $pid").waitFor()
        Runtime.getRuntime().exec("su -c echo -1000 > /proc/$pid/oom_score_adj").waitFor()
    }

    fun acquireCpuWakeLock() = takeIf { isRootAvailable() }?.runCatching {
        Runtime.getRuntime().exec("su -c echo ava_wakelock > /sys/power/wake_lock").waitFor()
    }

    fun releaseCpuWakeLock() = takeIf { isRootAvailable() }?.runCatching {
        Runtime.getRuntime().exec("su -c echo ava_wakelock > /sys/power/wake_unlock").waitFor()
    }

    fun grantLocationPermissionForBluetooth(packageName: String): Boolean =
        grantLocationPermissionViaRoot(packageName).takeIf { it } ?: false

    private fun grantLocationPermissionViaRoot(packageName: String): Boolean = runCatching {
        val process = Runtime.getRuntime().exec("su")
        java.io.DataOutputStream(process.outputStream).use { os ->
            os.writeBytes("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION\n")
            os.writeBytes("pm grant $packageName android.permission.ACCESS_FINE_LOCATION\n")
            os.writeBytes("settings put secure location_mode 3\n")
            os.writeBytes("settings put secure location_providers_allowed +gps,network\n")
            os.writeBytes("settings put global ble_scan_always_enabled 1\n")
            os.writeBytes("exit\n")
        }
        process.waitFor() == 0
    }.getOrDefault(false)

    fun grantLocationPermission(packageName: String): Boolean = grantLocationPermissionForBluetooth(packageName)

    fun disableBleScanLocationCheck(): Boolean = runCatching {
        isRootAvailable() && listOf(
            "settings put global ble_scan_always_enabled 1",
            "settings put secure location_mode 3",
            "settings put secure location_providers_allowed +gps,network"
        ).all { Runtime.getRuntime().exec(arrayOf("su", "-c", it)).waitFor() == 0 }
    }.getOrDefault(false)

    fun isQuadCoreA64Device(): Boolean = android.os.Build.MODEL.let {
        it.contains("QUAD-CORE A64", ignoreCase = true) || it.contains("ococci", ignoreCase = true)
    }

    fun enableAllCpuCoresForOcocciDevice() = takeIf { isQuadCoreA64Device() && isRootAvailable() }?.runCatching {
        listOf(
            "echo 0 > /sys/kernel/autohotplug/enable",
            "echo 1 > /sys/devices/system/cpu/cpu1/online",
            "echo 1 > /sys/devices/system/cpu/cpu2/online",
            "echo 1 > /sys/devices/system/cpu/cpu3/online"
        ).forEach { Runtime.getRuntime().exec("su -c $it").waitFor() }
    }

    private fun findBacklightPath(): String? = detectedBacklightPath ?: runCatching {
        takeIf { isRootAvailable() }?.let {
            val cmd = "ls /sys/class/leds/*/brightness /sys/class/backlight/*/brightness 2>/dev/null | grep -E 'lcd|backlight' | head -1"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }
                .takeIf { it.isNotEmpty() && it.contains("brightness") }
                ?.also { detectedBacklightPath = it }
        }
    }.getOrNull()

    fun readBacklightBrightness(): Int = runCatching {
        findBacklightPath()?.let { path ->
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $path"))
            process.inputStream.bufferedReader().readText().trim().also { process.waitFor() }.toIntOrNull()
        }
    }.getOrNull() ?: -1

    fun writeBacklightBrightness(brightness: Int): Boolean = runCatching {
        findBacklightPath()?.let { path ->
            Runtime.getRuntime().exec(arrayOf("su", "-c", "echo $brightness > $path")).waitFor() == 0
        } ?: false
    }.getOrDefault(false)

    fun executeDisplayToggle(context: Context, screenOn: Boolean, brightnessPercent: Int = -1) {
        targetScreenState = screenOn
        executor.execute {
            takeIf { targetScreenState == screenOn && isRootAvailable() }?.runCatching {
                when (screenOn) {
                    true -> {
                        writeBacklightBrightness(cachedBacklightBrightness.takeIf { it > 0 } ?: 128)
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system screen_brightness_mode 1")).waitFor()
                    }
                    false -> {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system screen_brightness_mode 0")).waitFor()
                        readBacklightBrightness().takeIf { it > 0 }?.let { cachedBacklightBrightness = it }
                        writeBacklightBrightness(0)
                    }
                }
            }
        }
    }

    fun executeScreenToggle(context: Context, screenOn: Boolean) = Thread {
        val mode = if (screenOn) 2 else 0
        val result = tryDexToggle(context, mode) ?: tryShizukuToggle(mode) ?: tryBacklightToggle(screenOn)
        showToggleResult(context, screenOn, result)
    }.start()

    private fun tryDexToggle(context: Context, mode: Int): String? = runCatching {
        takeIf { isRootAvailable() }?.let {
            val localDexFile = java.io.File(context.filesDir, "DisplayToggle.dex")
            if (!localDexFile.exists()) {
                context.assets.open("DisplayToggle.dex").use { input ->
                    localDexFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val cmd = "CLASSPATH=${localDexFile.absolutePath} app_process / DisplayToggle $mode"
            "dex".takeIf { Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() == 0 }
        }
    }.getOrNull()

    private fun tryShizukuToggle(mode: Int): String? = runCatching {
        "shizuku".takeIf { ShizukuUtils.isShizukuPermissionGranted() && ShizukuUtils.setDisplayPower(mode) }
    }.getOrNull()

    private fun tryBacklightToggle(screenOn: Boolean): String? = runCatching {
        "backlight".takeIf { findBacklightPath() != null && writeBacklightBrightness(if (screenOn) 128 else 0) }
    }.getOrNull()

    private fun showToggleResult(context: Context, screenOn: Boolean, method: String?) {
        val msgResId = when {
            method != null && screenOn -> com.example.ava.R.string.screen_toggle_on_success
            method != null -> com.example.ava.R.string.screen_toggle_off_success
            else -> com.example.ava.R.string.screen_toggle_failed
        }
        android.os.Handler(context.mainLooper).post {
            val msg = method?.let { context.getString(msgResId, it) } ?: context.getString(msgResId)
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun rebootDevice() = Thread {
        runCatching {
            when {
                isRootAvailable() -> Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")).waitFor()
                ShizukuUtils.isShizukuPermissionGranted() -> ShizukuUtils.rebootDevice()
            }
        }
    }.start()
}
