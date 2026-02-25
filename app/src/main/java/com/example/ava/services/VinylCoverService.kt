package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import com.example.ava.R
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ava.ui.components.GlassMusicPlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.net.URL

class VinylCoverService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {


    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private val viewModelStoreImpl = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = viewModelStoreImpl
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    

    private var coverUrl by mutableStateOf<String?>(null)
    private var coverBitmap by mutableStateOf<Bitmap?>(null)
    private var songTitle by mutableStateOf("")
    private var artistName by mutableStateOf("")
    private var isPlaying by mutableStateOf(true)
    private var currentTimeMs by mutableStateOf(0L)
    private var totalTimeMs by mutableStateOf(0L)
    private var volumeLevel by mutableStateOf(1.0f)
    private var repeatMode by mutableStateOf("off")
    private var shuffleEnabled by mutableStateOf(false)
    
    private var windowParams: WindowManager.LayoutParams? = null
    private var cachedHaCoverUrl: String? = null
    private var skipStateUpdateUntil: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VinylCoverService)
            setViewTreeViewModelStoreOwner(this@VinylCoverService)
            setViewTreeSavedStateRegistryOwner(this@VinylCoverService)
            
            setContent {
                GlassMusicPlayerView(
                    coverUrl = coverUrl,
                    coverBitmap = coverBitmap,
                    songTitle = songTitle,
                    artistName = artistName,
                    isPlaying = isPlaying,
                    volumeLevel = volumeLevel,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    onPlayPauseClick = {
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haMediaPlayPause()
                            }
                        }
                    },
                    onPreviousClick = {
                        isPlaying = true
                        skipStateUpdateUntil = System.currentTimeMillis() + 2000
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haMediaPrevious()
                            }
                        }
                    },
                    onNextClick = {
                        isPlaying = true
                        skipStateUpdateUntil = System.currentTimeMillis() + 2000
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haMediaNext()
                            }
                        }
                    },
                    onVolumeChange = { volume ->
                        volumeLevel = volume
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haSetVolume(volume)
                            }
                        }
                    },
                    onRepeatClick = {
                        val nextMode = when (repeatMode) {
                            "off" -> "all"
                            "all" -> "one"
                            else -> "off"
                        }
                        repeatMode = nextMode
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haSetRepeat(nextMode)
                            }
                        }
                    },
                    onShuffleClick = {
                        val newShuffle = !shuffleEnabled
                        shuffleEnabled = newShuffle
                        VoiceSatelliteService.getInstance()?.let { service ->
                            service.lifecycleScope.launch {
                                service._voiceSatellite.value?.player?.haSetShuffle(newShuffle)
                            }
                        }
                    },
                    onBackgroundClick = {
                        android.widget.Toast.makeText(this@VinylCoverService, getString(R.string.vinyl_cover_hiding), android.widget.Toast.LENGTH_SHORT).show()
                        hide()
                    }
                )
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowParams = params
        
        try {
            windowManager?.addView(composeView, params)
            composeView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create glass music player window", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> {
                var url = intent.getStringExtra(EXTRA_COVER_URL)
                val title = intent.getStringExtra(EXTRA_SONG_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST_NAME)
                val currentMs = intent.getLongExtra(EXTRA_CURRENT_TIME_MS, 0L)
                val totalMs = intent.getLongExtra(EXTRA_TOTAL_TIME_MS, 0L)
                

                if (url == null && cachedHaCoverUrl != null) {
                    url = cachedHaCoverUrl
                }
                

                title?.let { songTitle = it }
                artist?.let { artistName = it }

                isPlaying = true
                currentTimeMs = currentMs
                totalTimeMs = totalMs
                
                if (url != null && url != coverUrl) {
                    coverUrl = url
                    loadCoverImage(url)
                }
                
                if (songTitle.isNotEmpty()) {
                    show()
                }
            }
            ACTION_HIDE -> {
                hide()
            }
            ACTION_SET_HA_COVER -> {
                val url = intent.getStringExtra(EXTRA_COVER_URL)
                if (!url.isNullOrEmpty()) {
                    cachedHaCoverUrl = url
                    if (composeView?.visibility == android.view.View.VISIBLE && url != coverUrl) {
                        coverUrl = url
                        loadCoverImage(url)
                    }
                }
            }
            ACTION_UPDATE_COVER -> {
                val url = intent.getStringExtra(EXTRA_COVER_URL)
                url?.let {
                    coverUrl = it
                    loadCoverImage(it)
                }
            }
            ACTION_UPDATE_METADATA -> {
                val title = intent.getStringExtra(EXTRA_SONG_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST_NAME)
                val currentMs = intent.getLongExtra(EXTRA_CURRENT_TIME_MS, currentTimeMs)
                val totalMs = intent.getLongExtra(EXTRA_TOTAL_TIME_MS, totalTimeMs)
                
                title?.let { songTitle = it }
                artist?.let { artistName = it }
                if (title != null && title.isNotEmpty()) {
                    isPlaying = true
                } else if (intent.hasExtra(EXTRA_IS_PLAYING) && System.currentTimeMillis() > skipStateUpdateUntil) {
                    isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying)
                }
                currentTimeMs = currentMs
                totalTimeMs = totalMs
            }
            ACTION_UPDATE_PLAYBACK_STATE -> {
                if (System.currentTimeMillis() > skipStateUpdateUntil) {
                    isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying)
                }
            }
            ACTION_UPDATE_PROGRESS -> {
                currentTimeMs = intent.getLongExtra(EXTRA_CURRENT_TIME_MS, currentTimeMs)
                totalTimeMs = intent.getLongExtra(EXTRA_TOTAL_TIME_MS, totalTimeMs)
            }
            ACTION_UPDATE_PLAYBACK_SETTINGS -> {
                if (intent.hasExtra(EXTRA_VOLUME_LEVEL)) {
                    volumeLevel = intent.getFloatExtra(EXTRA_VOLUME_LEVEL, volumeLevel)
                }
                if (intent.hasExtra(EXTRA_REPEAT_MODE)) {
                    repeatMode = intent.getStringExtra(EXTRA_REPEAT_MODE) ?: repeatMode
                }
                if (intent.hasExtra(EXTRA_SHUFFLE_ENABLED)) {
                    shuffleEnabled = intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, shuffleEnabled)
                }
            }
        }
    }

    private fun loadCoverImage(url: String) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    try {
                        val bufferedInput = java.io.BufferedInputStream(connection.inputStream)
                        bufferedInput.mark(Int.MAX_VALUE)
                        
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(bufferedInput, null, options)
                        
                        bufferedInput.reset()
                        
                        val maxSize = 1080
                        var sampleSize = 1
                        if (options.outWidth > maxSize || options.outHeight > maxSize) {
                            val widthRatio = options.outWidth / maxSize
                            val heightRatio = options.outHeight / maxSize
                            sampleSize = maxOf(widthRatio, heightRatio)
                        }
                        
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }
                        val result = BitmapFactory.decodeStream(bufferedInput, null, decodeOptions)
                        bufferedInput.close()
                        result
                    } finally {
                        connection.disconnect()
                    }
                }
                coverBitmap = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Load cover failed: $url", e)
                coverBitmap = null
            }
        }
    }
    
    private fun show() {
        if (songTitle.isEmpty()) {
            return
        }
        handler.post {
            try {
                composeView?.let { view ->
                    if (view.visibility == View.GONE) {
                        windowManager?.removeView(view)
                        windowManager?.addView(view, windowParams)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-add view", e)
            }
            composeView?.visibility = View.VISIBLE
        }
    }

    private fun hide() {
        handler.post {
            composeView?.visibility = View.GONE
            songTitle = ""
            artistName = ""
            coverUrl = null
            coverBitmap?.let { if (!it.isRecycled) it.recycle() }
            coverBitmap = null
            cachedHaCoverUrl = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        handler.removeCallbacksAndMessages(null)
        coroutineScope.cancel()
        viewModelStoreImpl.clear()
        
        coverBitmap?.let { if (!it.isRecycled) it.recycle() }
        coverBitmap = null
        
        try { 
            composeView?.let { windowManager?.removeView(it) } 
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove glass music player window", e)
        }
    }

    companion object {
        private const val TAG = "VinylCoverService"
        
        const val ACTION_SHOW = "com.example.ava.SHOW_VINYL"
        const val ACTION_HIDE = "com.example.ava.HIDE_VINYL"
        const val ACTION_UPDATE_COVER = "com.example.ava.UPDATE_VINYL_COVER"
        const val ACTION_SET_HA_COVER = "com.example.ava.SET_HA_COVER"
        const val ACTION_UPDATE_METADATA = "com.example.ava.UPDATE_METADATA"
        const val ACTION_UPDATE_PLAYBACK_STATE = "com.example.ava.UPDATE_PLAYBACK_STATE"
        const val ACTION_UPDATE_PROGRESS = "com.example.ava.UPDATE_PROGRESS"
        
        const val EXTRA_COVER_URL = "cover_url"
        const val EXTRA_SONG_TITLE = "song_title"
        const val EXTRA_ARTIST_NAME = "artist_name"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_CURRENT_TIME_MS = "current_time_ms"
        const val EXTRA_TOTAL_TIME_MS = "total_time_ms"
        const val EXTRA_VOLUME_LEVEL = "volume_level"
        const val EXTRA_REPEAT_MODE = "repeat_mode"
        const val EXTRA_SHUFFLE_ENABLED = "shuffle_enabled"
        
        const val ACTION_UPDATE_PLAYBACK_SETTINGS = "com.example.ava.UPDATE_PLAYBACK_SETTINGS"

        fun show(context: Context, coverUrl: String? = null, songTitle: String? = null, artistName: String? = null, isPlaying: Boolean = true, currentTimeMs: Long = 0L, totalTimeMs: Long = 0L) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_SHOW
                coverUrl?.let { putExtra(EXTRA_COVER_URL, it) }
                songTitle?.let { putExtra(EXTRA_SONG_TITLE, it) }
                artistName?.let { putExtra(EXTRA_ARTIST_NAME, it) }
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_CURRENT_TIME_MS, currentTimeMs)
                putExtra(EXTRA_TOTAL_TIME_MS, totalTimeMs)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
        
        fun setHaCover(context: Context, coverUrl: String) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_SET_HA_COVER
                putExtra(EXTRA_COVER_URL, coverUrl)
            }
            context.startService(intent)
        }

        fun updateCover(context: Context, coverUrl: String) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_UPDATE_COVER
                putExtra(EXTRA_COVER_URL, coverUrl)
            }
            context.startService(intent)
        }
        
        fun updateMetadata(context: Context, songTitle: String?, artistName: String?, isPlaying: Boolean? = null, currentTimeMs: Long? = null, totalTimeMs: Long? = null) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_UPDATE_METADATA
                songTitle?.let { putExtra(EXTRA_SONG_TITLE, it) }
                artistName?.let { putExtra(EXTRA_ARTIST_NAME, it) }
                isPlaying?.let { putExtra(EXTRA_IS_PLAYING, it) }
                currentTimeMs?.let { putExtra(EXTRA_CURRENT_TIME_MS, it) }
                totalTimeMs?.let { putExtra(EXTRA_TOTAL_TIME_MS, it) }
            }
            context.startService(intent)
        }
        
        fun updatePlaybackState(context: Context, isPlaying: Boolean) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_UPDATE_PLAYBACK_STATE
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startService(intent)
        }
        
        fun updatePlaybackSettings(context: Context, volumeLevel: Float? = null, repeatMode: String? = null, shuffleEnabled: Boolean? = null) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_UPDATE_PLAYBACK_SETTINGS
                volumeLevel?.let { putExtra(EXTRA_VOLUME_LEVEL, it) }
                repeatMode?.let { putExtra(EXTRA_REPEAT_MODE, it) }
                shuffleEnabled?.let { putExtra(EXTRA_SHUFFLE_ENABLED, it) }
            }
            context.startService(intent)
        }
    }
}

