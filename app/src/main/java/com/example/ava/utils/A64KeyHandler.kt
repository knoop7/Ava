package com.example.ava.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.example.ava.services.ScreensaverController
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.services.VolumeControlService
import com.example.ava.services.DreamClockService
import com.example.ava.services.WeatherOverlayService
import com.example.ava.services.VinylCoverService


object A64KeyHandler {
    private const val TAG = "A64KeyHandler"
    private const val LONG_PRESS_THRESHOLD = 2000L
    private const val DOUBLE_CLICK_THRESHOLD = 300L
    private const val MIN_BRIGHTNESS = 10  
    private const val MAX_BRIGHTNESS = 255 
    
    private val handler = Handler(Looper.getMainLooper())
    private var mainButtonDownTime: Long = 0
    private var longPressRunnable: Runnable? = null
    
    
    private var menuLastClickTime: Long = 0
    private var menuClickCount: Int = 0
    private var menuButtonDownTime: Long = 0
    private var menuLongPressRunnable: Runnable? = null
    
    
    private var isScreenDimmed = false
    private var savedBrightness = MAX_BRIGHTNESS
    
    
    fun isA64Device(): Boolean {
        return DeviceCapabilities.isA64Device()
    }
    
    
    fun onKeyDown(context: Context, keyCode: Int, event: KeyEvent?): Boolean {
        
        ScreensaverController.onUserInteraction()
        

        if (keyCode == KeyEvent.KEYCODE_F12) {
            Log.d(TAG, "F12 pressed, toggling mic mute")
            VoiceSatelliteService.toggleMicMute()
            return true
        }
        
        if (!isA64Device()) {
            return false
        }
        
        Log.d(TAG, "onKeyDown: keyCode=$keyCode")
        
        return when (keyCode) {
            
            KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.d(TAG, "VOLUME_UP pressed")
                VolumeControlService.volumeUp(context)
                true
            }
            
            
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                Log.d(TAG, "VOLUME_DOWN pressed")
                VolumeControlService.volumeDown(context)
                true
            }
            
            
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                Log.d(TAG, "VOLUME_MUTE (main button) pressed")
                mainButtonDownTime = System.currentTimeMillis()
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = Runnable {
                    Log.d(TAG, "Long press detected, toggling brightness")
                    toggleBrightness(context)
                }
                handler.postDelayed(longPressRunnable!!, LONG_PRESS_THRESHOLD)
                true
            }
            
            
            
            KeyEvent.KEYCODE_MENU -> {
                Log.d(TAG, "MENU (physical button key 139) pressed")
                menuButtonDownTime = System.currentTimeMillis()
                menuLongPressRunnable?.let { handler.removeCallbacks(it) }
                menuLongPressRunnable = Runnable {
                    Log.d(TAG, "MENU long press detected, toggling brightness")
                    menuClickCount = 0 
                    toggleBrightness(context)
                }
                handler.postDelayed(menuLongPressRunnable!!, LONG_PRESS_THRESHOLD)
                true
            }
            
            
            KeyEvent.KEYCODE_BACK -> {
                Log.d(TAG, "BACK pressed, hiding overlays")
                DreamClockService.hide(context)
                WeatherOverlayService.hide(context)
                VinylCoverService.hide(context)
                false 
            }
            
            
            KeyEvent.KEYCODE_HOME -> {
                Log.d(TAG, "HOME pressed, hiding overlays")
                DreamClockService.hide(context)
                WeatherOverlayService.hide(context)
                VinylCoverService.hide(context)
                false
            }
            
            else -> false
        }
    }
    
    
    fun onKeyUp(context: Context, keyCode: Int, event: KeyEvent?): Boolean {
        if (!isA64Device()) {
            return false
        }
        
        Log.d(TAG, "onKeyUp: keyCode=$keyCode")
        
        return when (keyCode) {
            
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                Log.d(TAG, "VOLUME_MUTE (main button) released")
                longPressRunnable?.let { handler.removeCallbacks(it) }
                longPressRunnable = null
                
                val pressDuration = System.currentTimeMillis() - mainButtonDownTime
                Log.d(TAG, "Press duration: ${pressDuration}ms")
                
                if (pressDuration < LONG_PRESS_THRESHOLD) {
                    Log.d(TAG, "Short press detected, triggering voice wake")
                    triggerVoiceWake()
                }
                mainButtonDownTime = 0
                true
            }
            
            
            
            KeyEvent.KEYCODE_MENU -> {
                Log.d(TAG, "MENU (physical button key 139) released")
                menuLongPressRunnable?.let { handler.removeCallbacks(it) }
                menuLongPressRunnable = null
                
                val pressDuration = System.currentTimeMillis() - menuButtonDownTime
                Log.d(TAG, "MENU press duration: ${pressDuration}ms")
                
                if (pressDuration < LONG_PRESS_THRESHOLD) {
                    
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - menuLastClickTime < DOUBLE_CLICK_THRESHOLD) {
                        
                        Log.d(TAG, "MENU double click detected, hiding overlays")
                        menuClickCount = 0
                        DreamClockService.hide(context)
                        WeatherOverlayService.hide(context)
                        VinylCoverService.hide(context)
                    } else {
                        
                        Log.d(TAG, "MENU short press detected, triggering voice wake")
                        triggerVoiceWake()
                    }
                    menuLastClickTime = currentTime
                }
                menuButtonDownTime = 0
                true
            }
            
            else -> false
        }
    }
    
    
    fun cleanup() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
        menuLongPressRunnable?.let { handler.removeCallbacks(it) }
        menuLongPressRunnable = null
        menuClickCount = 0
        menuLastClickTime = 0
        mainButtonDownTime = 0
    }
    
    
    private fun triggerVoiceWake() {
        val service = VoiceSatelliteService.getInstance()
        if (service == null) {
            Log.w(TAG, "triggerVoiceWake: VoiceSatelliteService not available")
            return
        }
        
        val state = service.getState()
        Log.d(TAG, "triggerVoiceWake: currentState=$state")
        
        
        service.triggerManualWake()
    }
    
    
    private fun toggleBrightness(context: Context) {
        try {
            val contentResolver = context.contentResolver
            
            if (isScreenDimmed) {
                
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    savedBrightness
                )
                isScreenDimmed = false
                Log.d(TAG, "Brightness restored to: $savedBrightness")
            } else {
                
                savedBrightness = android.provider.Settings.System.getInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    MAX_BRIGHTNESS
                )
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    MIN_BRIGHTNESS
                )
                isScreenDimmed = true
                Log.d(TAG, "Brightness dimmed from $savedBrightness to $MIN_BRIGHTNESS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle brightness", e)
        }
    }
}
