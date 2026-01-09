package com.example.ava.services

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.ava.sensor.EnvironmentSensorManager
import com.example.ava.settings.ScreensaverSettings
import com.example.ava.settings.ScreensaverSettingsStore
import com.example.ava.settings.screensaverSettingsStore
import com.example.ava.utils.RootUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

object ScreensaverController {
    private const val TAG = "ScreensaverController"
    private const val DARK_THRESHOLD_LUX = 2.0f
    private const val IDLE_CHECK_INTERVAL_MS = 1000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var appContext: Context? = null
    private var settingsStore: ScreensaverSettingsStore? = null
    private var currentSettings: ScreensaverSettings = ScreensaverSettings()

    private var lastInteractionAt = System.currentTimeMillis()
    private var idleJob: Job? = null
    private var lightJob: Job? = null
    private var sensorManager: EnvironmentSensorManager? = null
    private var motionJob: Job? = null

    private var isScreensaverVisible = false
    private var isAppInForeground = true
    private var isScreenOffByDark = false
    private var wasEnabled = false
    private var activityCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var startedCount = 0
    private var wakeLock: PowerManager.WakeLock? = null

    fun start(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        settingsStore = ScreensaverSettingsStore(context.screensaverSettingsStore)
        registerActivityCallbacks(context.applicationContext)

        scope.launch {
            settingsStore?.getFlow()?.distinctUntilChanged()?.collectLatest { settings ->
                currentSettings = settings
                if (!settings.enabled) {
                    stopScreensaver()
                    stopSensors()
                    wasEnabled = false
                    return@collectLatest
                }
                if (!wasEnabled && settings.enabled) {
                    lastInteractionAt = System.currentTimeMillis()
                    wasEnabled = true
                }
                ensureIdleJob()
                updateSensors()
                val effectiveUrl = getEffectiveScreensaverUrl(settings)
                if (effectiveUrl.isBlank()) {
                    stopScreensaver()
                }
                if (isScreensaverVisible && effectiveUrl.isNotBlank()) {
                    ScreensaverWebViewService.updateUrl(requireContext(), effectiveUrl)
                }
            }
        }
    }

    private fun registerActivityCallbacks(context: Context) {
        val application = context.applicationContext as? Application ?: return
        if (activityCallbacks != null) return
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: android.app.Activity) {
                startedCount += 1
                isAppInForeground = startedCount > 0
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                startedCount = (startedCount - 1).coerceAtLeast(0)
                isAppInForeground = startedCount > 0
            }

            override fun onActivityCreated(
                activity: android.app.Activity,
                savedInstanceState: android.os.Bundle?
            ) = Unit

            override fun onActivityResumed(activity: android.app.Activity) = Unit

            override fun onActivityPaused(activity: android.app.Activity) = Unit

            override fun onActivitySaveInstanceState(
                activity: android.app.Activity,
                outState: android.os.Bundle
            ) = Unit

