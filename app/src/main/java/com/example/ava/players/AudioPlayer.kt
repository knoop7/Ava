package com.example.ava.players

import android.media.AudioManager
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.ava.utils.GpioAecController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class AudioPlayerState {
    PLAYING, PAUSED, IDLE
}

@UnstableApi
class AudioPlayer(
    private val audioManager: AudioManager,
    val focusGain: Int,
    private val playerBuilder: () -> Player
) : AutoCloseable {
    private var _player: Player? = null
    private var isPlayerInit = false
    private var currentListener: Player.Listener? = null
    private val isClosed = AtomicBoolean(false)
    private val isClosing = AtomicBoolean(false)

    private val _state = MutableStateFlow(AudioPlayerState.IDLE)
    val state = _state.asStateFlow()

    val isPlaying: Boolean get() = try { _player?.isPlaying ?: false } catch (e: Exception) { false }
    val isPaused: Boolean
        get() = try {
            _player?.let {
                !it.isPlaying && it.playbackState != Player.STATE_IDLE && it.playbackState != Player.STATE_ENDED
            } ?: false
        } catch (e: Exception) { false }
    val isStopped
        get() = try {
            _player?.let { it.playbackState == Player.STATE_IDLE || it.playbackState == Player.STATE_ENDED } ?: true
        } catch (e: Exception) { true }
    
    
    val currentPosition: Long get() = try { _player?.currentPosition ?: 0L } catch (e: Exception) { 0L }
    val duration: Long get() = try { _player?.duration?.let { if (it > 0 && it != Long.MIN_VALUE + 1) it else 0L } ?: 0L } catch (e: Exception) { 0L }
    
    
    fun seekTo(positionMs: Long) {
        try { _player?.seekTo(positionMs) } catch (e: Exception) { }
    }

    private var _volume: Float = 1.0f
    var volume
        get() = _volume
        set(value) {
            _volume = value
            try { _player?.volume = value } catch (e: Exception) { }
        }

    fun init() {
        if (isClosed.get()) return
        
        val oldPlayer = _player
        _player = null
        oldPlayer?.let { player ->
            try {
                player.stop()
                player.clearMediaItems()
            } catch (e: Exception) { }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try { player.release() } catch (e: Exception) { }
            }
        }
        
        try {
            _player = playerBuilder().apply {
                volume = _volume
            }
            
            currentListener?.let { listener ->
                _player?.addListener(listener)
            }
            isPlayerInit = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init player", e)
        }
    }

    fun play(mediaUri: String, onCompletion: () -> Unit = {}) {
        play(listOf(mediaUri), onCompletion)
    }

    fun play(mediaUris: Iterable<String>, onCompletion: () -> Unit = {}) {
        if (isClosed.get()) {
            onCompletion()
            return
        }
        
        if (!isPlayerInit) init()
        if (_player == null) {
            Log.e(TAG, "Player is null, cannot play")
            onCompletion()
            return
        }

        isPlayerInit = false
        val player = _player ?: run {
            onCompletion()
            return
        }

        try {
            
            player.stop()
            player.clearMediaItems()
            currentListener?.let { 
                try { player.removeListener(it) } catch (e: Exception) { }
            }
            
            val listener = getPlayerListener(onCompletion)
            currentListener = listener
            player.addListener(listener)
            
            for (mediaUri in mediaUris) {
                player.addMediaItem(MediaItem.fromUri(mediaUri))
            }
            player.playWhenReady = true
            player.prepare()
            
            
            GpioAecController.activateAEC()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing media $mediaUris", e)
            onCompletion()
            safeClose()
        }
    }

    fun pause() {
        try { 
            if (isPlaying) {
                _player?.pause()
                GpioAecController.activateBeamforming()
            }
        } catch (e: Exception) { }
    }

    fun unpause() {
        try { 
            if (isPaused) {
                _player?.play()
                GpioAecController.activateAEC()
            }
        } catch (e: Exception) { }
    }

    fun stop() {
        safeClose()
    }

    
    var onDurationChanged: ((Long) -> Unit)? = null
    var onMediaMetadataChanged: ((artworkUri: String?) -> Unit)? = null
    var onPlaybackEnded: (() -> Unit)? = null
    var onPlaybackStarted: (() -> Unit)? = null
    
    private fun getPlayerListener(onCompletion: () -> Unit) = object : Player.Listener {
        private val completionCalled = AtomicBoolean(false)
        
        private fun safeComplete() {
            if (completionCalled.compareAndSet(false, true)) {
                onCompletion()
            }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                
                try {
                    onPlaybackEnded?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking onPlaybackEnded", e)
                }
                safeComplete()
                safeClose()
            } else if (playbackState == Player.STATE_READY) {
                
                try {
                    val duration = _player?.duration ?: 0L
                    
                    if (duration > 0 && duration != Long.MIN_VALUE + 1) {
                        onDurationChanged?.invoke(duration)
                    }
                    
                    val metadata = _player?.mediaMetadata
                    val artworkUri = metadata?.artworkUri?.toString()
                    onMediaMetadataChanged?.invoke(artworkUri)
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting metadata", e)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            safeComplete()
            safeClose()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying")
            try {
                if (isPlaying) {
                    _state.value = AudioPlayerState.PLAYING
                    try {
                        Log.d(TAG, "Invoking onPlaybackStarted callback")
                        onPlaybackStarted?.invoke()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error invoking onPlaybackStarted", e)
                    }
                } else if (isPaused)
                    _state.value = AudioPlayerState.PAUSED
                else
                    _state.value = AudioPlayerState.IDLE
                
                if (!isPlaying && _state.value == AudioPlayerState.IDLE) {
                    try {
                        onPlaybackEnded?.invoke()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error invoking onPlaybackEnded fallback", e)
                    }
                }
            } catch (e: Exception) { }
        }
        
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            
            try {
                val metadata = _player?.mediaMetadata
                val artworkUri = metadata?.artworkUri?.toString()
                
                onMediaMetadataChanged?.invoke(artworkUri)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting metadata on transition", e)
                
                onMediaMetadataChanged?.invoke(null)
            }
        }
    }
    
    private fun safeClose() {
        if (isClosing.compareAndSet(false, true)) {
            try {
                close()
            } finally {
                isClosing.set(false)
            }
        }
    }

    override fun close() {
        isPlayerInit = false
        val playerToRelease = _player
        _player = null
        currentListener = null
        _state.value = AudioPlayerState.IDLE
        
        if (playerToRelease != null) {
            try {
                playerToRelease.stop()
                playerToRelease.clearMediaItems()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping player", e)
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    playerToRelease.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing player", e)
                }
            }
            
            GpioAecController.activateBeamforming()
        }
    }
    
    fun destroy() {
        isClosed.set(true)
        close()
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}