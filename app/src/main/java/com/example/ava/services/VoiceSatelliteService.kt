package com.example.ava.services

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import java.io.File
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_SONIFICATION
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.C.USAGE_NOTIFICATION_RINGTONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.ava.esphome.Stopped
import kotlinx.coroutines.flow.distinctUntilChanged
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
import com.example.ava.settings.MicrophoneSettingsStore
import com.example.ava.settings.NotificationSettingsStore
import com.example.ava.settings.PlayerSettingsStore
import com.example.ava.settings.VoiceSatelliteSettings
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.ava.settings.microphoneSettingsStore
import com.example.ava.settings.notificationSettingsStore
import com.example.ava.notifications.NotificationScenes
import com.example.ava.settings.playerSettingsStore
import com.example.ava.settings.voiceSatelliteSettingsStore
import com.example.ava.settings.screensaverSettingsStore
import com.example.ava.update.AppUpdater
import com.example.ava.utils.RootHelper
import com.example.ava.utils.translate
import com.example.ava.wakelocks.BluetoothWakeLock
import com.example.ava.wakelocks.WifiWakeLock
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class VoiceSatelliteService() : LifecycleService() {
    private val wifiWakeLock = WifiWakeLock()
    private val bluetoothWakeLock = BluetoothWakeLock()
    private var screenReceiver: android.content.BroadcastReceiver? = null
    private val satelliteSettingsStore: VoiceSatelliteSettingsStore by lazy {
        VoiceSatelliteSettingsStore(applicationContext.voiceSatelliteSettingsStore)
    }
    private val microphoneSettingsStore: MicrophoneSettingsStore by lazy {
        MicrophoneSettingsStore(applicationContext.microphoneSettingsStore)
    }
    private val playerSettingsStore: PlayerSettingsStore by lazy {
        PlayerSettingsStore(applicationContext.playerSettingsStore)
    }
    private val notificationSettingsStore: NotificationSettingsStore by lazy {
        NotificationSettingsStore(applicationContext.notificationSettingsStore)
    }
    private val experimentalSettingsStore: com.example.ava.settings.ExperimentalSettingsStore by lazy {
        com.example.ava.settings.ExperimentalSettingsStore(applicationContext)
    }
    private val browserSettingsStore: com.example.ava.settings.BrowserSettingsStore by lazy {
        com.example.ava.settings.BrowserSettingsStore(applicationContext)
    }
    private var hideVinylJob: kotlinx.coroutines.Job? = null
    private var voiceSatelliteNsd = AtomicReference<NsdRegistration?>(null)
    internal val _voiceSatellite = MutableStateFlow<VoiceSatellite?>(null)
    private val initializing = java.util.concurrent.atomic.AtomicBoolean(false)
    
    
    private var cachedVinylCoverEnabled = false
    private suspend fun updateVinylCoverCache() {
        val vinylEnabled = playerSettingsStore.enableVinylCover.get()
        val haMediaEntity = satelliteSettingsStore.get().haMediaPlayerEntity
        cachedVinylCoverEnabled = vinylEnabled && haMediaEntity.isNotEmpty()
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private suspend fun syncOverlayPermissionState(): Boolean {
        if (hasOverlayPermission()) return true
        val settings = playerSettingsStore.get()
        var changed = false
        if (settings.enableFloatingWindow) {
            playerSettingsStore.enableFloatingWindow.set(false)
            changed = true
        }
        if (settings.enableHaSwitchOverlay) {
            playerSettingsStore.enableHaSwitchOverlay.set(false)
            changed = true
        }
        if (changed) {
            FloatingWindowService.hide(this)
            HaSwitchOverlayService.hide(this)
        }
        return false
    }
    
    
    private var lastPlayedUrl: String? = null
    
    
    private fun cancelHideJob() {
        hideVinylJob?.cancel()
        hideVinylJob = null
    }
    

    val voiceSatelliteState = _voiceSatellite.flatMapLatest {
        it?.state ?: flowOf(Stopped)
    }

    fun startVoiceSatellite() {
        
        getSharedPreferences("ava_prefs", MODE_PRIVATE).edit()
            .putBoolean("service_user_stopped", false)
            .apply()
        
        
        val packageName = packageName
        val serviceName = "com.example.ava.services.VoiceSatelliteService"
        RootHelper.installBootScript(packageName, serviceName)
        
        val serviceIntent = Intent(this, this::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }
    }

    private var originalBrightness: Float = -1f
    
    
    private val isQuadCoreA64Device by lazy {
        try {
            val cpuInfo = File("/proc/cpuinfo").readText()
            
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) ||
            cpuInfo.contains("A64")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cpuinfo", e)
            false
        }
    }
    
    private fun applyScreenToggle(screenOn: Boolean) {
        com.example.ava.utils.RootUtils.executeScreenToggle(this, screenOn)
    }
    
    private fun applyBrightnessToggle(screenOn: Boolean) {
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (screenOn) {
                    if (originalBrightness >= 0) {
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system screen_brightness ${(originalBrightness * 255).toInt()}")).waitFor()
                        originalBrightness = -1f
                    }
                } else {
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "settings get system screen_brightness"))
                    val output = proc.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (output.isNotEmpty()) {
                        originalBrightness = output.toFloat() / 255f
                    }
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system screen_brightness 0")).waitFor()
                }
                Log.d(TAG, "A64 brightness toggle: screenOn=$screenOn")
            } catch (e: Exception) {
                Log.e(TAG, "A64 brightness toggle failed", e)
            }
        }
    }
    
    private fun applyDisplayToggle(screenOn: Boolean) {
        
        lifecycleScope.launch(Dispatchers.IO) {
            val mode = if (screenOn) 2 else 0
            var success = false
            
            
            if (!success && com.example.ava.utils.RootUtils.isRootAvailable()) {
                try {
                    val localDexFile = java.io.File(filesDir, "DisplayToggle.dex")
                    if (!localDexFile.exists()) {
                        assets.open("DisplayToggle.dex").use { input ->
                            localDexFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    val cmd = "CLASSPATH=${localDexFile.absolutePath} app_process / DisplayToggle $mode"
                    success = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() == 0
                    Log.d(TAG, "DisplayToggle dex result: $success, mode=$mode")
                } catch (e: Exception) {
                    Log.e(TAG, "DisplayToggle dex failed", e)
                }
            }
            
            
            if (!success && com.example.ava.utils.ShizukuUtils.isShizukuPermissionGranted()) {
                try { 
                    success = com.example.ava.utils.ShizukuUtils.setDisplayPower(mode) 
                    Log.d(TAG, "DisplayToggle shizuku result: $success, mode=$mode")
                } catch (e: Exception) {
                    Log.e(TAG, "DisplayToggle shizuku failed", e)
                }
            }
            
            
            if (!success && com.example.ava.utils.RootUtils.isRootAvailable()) {
                try {
                    if (screenOn) {
                        val brightness = if (originalBrightness >= 0) (originalBrightness * 255).toInt() else 128
                        com.example.ava.utils.RootUtils.writeBacklightBrightness(brightness)
                        originalBrightness = -1f
                    } else {
                        val current = com.example.ava.utils.RootUtils.readBacklightBrightness()
                        if (current > 0) originalBrightness = current / 255f
                        com.example.ava.utils.RootUtils.writeBacklightBrightness(0)
                    }
                    Log.d(TAG, "DisplayToggle sys backlight: screenOn=$screenOn")
                } catch (e: Exception) {
                    Log.e(TAG, "DisplayToggle sys backlight failed", e)
                }
            }
        }
    }

    fun stopVoiceSatellite() {
        
        getSharedPreferences("ava_prefs", MODE_PRIVATE).edit()
            .putBoolean("service_user_stopped", true)
            .apply()
        
        val satellite = _voiceSatellite.getAndUpdate { null }
        if (satellite != null) {
            satellite.close()
            voiceSatelliteNsd.getAndSet(null)?.unregister(this)
            wifiWakeLock.release()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        
        initializing.set(false)
    }
    
    
    fun restartVoiceSatellite() {
        
        if (_voiceSatellite.value == null) return
        
        lifecycleScope.launch {
            stopVoiceSatellite()
            kotlinx.coroutines.delay(500) 
            startVoiceSatellite()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        requestBatteryOptimizationExemption()
        ScreensaverController.start(this)
        
        com.example.ava.utils.EchoShowSupport.grantOverlayPermissionIfNeeded(this)
        
        com.example.ava.utils.ShizukuUtils.init(packageName)
        
        bluetoothWakeLock.create(this, TAG)
        registerScreenReceiver()
        registerControlReceiver()
    }
    
    private var controlReceiver: android.content.BroadcastReceiver? = null
    
    private fun registerControlReceiver() {
        controlReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                val action = intent?.action ?: return
                Log.i(TAG, "Control action received: $action")
                when (action) {
                    "com.example.ava.ACTION_TOGGLE_MIC" -> toggleMicMute()
                    "com.example.ava.ACTION_MUTE_MIC" -> setMicMute(true)
                    "com.example.ava.ACTION_UNMUTE_MIC" -> setMicMute(false)
                    "com.example.ava.ACTION_WAKE" -> manualWake()
                    "com.example.ava.ACTION_STOP" -> stopVoiceSession()
                }
            }
        }
        
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.ava.ACTION_TOGGLE_MIC")
            addAction("com.example.ava.ACTION_MUTE_MIC")
            addAction("com.example.ava.ACTION_UNMUTE_MIC")
            addAction("com.example.ava.ACTION_WAKE")
            addAction("com.example.ava.ACTION_STOP")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter)
        }
        Log.i(TAG, "Control receiver registered")
    }
    
    private fun registerScreenReceiver() {
        screenReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                when (intent?.action) {
                    android.content.Intent.ACTION_SCREEN_OFF -> {
                        bluetoothWakeLock.acquire()
                    }
                    android.content.Intent.ACTION_SCREEN_ON -> {
                    }
                }
            }
        }
        
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request battery optimization exemption", e)
                }
            }
        }
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        val userStopped = getSharedPreferences("ava_prefs", MODE_PRIVATE)
            .getBoolean("service_user_stopped", false)
        
        
        if (!userStopped && _voiceSatellite.value != null) {
            val restartIntent = Intent(this, VoiceSatelliteService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }

    class VoiceSatelliteBinder(val service: VoiceSatelliteService) : Binder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return VoiceSatelliteBinder(this)
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        
        if (_voiceSatellite.value != null) {
            return super.onStartCommand(intent, flags, startId)
        }
        
        
        if (!initializing.compareAndSet(false, true)) {
            return super.onStartCommand(intent, flags, startId)
        }

        
        createVoiceSatelliteServiceNotificationChannel(this@VoiceSatelliteService)
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                2,
                createVoiceSatelliteServiceNotification(
                    this@VoiceSatelliteService,
                    "Starting..."
                ),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(
                2,
                createVoiceSatelliteServiceNotification(
                    this@VoiceSatelliteService,
                    "Starting..."
                )
            )
        }

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                wifiWakeLock.create(applicationContext, TAG)
                updateNotificationOnStateChanges()

                
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
                     2,
                     createVoiceSatelliteServiceNotification(
                         this@VoiceSatelliteService,
                         Stopped.translate(resources)
                     )
                )
                
                
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.ava.utils.RootUtils.requestRootPermission()
                        
                        com.example.ava.utils.RootUtils.setProcessHighPriority(android.os.Process.myPid())
                        com.example.ava.utils.RootUtils.acquireCpuWakeLock()
                        
                        com.example.ava.utils.RootUtils.enableAllCpuCoresForOcocciDevice()
                    } catch (e: Exception) {
                        Log.w(TAG, "Root request failed", e)
                    }
                }

                satelliteSettingsStore.ensureMacAddressIsSet()
                val settings = satelliteSettingsStore.get()
                _voiceSatellite.value = createVoiceSatellite(settings).apply { start() }
                voiceSatelliteNsd.getAndSet(null)?.unregister(this@VoiceSatelliteService)
                voiceSatelliteNsd.set(registerVoiceSatelliteNsd(settings))
                wifiWakeLock.acquire()
                startWakeLockRenewal()
                
                
                startSettingsWatcher()
                
                
                loadCustomScenesFromSettings()
                
                
                val playerSettings = playerSettingsStore.get()
                syncOverlayPermissionState()
                Log.d(TAG, "Overlay init: weather=${playerSettings.enableWeatherOverlay}/${playerSettings.enableWeatherOverlayVisible}, clock=${playerSettings.enableDreamClock}/${playerSettings.enableDreamClockVisible}")
                if (playerSettings.enableWeatherOverlay && playerSettings.enableWeatherOverlayVisible) {
                    WeatherOverlayService.show(this@VoiceSatelliteService)
                }
                if (playerSettings.enableDreamClock && playerSettings.enableDreamClockVisible) {
                    DreamClockService.show(this@VoiceSatelliteService)
                }
                
                
                initializing.set(false)
                
                
                startAutoUpdateChecker()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start voice satellite", e)
                initializing.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_STICKY
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.catchLog(name: String): kotlinx.coroutines.flow.Flow<T> = catch { e ->
        Log.e(TAG, "Settings watcher error in $name", e)
    }

    private fun startSettingsWatcher() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _voiceSatellite.flatMapLatest { satellite ->
                if (satellite == null) emptyFlow()
                else merge(
                    
                    
                    
                    satellite.audioInput.activeWakeWords.drop(1).onEach {
                        if (it.isNotEmpty()) {
                            val currentSettings = microphoneSettingsStore.get()
                            if (currentSettings.wakeWord != it.first()) {
                                microphoneSettingsStore.wakeWord.set(it.first())
                            }
                            if (currentSettings.wakeWords != it) {
                                microphoneSettingsStore.wakeWords.set(it)
                            }
                        }
                    }.catchLog("activeWakeWords"),
                    microphoneSettingsStore.wakeWords.drop(1).onEach { newWakeWords ->
                        val currentActive = satellite.audioInput.activeWakeWords.value
                        if (newWakeWords.isNotEmpty() && newWakeWords.toSet() != currentActive.toSet()) {
                            satellite.audioInput.setActiveWakeWords(newWakeWords)
                        }
                    }.catchLog("wakeWords"),
                    satellite.audioInput.muted.drop(1).onEach {
                        microphoneSettingsStore.muted.set(it)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val messageRes = if (it) {
                                com.example.ava.R.string.microphone_muted_toast
                            } else {
                                com.example.ava.R.string.microphone_unmuted_toast
                            }
                            android.widget.Toast.makeText(this@VoiceSatelliteService, getString(messageRes), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }.catchLog("muted"),
                    satellite.player.volume.drop(1).onEach {
                        playerSettingsStore.volume.set(it)
                    }.catchLog("volume"),
                    satellite.player.muted.drop(1).onEach {
                        playerSettingsStore.muted.set(it)
                    }.catchLog("playerMuted"),
                    satellite.player.enableScreenOff.drop(1).onEach {
                        playerSettingsStore.enableScreenOff.set(it)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            applyScreenToggle(it)
                        }
                    }.catchLog("screenOff"),
                    
                    
                    satellite.player.mediaPlayer.state.onEach { _ -> }.catchLog("mediaState"),
                    
                    playerSettingsStore.enableDreamClock.drop(1).distinctUntilChanged().onEach { enabled ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (enabled) {
                                DreamClockService.show(this@VoiceSatelliteService)
                            } else {
                                DreamClockService.hide(this@VoiceSatelliteService)
                            }
                        }
                    }.catchLog("dreamClock"),
                    
                    playerSettingsStore.enableWeatherOverlay.drop(1).distinctUntilChanged().onEach { enabled ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (enabled) {
                                WeatherOverlayService.show(this@VoiceSatelliteService)
                            } else {
                                WeatherOverlayService.hide(this@VoiceSatelliteService)
                            }
                        }
                    }.catchLog("weatherOverlay"),
                    
                    playerSettingsStore.enableWeatherOverlayVisible.drop(1).distinctUntilChanged().onEach { enabled ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val settings = playerSettingsStore.get()
                            if (settings.enableWeatherOverlay) {
                                if (enabled) {
                                    WeatherOverlayService.show(this@VoiceSatelliteService)
                                } else {
                                    WeatherOverlayService.setVisible(this@VoiceSatelliteService, false)
                                }
                            }
                        }
                    }.catchLog("weatherVisible"),
                    
                    playerSettingsStore.enableDreamClockVisible.drop(1).distinctUntilChanged().onEach { enabled ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val settings = playerSettingsStore.get()
                            if (settings.enableDreamClock) {
                                if (enabled) {
                                    DreamClockService.show(this@VoiceSatelliteService)
                                } else {
                                    DreamClockService.setVisible(this@VoiceSatelliteService, false)
                                }
                            }
                        }
                    }.catchLog("dreamClockVisible"),
                    
                    satellite.player.haRemoteUrl.drop(1).onEach { url ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val browserSettings = browserSettingsStore.get()
                            if (browserSettings.enableBrowserDisplay && browserSettings.enableBrowserVisible) {
                                if (url.isNotEmpty()) {
                                    WebViewService.show(this@VoiceSatelliteService, url)
                                } else {
                                    WebViewService.hide(this@VoiceSatelliteService)
                                }
                            }
                        }
                    }.catchLog("haRemoteUrl"),
                    
                    satellite.player.notificationScene.drop(1).onEach { sceneTitle ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (sceneTitle.isNotEmpty()) {
                                NotificationOverlayService.showSceneByTitle(this@VoiceSatelliteService, sceneTitle)
                            }
                        }
                    }.catchLog("notificationScene"),
                    
                    browserSettingsStore.enableBrowserVisible.drop(1).onEach { visible ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (!visible) {
                                WebViewService.destroy(this@VoiceSatelliteService)
                            } else {
                                val browserSettings = browserSettingsStore.get()
                                if (browserSettings.haRemoteUrlEnabled && browserSettings.enableBrowserDisplay) {
                                    val savedVoiceSettings = satelliteSettingsStore.get()
                                    if (savedVoiceSettings.haRemoteUrl.isNotEmpty()) {
                                        WebViewService.show(this@VoiceSatelliteService, savedVoiceSettings.haRemoteUrl)
                                    }
                                }
                            }
                        }
                    }.catchLog("browserVisible"),
                    
                    browserSettingsStore.userAgentMode.drop(1).distinctUntilChanged().onEach { _ ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val browserSettings = browserSettingsStore.get()
                            if (browserSettings.enableBrowserVisible) {
                                val savedVoiceSettings = satelliteSettingsStore.get()
                                if (savedVoiceSettings.haRemoteUrl.isNotEmpty()) {
                                    WebViewService.destroy(this@VoiceSatelliteService)
                                    kotlinx.coroutines.delay(100)
                                    WebViewService.show(this@VoiceSatelliteService, savedVoiceSettings.haRemoteUrl)
                                }
                            }
                        }
                    }.catchLog("userAgentMode"),
                    
                    playerSettingsStore.enableVinylCover.drop(1).distinctUntilChanged().onEach { _ ->
                        updateVinylCoverCache()
                    }.catchLog("vinylCover")
                )
            }.collect {}
        }
    }
    
    
    private suspend fun loadCustomScenesFromSettings() {
        val notificationSettings = notificationSettingsStore.get()
        val url = notificationSettings.customSceneUrl
        if (url.isNotBlank()) {
            NotificationScenes.loadCustomSceneFromUrl(url)
        }
    }
                    
                    private suspend fun createVoiceSatellite(satelliteSettings: VoiceSatelliteSettings): VoiceSatellite {
                        val microphoneSettings = microphoneSettingsStore.get()
                        val savedWakeWords = microphoneSettings.wakeWords.ifEmpty { listOf(microphoneSettings.wakeWord) }
                        val audioInput = VoiceSatelliteAudioInput(
                            activeWakeWords = savedWakeWords,
                            activeStopWords = listOf(microphoneSettings.stopWord),
                            wakeWordProvider = AssetWakeWordProvider(assets),
                            stopWordProvider = AssetWakeWordProvider(assets, "stopWords"),
                            muted = microphoneSettings.muted
                        )
                    
                        val playerSettings = playerSettingsStore.get()
                        cachedVinylCoverEnabled = playerSettings.enableVinylCover && satelliteSettings.haMediaPlayerEntity.isNotEmpty()
                        val browserSettingsData = browserSettingsStore.get()
                        val screensaverSettingsData = com.example.ava.settings.ScreensaverSettingsStore(
                            applicationContext.screensaverSettingsStore
                        ).get()
                        
                        val player = VoiceSatellitePlayer(
                            ttsPlayer = TtsPlayer(
                                createAudioPlayer(
                                    USAGE_MEDIA,
                                    AUDIO_CONTENT_TYPE_MUSIC,
                                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                                )
                            ),
                            mediaPlayer = createAudioPlayer(
                                USAGE_MEDIA,
                                AUDIO_CONTENT_TYPE_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN
                            ),
                            wakeSoundPlayer = createAudioPlayer(
                                USAGE_MEDIA,
                                AUDIO_CONTENT_TYPE_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                            ),
                            volume = playerSettings.volume,
                            muted = playerSettings.muted,
                            enableWakeSound = playerSettingsStore.enableWakeSound,
                            enableScreenOff = playerSettingsStore.enableScreenOff,
                            wakeSound = playerSettingsStore.wakeSound,
                            wakeSound2 = playerSettingsStore.wakeSound2,
                            timerFinishedSound = playerSettingsStore.timerFinishedSound,
                            stopSound = playerSettingsStore.stopSound,
                            enableStopSound = playerSettingsStore.enableStopSound,
                            continuousPromptSound = playerSettingsStore.continuousPromptSound,
                            enableContinuousConversation = playerSettingsStore.enableContinuousConversation
                        ).apply {
                            onMediaPlay = { url ->
                                lastPlayedUrl = url
                            }
                            onMediaPause = {
                            }
                            onMediaResume = {
                            }
                            onMediaStop = {
                            }
                            onMediaCover = { coverUrl ->
                            }
                            onMediaDuration = { duration ->
                                
                                
                            }
                            onPlaybackEnded = {
                            }
                            onHaCoverUrl = { coverUrl ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled && coverUrl.isNotEmpty()) {
                                        VinylCoverService.setHaCover(this@VoiceSatelliteService, coverUrl)
                                    }
                                }
                            }
                            onHaMediaTitle = { title ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled && title.isNotEmpty()) {
                                        VinylCoverService.updateMetadata(
                                            this@VoiceSatelliteService,
                                            songTitle = title,
                                            artistName = null,
                                            isPlaying = true
                                        )
                                    }
                                }
                            }
                            onHaMediaArtist = { artist ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled && artist.isNotEmpty()) {
                                        VinylCoverService.updateMetadata(
                                            this@VoiceSatelliteService,
                                            songTitle = null,
                                            artistName = artist
                                        )
                                    }
                                }
                            }
                            onHaVolumeLevel = { volume ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled) {
                                        VinylCoverService.updatePlaybackSettings(
                                            this@VoiceSatelliteService,
                                            volumeLevel = volume
                                        )
                                    }
                                }
                            }
                            onHaRepeatMode = { mode ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled) {
                                        VinylCoverService.updatePlaybackSettings(
                                            this@VoiceSatelliteService,
                                            repeatMode = mode
                                        )
                                    }
                                }
                            }
                            onHaShuffle = { shuffle ->
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled) {
                                        VinylCoverService.updatePlaybackSettings(
                                            this@VoiceSatelliteService,
                                            shuffleEnabled = shuffle
                                        )
                                    }
                                }
                            }
                            onHaPlaybackStateWithMetadata = { isPlaying, hasMetadata ->
                                val coverCache = haMediaCoverCache
                                val titleCache = haMediaTitleCache
                                val artistCache = haMediaArtistCache
                                lifecycleScope.launch {
                                    if (cachedVinylCoverEnabled) {
                                        if (isPlaying && hasMetadata) {
                                            cancelHideJob()
                                            VinylCoverService.show(
                                                this@VoiceSatelliteService,
                                                coverUrl = coverCache.ifEmpty { null },
                                                songTitle = titleCache.ifEmpty { null },
                                                artistName = artistCache.ifEmpty { null }
                                            )
                                        } else if (!isPlaying) {
                                            cancelHideJob()
                                            hideVinylJob = lifecycleScope.launch {
                                                kotlinx.coroutines.delay(3000)
                                                VinylCoverService.hide(this@VoiceSatelliteService)
                                            }
                                        }
                                        VinylCoverService.updatePlaybackState(
                                            this@VoiceSatelliteService,
                                            isPlaying = isPlaying
                                        )
                                    }
                                }
                            }
                        }
                    
                        return VoiceSatellite(
                            coroutineContext = lifecycleScope.coroutineContext,
                            name = satelliteSettings.name,
                            port = satelliteSettings.serverPort,
                            audioInput = audioInput,
                            player = player,
                            settingsStore = satelliteSettingsStore,
                            notificationSettingsStore = notificationSettingsStore,
                            experimentalSettingsStore = experimentalSettingsStore,
                            playerSettingsStore = playerSettingsStore,
                            browserSettingsData = browserSettingsData,
                            screensaverSettingsData = screensaverSettingsData,
                            playerSettingsData = playerSettings,
                            onRestartService = { restartVoiceSatellite() },
                            context = this@VoiceSatelliteService
                        ).apply {
                            val expSettings = experimentalSettingsStore.get()
                            multiDeviceArbiterEnabled = expSettings.multiDeviceArbiterEnabled
                            
                            onConversationText = { role, text ->
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get() && role == "assistant") {
                                        FloatingWindowService.showAssistantText(this@VoiceSatelliteService, text)
                                    }
                                }
                            }
                            
                            onListeningStarted = {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.showListening(this@VoiceSatelliteService)
                                    }
                                }
                            }
                            
                            onProcessingStarted = {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.showProcessing(this@VoiceSatelliteService)
                                    }
                                }
                            }
                            
                            onStreamingDelta = { text ->
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.appendText(this@VoiceSatelliteService, text)
                                    }
                                }
                            }
                            
                            onStreamingFinished = {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.finishStreaming(this@VoiceSatelliteService)
                                    }
                                }
                            }
                            
                            onConversationEnd = {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    
                                    FloatingWindowService.hide(this@VoiceSatelliteService)
                                }
                            }
                            
                            onDeviceAction = { action ->
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableHaSwitchOverlay.get()) {
                                        HaSwitchOverlayService.showDeviceAction(
                                            this@VoiceSatelliteService,
                                            action.type,
                                            action.isOn
                                        )
                                    }
                                }
                            }
                            
                            onTtsDurationReady = { durationMs, text ->
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.showKaraokeText(this@VoiceSatelliteService, text, durationMs)
                                    }
                                }
                            }
                            
                            onTtsProgressUpdate = { currentMs, totalMs, text ->
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    if (!syncOverlayPermissionState()) return@launch
                                    if (playerSettingsStore.enableFloatingWindow.get()) {
                                        FloatingWindowService.updateKaraokeProgress(this@VoiceSatelliteService, currentMs, totalMs)
                                    }
                                }
                            }
                            
                        }
                    }
                    
                    private fun updateNotificationOnStateChanges() {
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            _voiceSatellite
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
                                }.collect {}
                        }
                    }
    fun createAudioPlayer(usage: Int, contentType: Int, focusGain: Int): AudioPlayer {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        val handleAudioFocus = (usage == USAGE_MEDIA)
        return AudioPlayer(audioManager, focusGain) {
            val dataSourceFactory = DefaultDataSource.Factory(this@VoiceSatelliteService)
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 2000, 100, 100)
                .build()
            ExoPlayer.Builder(this@VoiceSatelliteService)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build(),
                    handleAudioFocus
                ).build()
        }
    }
    

    private fun registerVoiceSatelliteNsd(settings: VoiceSatelliteSettings) =
        registerVoiceSatelliteNsd(
            context = this@VoiceSatelliteService,
            name = settings.name,
            port = settings.serverPort,
            macAddress = settings.macAddress,
            onNameChanged = { newName ->
                
                lifecycleScope.launch {
                    satelliteSettingsStore.saveName(newName)
                }
            }
        )
    
    
    override fun onDestroy() {
        instance = null
        _voiceSatellite.getAndUpdate { null }?.close()
        voiceSatelliteNsd.getAndSet(null)?.unregister(this)
        wifiWakeLock.release()
        bluetoothWakeLock.release()
        
        screenReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        screenReceiver = null
        
        controlReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        controlReceiver = null
        
        com.example.ava.utils.RootUtils.releaseCpuWakeLock()
        
        updateCheckHandler.removeCallbacksAndMessages(null)
        
        DreamClockService.hide(this)
        WeatherOverlayService.hide(this)
        VinylCoverService.hide(this)
        FloatingWindowService.hide(this)
        
        RootHelper.removeBootScript()
        initializing.set(false)
        super.onDestroy()
    }
    
    
    private val updateCheckHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L  
    private val WAKELOCK_RENEWAL_INTERVAL = 25 * 60 * 1000L
    
    private fun startWakeLockRenewal() {
        updateCheckHandler.postDelayed(object : Runnable {
            override fun run() {
                if (_voiceSatellite.value != null) {
                    wifiWakeLock.renewIfNeeded()
                    bluetoothWakeLock.renewIfNeeded()
                    updateCheckHandler.postDelayed(this, WAKELOCK_RENEWAL_INTERVAL)
                }
            }
        }, WAKELOCK_RENEWAL_INTERVAL)
    }
    
    private fun startAutoUpdateChecker() {
    }

    
    fun triggerManualWake() {
        _voiceSatellite.value?.triggerManualWake()
    }
    
    
    fun getState(): com.example.ava.esphome.EspHomeState {
        return _voiceSatellite.value?.state?.value ?: com.example.ava.esphome.Disconnected
    }
    
    fun onScreenTouch(isTouching: Boolean) {
        _voiceSatellite.value?.onScreenTouch(isTouching)
    }
    
    suspend fun callHaService(service: String, entityId: String) {
        _voiceSatellite.value?.callHaServicePublic(service, entityId)
    }
    
    fun getQuickEntityStates(): Map<String, String> {
        return _voiceSatellite.value?.getQuickEntityStateCache() ?: emptyMap()
    }
    
    fun getQuickEntityUnits(): Map<String, String> {
        return _voiceSatellite.value?.getQuickEntityUnitCache() ?: emptyMap()
    }
    
    suspend fun resubscribeQuickEntities() {
        _voiceSatellite.value?.subscribeQuickEntities()
    }
    
    companion object {
        const val TAG = "VoiceSatelliteService"
        
        private var instance: VoiceSatelliteService? = null
        
        fun getInstance(): VoiceSatelliteService? = instance
        
        fun toggleMicMute() {
            instance?._voiceSatellite?.value?.toggleMicMute()
        }
        
        fun setMicMute(muted: Boolean) {
            instance?._voiceSatellite?.value?.setMicMute(muted)
        }
        
        fun manualWake() {
            instance?._voiceSatellite?.value?.manualWake()
        }
        
        fun stopVoiceSession() {
            instance?._voiceSatellite?.value?.stopVoiceSession()
        }
    }
}
