package com.example.ava.services

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH
import androidx.media3.common.C.USAGE_ASSISTANT
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.ava.esphome.Stopped
import com.example.ava.esphome.voicesatellite.VoiceSatellite
import com.example.ava.esphome.voicesatellite.VoiceSatelliteAudioInput
import com.example.ava.esphome.voicesatellite.VoiceSatellitePlayer
import com.example.ava.microwakeword.AssetWakeWordProvider
import com.example.ava.notifications.createVoiceSatelliteServiceNotification
import com.example.ava.notifications.createVoiceSatelliteServiceNotificationChannel
import com.example.ava.nsd.NsdRegistration
import com.example.ava.nsd.registerVoiceSatelliteNsd
import com.example.ava.players.AudioPlayer
import com.example.ava.players.TtsPlayer
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.VoiceSatelliteSettings
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.ava.settings.playerSettingsStore
import com.example.ava.settings.voiceSatelliteSettingsStore
import com.example.ava.utils.translate
import com.example.ava.wakelocks.WifiWakeLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class VoiceSatelliteService() : LifecycleService() {
    private val wifiWakeLock = WifiWakeLock()
    private lateinit var satelliteSettingsStore: VoiceSatelliteSettingsStore
    private lateinit var playerSettingsStore: PlayerSettingsStore
    private var voiceSatelliteNsd = AtomicReference<NsdRegistration?>(null)
    private val _voiceSatellite = MutableStateFlow<VoiceSatellite?>(null)

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
        satelliteSettingsStore =
            VoiceSatelliteSettingsStore(applicationContext.voiceSatelliteSettingsStore)
        playerSettingsStore = PlayerSettingsStore(applicationContext.playerSettingsStore)
        createVoiceSatelliteServiceNotificationChannel(this)
        updateNotificationOnStateChanges()
        startSettingsWatcher()
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
                    createVoiceSatelliteServiceNotification(
                        this@VoiceSatelliteService,
                        Stopped.translate(resources)
                    )
                )
                satelliteSettingsStore.ensureMacAddressIsSet()
                val settings = satelliteSettingsStore.get()
                _voiceSatellite.value = createVoiceSatellite(settings).apply { start() }
                voiceSatelliteNsd.set(registerVoiceSatelliteNsd(settings))
                wifiWakeLock.acquire()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startSettingsWatcher() {
        _voiceSatellite.flatMapLatest { satellite ->
            if (satellite == null) emptyFlow()
            else merge(
                // Update settings when satellite changes,
                // dropping the initial value to avoid overwriting
                // settings with the initial/default values
                satellite.audioInput.activeWakeWords.drop(1).onEach {
                    satelliteSettingsStore.wakeWord.set(if (it.isNotEmpty()) it.first() else "")
                },
                satellite.player.volume.drop(1).onEach {
                    playerSettingsStore.volume.set(it)
                },
                satellite.player.muted.drop(1).onEach {
                    playerSettingsStore.muted.set(it)
                }
            )
        }.launchIn(lifecycleScope)
    }

    private suspend fun createVoiceSatellite(satelliteSettings: VoiceSatelliteSettings): VoiceSatellite {
        val audioInput = VoiceSatelliteAudioInput(
            activeWakeWords = listOf(satelliteSettings.wakeWord),
            activeStopWords = listOf(satelliteSettings.stopWord),
            wakeWordProvider = AssetWakeWordProvider(assets),
            stopWordProvider = AssetWakeWordProvider(assets, "stopWords"),
        )

        val playerSettings = playerSettingsStore.get()
        val player = VoiceSatellitePlayer(
            ttsPlayer = TtsPlayer(
                createAudioPlayer(
                    USAGE_ASSISTANT,
                    AUDIO_CONTENT_TYPE_SPEECH,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            ),
            mediaPlayer = createAudioPlayer(
                USAGE_MEDIA,
                AUDIO_CONTENT_TYPE_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ),
            volume = playerSettings.volume,
            muted = playerSettings.muted,
            enableWakeSound = playerSettingsStore.enableWakeSound,
            wakeSound = playerSettingsStore.wakeSound,
            timerFinishedSound = playerSettingsStore.timerFinishedSound
        )

        return VoiceSatellite(
            coroutineContext = lifecycleScope.coroutineContext,
            name = satelliteSettings.name,
            port = satelliteSettings.serverPort,
            audioInput = audioInput,
            player = player,
            settingsStore = satelliteSettingsStore
        )
    }

    private fun updateNotificationOnStateChanges() = _voiceSatellite
        .flatMapLatest {
            it?.state ?: emptyFlow()
        }
        .onEach {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
                2,
                createVoiceSatelliteServiceNotification(
                    this@VoiceSatelliteService,
                    it.translate(resources)
                )
            )
        }
        .launchIn(lifecycleScope)

    fun createAudioPlayer(usage: Int, contentType: Int, focusGain: Int): AudioPlayer {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return AudioPlayer(audioManager, focusGain) {
            ExoPlayer.Builder(this@VoiceSatelliteService).setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build(),
                false
            ).build()
        }
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