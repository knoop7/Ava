package com.example.ava.services

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import com.example.ava.esphome.Stopped
import com.example.ava.esphome.voicesatellite.VoiceSatellite
import com.example.ava.microwakeword.AssetWakeWordProvider
import com.example.ava.notifications.createVoiceSatelliteServiceNotification
import com.example.ava.nsd.NsdRegistration
import com.example.ava.nsd.registerVoiceSatelliteNsd
import com.example.ava.players.MediaPlayer
import com.example.ava.players.TtsPlayer
import com.example.ava.settings.VoiceSatelliteSettings
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.ava.settings.voiceSatelliteSettingsStore
import com.example.ava.wakelocks.WifiWakeLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class VoiceSatelliteService() : LifecycleService() {
    private val wifiWakeLock = WifiWakeLock()
    private lateinit var settingsStore: VoiceSatelliteSettingsStore
    private var voiceSatelliteNsd = AtomicReference<NsdRegistration?>(null)
    private val _voiceSatellite = MutableStateFlow<VoiceSatellite?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val voiceSatelliteState = _voiceSatellite.flatMapLatest {
        it?.state ?: flowOf(Stopped)
    }

    fun startVoiceSatellite() {
        val serviceIntent = Intent(this, this::class.java)
        applicationContext.startForegroundService(serviceIntent)
    }

    fun stopVoiceSatellite() {
        val satellite = _voiceSatellite.getAndUpdate { null }
        if (satellite != null) {
            Log.d(TAG, "Stopping voice satellite")
            satellite.close()
            voiceSatelliteNsd.getAndSet(null)?.unregister(this)
            wifiWakeLock.release()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiWakeLock.create(applicationContext, TAG)
        settingsStore = VoiceSatelliteSettingsStore(applicationContext.voiceSatelliteSettingsStore)
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
            if (_voiceSatellite.value == null) {
                Log.d(TAG, "Starting voice satellite")
                startForeground(
                    2,
                    createVoiceSatelliteServiceNotification(this@VoiceSatelliteService)
                )
                settingsStore.ensureMacAddressIsSet()
                val settings = settingsStore.get()
                _voiceSatellite.value = createVoiceSatellite(settings).apply { start() }
                voiceSatelliteNsd.set(registerVoiceSatelliteNsd(settings))
                wifiWakeLock.acquire()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createVoiceSatellite(settings: VoiceSatelliteSettings): VoiceSatellite {
        val ttsPlayer = TtsPlayer(ExoPlayer.Builder(this@VoiceSatelliteService).build())
        val mediaPlayer = MediaPlayer(ExoPlayer.Builder(this@VoiceSatelliteService).build())
        return VoiceSatellite(
            coroutineContext = lifecycleScope.coroutineContext,
            name = settings.name,
            port = settings.serverPort,
            wakeWordProvider = AssetWakeWordProvider(assets),
            stopWordProvider = AssetWakeWordProvider(assets, "stopWords"),
            ttsPlayer = ttsPlayer,
            mediaPlayer = mediaPlayer,
            settingsStore = settingsStore
        )
    }

    private fun registerVoiceSatelliteNsd(settings: VoiceSatelliteSettings) =
        registerVoiceSatelliteNsd(
            context = this@VoiceSatelliteService,
            name = settings.name,
            port = settings.serverPort,
            macAddress = settings.macAddress
        )

    override fun onDestroy() {
        _voiceSatellite.getAndUpdate { null }?.close()
        voiceSatelliteNsd.getAndSet(null)?.unregister(this)
        wifiWakeLock.release()
        super.onDestroy()
    }

    companion object {
        const val TAG = "VoiceSatelliteService"
    }
}