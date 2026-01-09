package com.example.ava.players

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class TtsPlayer
    (private val player: AudioPlayer) : AutoCloseable {

    private var ttsStreamUrl: String? = null
    private var _ttsPlayed: Boolean = false
    val ttsPlayed: Boolean
        get() = _ttsPlayed

    private var onCompletion: (() -> Unit)? = null
    
    
    var onPlaybackEnded: (() -> Unit)? = null
        set(value) {
            field = value
            player.onPlaybackEnded = value
        }

    val isPlaying get() = player.isPlaying

    var volume
        get() = player.volume
        set(value) {
            player.volume = value
        }

    fun runStart(ttsStreamUrl: String?, onCompletion: () -> Unit) {
        this.ttsStreamUrl = ttsStreamUrl
        this.onCompletion = onCompletion
        _ttsPlayed = false
        
        
        player.init()
    }

    fun runEnd() {
        
        
        
        if (!_ttsPlayed) {
            fireAndRemoveCompletionHandler()
        }
        _ttsPlayed = false
        ttsStreamUrl = null
    }

    fun streamTts() {
        if (ttsStreamUrl != null)
            playTts(ttsStreamUrl)
    }

    fun playTts(ttsUrl: String?) {
        if (!ttsUrl.isNullOrBlank()) {
            _ttsPlayed = true
            player.play(ttsUrl) {
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
        ttsStreamUrl = null
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