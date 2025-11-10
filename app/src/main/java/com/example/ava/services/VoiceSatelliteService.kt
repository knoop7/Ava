package com.example.ava.services

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.ava.esphome.VoiceSatellite
import com.example.ava.microwakeword.AssetWakeWordProvider
import com.example.ava.notifications.createVoiceSatelliteServiceNotification
import com.example.ava.nsd.NsdRegistration
import com.example.ava.nsd.registerVoiceSatelliteNsd
import com.example.ava.players.TtsPlayer
import com.example.ava.preferences.VoiceAssistantPreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class VoiceSatelliteService() : LifecycleService() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var settingsStore: VoiceAssistantPreferencesStore
    private var voiceSatelliteNsd = AtomicReference<NsdRegistration?>(null)
    private val _voiceSatelliteStateFlow = MutableStateFlow<VoiceSatellite?>(null)
    val voiceSatelliteStateFlow = _voiceSatelliteStateFlow.asStateFlow()

    fun startVoiceSatellite() {
        val serviceIntent = Intent(this, this::class.java)
        applicationContext.startForegroundService(serviceIntent)
    }

    fun stopVoiceSatellite() {
        val satellite = _voiceSatelliteStateFlow.getAndUpdate { null }
        if (satellite != null) {
            Log.d(TAG, "Stopping voice satellite")
            wakeLock.release()
            satellite.close()
            voiceSatelliteNsd.getAndSet(null)?.unregister(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::Wakelock")
        settingsStore = VoiceAssistantPreferencesStore(applicationContext)
    }

    class VoiceSatelliteBinder(val service: VoiceSatelliteService) : Binder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return VoiceSatelliteBinder(this)
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            // already started?
            if (_voiceSatelliteStateFlow.value == null) {
                Log.d(TAG, "Starting voice satellite")
                startForeground(
                    2,
                    createVoiceSatelliteServiceNotification(this@VoiceSatelliteService)
                )
                @SuppressLint("WakelockTimeout")
                wakeLock.acquire()
                val settings = settingsStore.getSettings()
                val wakeWordProvider = AssetWakeWordProvider(assets)
                val stopWordProvider = AssetWakeWordProvider(assets, "stopWords")
                val ttsPlayer = TtsPlayer(this@VoiceSatelliteService)
                val satellite = VoiceSatellite(
                    lifecycleScope.coroutineContext,
                    settings,
                    wakeWordProvider,
                    stopWordProvider,
                    ttsPlayer
                )
                _voiceSatelliteStateFlow.value = satellite
                satellite.start()
                voiceSatelliteNsd.set(
                    registerVoiceSatelliteNsd(
                        context = this@VoiceSatelliteService,
                        name = settings.name,
                        port = settings.serverPort,
                        macAddress = settings.macAddress
                    )
                )
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        _voiceSatelliteStateFlow.getAndUpdate { null }?.close()
        voiceSatelliteNsd.getAndSet(null)?.unregister(this)
        if (wakeLock.isHeld)
            wakeLock.release()
        super.onDestroy()
    }

    companion object {
        const val TAG = "VoiceSatelliteService"
    }
}