            override fun onActivityDestroyed(activity: android.app.Activity) = Unit
        }
        application.registerActivityLifecycleCallbacks(callbacks)
        activityCallbacks = callbacks
    }

    fun onUserInteraction() {
        lastInteractionAt = System.currentTimeMillis()
        if (appContext == null) return
        if (isScreensaverVisible) {
            stopScreensaver()
        }
        
    }

    private fun ensureIdleJob() {
        if (idleJob != null) return
        idleJob = scope.launch {
            while (isActive) {
                delay(IDLE_CHECK_INTERVAL_MS)
                if (currentSettings.enabled) {
                    checkIdleAndShow()
                }
            }
        }
    }

    private fun checkIdleAndShow() {
        val context = appContext ?: return
        val effectiveUrl = getEffectiveScreensaverUrl(currentSettings)
        if (effectiveUrl.isBlank()) return
        if (isScreensaverVisible) return
        val elapsed = System.currentTimeMillis() - lastInteractionAt
        if (elapsed < currentSettings.timeoutSeconds * 1000L) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) return
        ScreensaverWebViewService.show(context, effectiveUrl)
        isScreensaverVisible = true
        Log.d(TAG, "Screensaver shown")
    }
    
    private fun getEffectiveScreensaverUrl(settings: ScreensaverSettings): String {
        return if (settings.xiaomiWallpaperEnabled) {
            "file:///android_asset/xiaomi_wallpaper.html"
        } else {
            settings.screensaverUrl
        }
    }

    private fun stopScreensaver() {
        if (!isScreensaverVisible) return
        ScreensaverWebViewService.hide(requireContext())
        isScreensaverVisible = false
        Log.d(TAG, "Screensaver hidden")
    }

    private fun updateSensors() {
        updateLightSensor()
        updateProximityMotion()
    }

    private fun updateLightSensor() {
        if (!currentSettings.darkOffEnabled) {
            stopLightSensor()
            return
        }
        if (sensorManager == null) {
            sensorManager = EnvironmentSensorManager(requireContext())
            sensorManager?.startListening()
        }
        if (sensorManager?.hasLightSensor != true) {
            stopLightSensor()
            return
        }
        if (lightJob == null) {
            lightJob = scope.launch {
                sensorManager?.lightLevel?.collectLatest { lux ->
                    handleLightLevel(lux)
                }
            }
        }
    }

    private fun stopLightSensor() {
        lightJob?.cancel()
        lightJob = null
        if (motionJob == null) {
            sensorManager?.stopListening()
            sensorManager = null
        }
    }

    private fun updateProximityMotion() {
        if (!currentSettings.motionOnEnabled) {
            stopProximityMotion()
            return
        }
        if (sensorManager == null) {
            sensorManager = EnvironmentSensorManager(requireContext())
            sensorManager?.startListening()
        }
        if (sensorManager?.hasProximitySensor != true) {
            stopProximityMotion()
            return
        }
        if (motionJob != null) return
        motionJob = scope.launch {
            sensorManager?.proximity?.collectLatest { distance ->
                handleMotion(distance)
            }
        }
    }

    private fun stopProximityMotion() {
        motionJob?.cancel()
        motionJob = null
        if (lightJob == null) {
            sensorManager?.stopListening()
            sensorManager = null
        }
    }

    private fun stopSensors() {
        stopLightSensor()
        stopProximityMotion()
    }

    private fun handleLightLevel(lux: Float) {
        if (!currentSettings.darkOffEnabled) return
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        val isInteractive = powerManager.isInteractive
        
        if (lux < DARK_THRESHOLD_LUX && isInteractive && !isScreenOffByDark) {
            Log.d(TAG, "Dark detected (lux=$lux), turning off screen")
            
            if (isScreensaverVisible) {
                ScreensaverWebViewService.pause(requireContext())
            }
            acquireWakeLock()
            RootUtils.executeDisplayToggle(requireContext(), false)
            isScreenOffByDark = true
        } else if (lux >= DARK_THRESHOLD_LUX && isScreenOffByDark) {
            Log.d(TAG, "Light restored (lux=$lux), turning on screen")
            RootUtils.executeDisplayToggle(requireContext(), true)
            
            if (isScreensaverVisible) {
                ScreensaverWebViewService.resume(requireContext())
            }
            releaseWakeLock()
            isScreenOffByDark = false
            
            val effectiveUrl = getEffectiveScreensaverUrl(currentSettings)
            if (effectiveUrl.isNotBlank() && !isScreensaverVisible) {
                ScreensaverWebViewService.show(requireContext(), effectiveUrl)
                isScreensaverVisible = true
                Log.d(TAG, "Screensaver restored after dark wake")
            }
        }
    }

    private fun handleMotion(distance: Float) {
        if (!currentSettings.motionOnEnabled) return
        if (currentSettings.backgroundPauseEnabled && !isAppInForeground) return
        val sensorMax = sensorManager?.proximityMaxRange ?: return
        val isNear = distance < sensorMax
        if (isNear) {
            RootUtils.executeDisplayToggle(requireContext(), true)
            onUserInteraction()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Ava:DarkSensorWakeLock"
        ).apply {
            acquire() 
        }
        Log.d(TAG, "WakeLock acquired for dark sensor")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun requireContext(): Context = checkNotNull(appContext) { "ScreensaverController not started" }
}
