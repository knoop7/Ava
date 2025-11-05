package com.example.ava.players

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import kotlin.apply

class TtsPlayer(context: Context) : MediaPlayer, AutoCloseable {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addAnalyticsListener(EventLogger())
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed to $playbackState")
                // If there's a playback error then the player state will return to idle
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    fireAndRemoveCompletionHandler()
                }
            }
        })
    }

    private var ttsStreamUrl: String? = null
    private var _ttsPlayed: Boolean = false
    val ttsPlayed: Boolean
        get() = _ttsPlayed

    private var onCompletion: (() -> Unit)? = null

    val isPlaying get() = player.isPlaying

    override var volume
        get() = player.volume
        set(value) {
            player.volume = value
        }

    fun runStart(ttsStreamUrl: String?) {
        this.ttsStreamUrl = ttsStreamUrl
        _ttsPlayed = false
        onCompletion = null
    }

    fun runEnd() {
        // Manually fire the completion handler only
        // if tts playback was not started, else it
        // will (or was) fired when the playback ended
        if (!_ttsPlayed)
            fireAndRemoveCompletionHandler()
        _ttsPlayed = false
        ttsStreamUrl = null
    }

    fun runStopped() {
        onCompletion = null
        _ttsPlayed = false
        ttsStreamUrl = null
        player.stop()
    }

    fun streamTts(onCompletion: () -> Unit) {
        playTts(ttsStreamUrl, onCompletion)
    }

    fun playTts(ttsUrl: String?, onCompletion: () -> Unit) {
        if (!ttsUrl.isNullOrBlank()) {
            this.onCompletion = onCompletion
            _ttsPlayed = true
            play(ttsUrl, null)
        } else {
            Log.w(TAG, "TTS URL is null or blank")
        }
    }

    fun playAnnouncement(mediaUrl: String?, preannounceUrl: String?, onCompletion: () -> Unit) {
        if (!mediaUrl.isNullOrBlank()) {
            this.onCompletion = onCompletion
            play(mediaUrl, preannounceUrl)
        }
    }

    private fun play(mediaUrl: String, preannounceUrl: String?) {
        runCatching {
            player.clearMediaItems()
            if (!preannounceUrl.isNullOrBlank())
                player.addMediaItem(MediaItem.fromUri(preannounceUrl))
            player.addMediaItem(MediaItem.fromUri(mediaUrl))
            player.playWhenReady = true
            player.prepare()
        }.onFailure {
            Log.e(TAG, "Error playing media $mediaUrl", it)
        }
    }

    private fun fireAndRemoveCompletionHandler() {
        val completion = onCompletion
        onCompletion = null
        completion?.invoke()
    }

    override fun addListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }

    override fun close() {
        player.release()
    }

    companion object {
        private const val TAG = "TtsPlayer"
    }
}