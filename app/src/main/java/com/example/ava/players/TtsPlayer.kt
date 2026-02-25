package com.example.ava.players

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class TtsPlayer
    (private val player: AudioPlayer) : AutoCloseable {

    private var _ttsPlayed: Boolean = false
    val ttsPlayed: Boolean
        get() = _ttsPlayed

    private var onCompletion: (() -> Unit)? = null
    
    
    var onPlaybackEnded: (() -> Unit)? = null
        set(value) {
            field = value
            player.onPlaybackEnded = value
        }

    var onTtsDurationReady: ((Long) -> Unit)? = null
    var onTtsPlaybackStarted: (() -> Unit)? = null
    var onTtsProgressUpdate: ((currentMs: Long, totalMs: Long) -> Unit)? = null
    
    private var progressHandler: android.os.Handler? = null
    private var progressRunnable: Runnable? = null
    
    private fun startProgressTracking() {
        stopProgressTracking()
        progressHandler = android.os.Handler(android.os.Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    val current = currentPosition
                    val total = duration
                    if (total > 0) {
                        onTtsProgressUpdate?.invoke(current, total)
                    }
                    progressHandler?.postDelayed(this, 100)
                }
            }
        }
        progressHandler?.post(progressRunnable!!)
    }
    
    private fun stopProgressTracking() {
        progressRunnable?.let { progressHandler?.removeCallbacks(it) }
        progressRunnable = null
        progressHandler = null
    }

    val isPlaying get() = player.isPlaying
    val currentPosition: Long get() = player.currentPosition
    val duration: Long get() = player.duration

    var volume
        get() = player.volume
        set(value) {
            player.volume = value
        }

    fun runStart(onCompletion: () -> Unit) {
        this.onCompletion = onCompletion
        _ttsPlayed = false
        player.init()
    }

    fun runEnd() {
        if (!_ttsPlayed) {
            fireAndRemoveCompletionHandler()
        }
        _ttsPlayed = false
        stopProgressTracking()
        onTtsDurationReady = null
        onTtsPlaybackStarted = null
        onTtsProgressUpdate = null
    }

    fun markAsPlayed() {
        _ttsPlayed = true
    }
    
    fun triggerCompletion() {
        fireAndRemoveCompletionHandler()
    }
    
    fun playTts(ttsUrl: String?) {
        Log.d(TAG, "playTts called: url=$ttsUrl")
        if (!ttsUrl.isNullOrBlank()) {
            _ttsPlayed = true
            player.onDurationChanged = { durationMs ->
                Log.d(TAG, "onDurationChanged: $durationMs ms")
                onTtsDurationReady?.invoke(durationMs)
                player.onDurationChanged = null
            }
            player.onPlaybackStarted = {
                Log.d(TAG, "onPlaybackStarted triggered")
                onTtsPlaybackStarted?.invoke()
                startProgressTracking()
                player.onPlaybackStarted = null
            }
            player.play(ttsUrl) {
                stopProgressTracking()
                player.onDurationChanged = null
                player.onPlaybackStarted = null
                fireAndRemoveCompletionHandler()
            }
        } else {
            Log.w(TAG, "TTS URL is null or blank")
        }
    }

    fun playSound(soundUrl: String?, onCompletion: () -> Unit) {
        Log.d(TAG, "playSound: soundUrl=$soundUrl")
        playAnnouncement(soundUrl, null, onCompletion)
    }

    fun playAnnouncement(mediaUrl: String?, preannounceUrl: String?, onCompletion: () -> Unit) {
        Log.d(TAG, "playAnnouncement: mediaUrl=$mediaUrl")
        if (!mediaUrl.isNullOrBlank()) {
            player.play(
                if (preannounceUrl.isNullOrBlank()) listOf(mediaUrl) else listOf(
                    preannounceUrl,
                    mediaUrl
                ), onCompletion
            )
        } else {
            Log.w(TAG, "Media URL is null or blank")
            onCompletion()
        }
    }

    fun stop() {
        onCompletion = null
        _ttsPlayed = false
        player.stop()
    }

    private fun fireAndRemoveCompletionHandler() {
        val completion = onCompletion
        onCompletion = null
        completion?.invoke()
    }

    override fun close() {
        player.close()
    }

    companion object {
        private const val TAG = "TtsPlayer"
    }
}