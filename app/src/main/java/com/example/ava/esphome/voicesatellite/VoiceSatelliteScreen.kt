package com.example.ava.esphome.voicesatellite

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.ava.R
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.BinarySensorEntity
import com.example.ava.esphome.entities.NumberEntity
import com.example.ava.settings.ExperimentalSettingsStore
import com.example.esphomeproto.api.EntityCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VoiceSatelliteScreen(
    private val context: Context,
    private val scope: CoroutineScope,
    private val device: EspHomeDevice,
    private val experimentalSettingsStore: ExperimentalSettingsStore
) {
    private val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    private val _screenBrightness = MutableStateFlow(0f)
    private val _proximityState = MutableStateFlow(false)
    
    private var environmentSensorManager: com.example.ava.sensor.EnvironmentSensorManager? = null
    private var proximitySensorEntity: BinarySensorEntity? = null
    private var proximityJob: Job? = null
    private var brightnessObserver: android.database.ContentObserver? = null
    private var orientationOverlayView: android.view.View? = null
    private var lastWakeTime = 0L

    companion object {
        private const val TAG = "VoiceSatelliteScreen"
    }

    fun initProximitySensor() {
        val sensorManager = environmentSensorManager ?: run {
            environmentSensorManager = com.example.ava.sensor.EnvironmentSensorManager(context)
            environmentSensorManager!!.startListening()
            environmentSensorManager!!
        }
        
        var awayDelayJob: Job? = null
        var wasNear = false
        
        proximityJob?.cancel()
        proximityJob = scope.launch {
            val settings = experimentalSettingsStore.get()
            
            if (settings.proximitySendToHass && sensorManager.hasProximitySensor && proximitySensorEntity == null) {
                proximitySensorEntity = BinarySensorEntity(
                    key = 22,
                    name = context.getString(R.string.entity_proximity_sensor),
                    objectId = "proximity_sensor",
                    deviceClass = "occupancy",
                    icon = "mdi:motion-sensor",
                    getState = _proximityState
                )
                proximitySensorEntity?.let { device.addEntity(it) }
            }
            sensorManager.proximity.collect { distance ->
                val currentSettings = experimentalSettingsStore.get()
                
                val isNear = distance < sensorManager.proximityMaxRange
                
                if (isNear) {
                    awayDelayJob?.cancel()
                    awayDelayJob = null
                    _proximityState.value = true
                    
                    if (currentSettings.proximityWakeScreen && !powerManager.isInteractive) {
                        val now = System.currentTimeMillis()
                        if (now - lastWakeTime > 3000) { 
                            wakeScreen(currentSettings.proximityAutoUnlock)
                            lastWakeTime = now
                        }
                    }
                } else if (wasNear && awayDelayJob == null) {
                    val delayMs = currentSettings.proximityAwayDelay * 1000L
                    awayDelayJob = scope.launch {
                        delay(delayMs)
                        _proximityState.value = false
                        awayDelayJob = null
                    }
                }
                wasNear = isNear
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun wakeScreen(unlock: Boolean) {
        if (powerManager.isInteractive) return
        
        val flags = android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
            (if (unlock) android.os.PowerManager.ON_AFTER_RELEASE else 0)
        
        val wakeLock = powerManager.newWakeLock(flags, "Ava:ProximityWake")
        wakeLock.acquire(3000L)
        wakeLock.release()
        
        if (unlock) {
            try {
                val intent = android.content.Intent(context, com.example.ava.UnlockActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("unlock", true)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start unlock activity: ${e.message}")
            }
        }
    }
    
    fun stopProximitySensor() {
        proximityJob?.cancel()
        proximityJob = null
        proximitySensorEntity = null
    }
    
    @Suppress("DEPRECATION")
    fun initScreenBrightness() {
        val currentBrightness = try {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            ).toFloat()
        } catch (e: Exception) {
            128f
        }
        _screenBrightness.value = currentBrightness
        
        val brightnessEntity = NumberEntity(
            key = "screen_brightness".hashCode(),
            name = context.getString(R.string.entity_screen_brightness),
            objectId = "screen_brightness",
            icon = "mdi:brightness-6",
            minValue = 1f,
            maxValue = 255f,
            step = 1f,
            getState = _screenBrightness,
            setState = { value ->
                setScreenBrightness(value.toInt())
                _screenBrightness.value = value
            },
            entityCategory = EntityCategory.ENTITY_CATEGORY_NONE,
            mode = com.example.esphomeproto.api.NumberMode.NUMBER_MODE_SLIDER
        )
        device.addEntity(brightnessEntity)
        
        startBrightnessObserver()
    }
    
    @Suppress("DEPRECATION")
    private fun startBrightnessObserver() {
        brightnessObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                try {
                    val brightness = android.provider.Settings.System.getInt(
                        context.contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                    ).toFloat()
                    _screenBrightness.value = brightness
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read brightness: ${e.message}")
                }
            }
        }
        context.contentResolver.registerContentObserver(
            android.provider.Settings.System.getUriFor(android.provider.Settings.System.SCREEN_BRIGHTNESS),
            false,
            brightnessObserver!!
        )
    }
    
    @Suppress("DEPRECATION")
    private fun setScreenBrightness(brightness: Int) {
        val value = brightness.coerceIn(0, 255)
        try {
            if (!android.provider.Settings.System.canWrite(context)) {
                Log.w(TAG, "No WRITE_SETTINGS permission")
                return
            }
            val resolver = context.contentResolver
            android.provider.Settings.System.putInt(
                resolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set screen brightness", e)
        }
    }
    
    @SuppressLint("InflateParams")
    fun initForceOrientation() {
        scope.launch {
            try {
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    Log.w(TAG, "No overlay permission for force orientation")
                    experimentalSettingsStore.setForceOrientationEnabled(false)
                    return@launch
                }
                
                val settings = experimentalSettingsStore.get()
                val orientation = when (settings.forceOrientationMode) {
                    "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                
                if (orientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    Log.i(TAG, "Force orientation: auto mode, skipping")
                    return@launch
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    removeOrientationOverlay()
                    
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                    val params = android.view.WindowManager.LayoutParams(
                        0, 0,
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            @Suppress("DEPRECATION")
                            android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        android.graphics.PixelFormat.TRANSLUCENT
                    )
                    params.screenOrientation = orientation
                    
                    val view = android.view.View(context)
                    windowManager.addView(view, params)
                    orientationOverlayView = view
                    Log.i(TAG, "Force orientation enabled: ${settings.forceOrientationMode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init force orientation", e)
            }
        }
    }
    
    fun stopBrightnessObserver() {
        brightnessObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
        }
        brightnessObserver = null
    }
    
    fun removeOrientationOverlay() {
        val view = orientationOverlayView ?: return
        orientationOverlayView = null
        val doRemove = Runnable {
            try {
                if (view.isAttachedToWindow) {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                    wm.removeView(view)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove orientation overlay: ${e.message}")
            }
        }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            doRemove.run()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post(doRemove)
        }
    }
    
    fun close() {
        stopProximitySensor()
        stopBrightnessObserver()
        removeOrientationOverlay()
        environmentSensorManager?.stopListening()
        environmentSensorManager = null
    }
}
