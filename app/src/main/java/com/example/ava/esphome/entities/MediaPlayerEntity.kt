package com.example.ava.esphome.entities

import androidx.media3.common.Player
import com.example.ava.players.MediaPlayer
import com.example.ava.players.TtsPlayer
import com.example.ava.settings.SettingsStore
import com.example.ava.settings.VoiceSatelliteSettings
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.MediaPlayerCommand
import com.example.esphomeproto.api.MediaPlayerCommandRequest
import com.example.esphomeproto.api.MediaPlayerState
import com.example.esphomeproto.api.MediaPlayerStateResponse
import com.example.esphomeproto.api.listEntitiesMediaPlayerResponse
import com.example.esphomeproto.api.mediaPlayerStateResponse
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MediaPlayerEntity(
    val ttsPlayer: TtsPlayer,
    val mediaPlayer: MediaPlayer,
    val key: Int = KEY,
    val name: String = NAME,
    val objectId: String = OBJECT_ID,
    val settingsStore: SettingsStore<VoiceSatelliteSettings>
) : Entity, AutoCloseable {

    private val mediaPlayerState = AtomicReference(MediaPlayerState.MEDIA_PLAYER_STATE_IDLE)
    private val muted = AtomicBoolean(false)
    private val volume = AtomicReference(1.0f)
    private val isDucked = AtomicBoolean(false)

    private val _state = MutableStateFlow(getStateResponse())
    override val state = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (ttsPlayer.isStopped && mediaPlayer.isStopped)
                setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_IDLE)
        }
    }

    override suspend fun start() {
        ttsPlayer.addListener(playerListener)
        mediaPlayer.addListener(playerListener)
        val settings = settingsStore.get()
        setIsMuted(settings.muted, false)
        setVolume(settings.volume, false)
    }

    override fun handleMessage(message: GeneratedMessage) = flow {
        when (message) {
            is ListEntitiesRequest -> emit(listEntitiesMediaPlayerResponse {
                key = this@MediaPlayerEntity.key
                name = this@MediaPlayerEntity.name
                objectId = this@MediaPlayerEntity.objectId
                supportsPause = true
            })

            is MediaPlayerCommandRequest -> {
                if (message.hasMediaUrl) {
                    setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PLAYING)
                    mediaPlayer.play(message.mediaUrl)
                } else if (message.hasCommand) {
                    if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PAUSE && mediaPlayer.isPlaying) {
                        setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PAUSED)
                        mediaPlayer.pause()
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PLAY && mediaPlayer.isPaused) {
                        setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PLAYING)
                        mediaPlayer.unpause()
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_MUTE) {
                        setIsMuted(true)
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_UNMUTE) {
                        setIsMuted(false)
                    }
                } else if (message.hasVolume) {
                    setVolume(message.volume)
                }
            }
        }
    }

    fun duck() {
        if (isDucked.compareAndSet(false, true) && !muted.get()) {
            mediaPlayer.volume = volume.get() / 2
        }
    }

    fun unDuck() {
        if (isDucked.compareAndSet(true, false) && !muted.get()) {
            mediaPlayer.volume = volume.get()
        }
    }

    private fun setMediaPlayerState(state: MediaPlayerState) {
        this.mediaPlayerState.set(state)
        stateChanged()
    }

    private suspend fun setVolume(volume: Float, persist: Boolean = true) {
        this.volume.set(volume)
        if (!muted.get()) {
            ttsPlayer.volume = volume
            mediaPlayer.volume = if (isDucked.get()) volume / 2 else volume
        }
        if (persist)
            settingsStore.update { it.copy(volume = volume) }
        stateChanged()
    }

    private suspend fun setIsMuted(isMuted: Boolean, persist: Boolean = true) {
        this.muted.set(isMuted)
        if (isMuted) {
            mediaPlayer.volume = 0.0f
            ttsPlayer.volume = 0.0f
        } else {
            ttsPlayer.volume = volume.get()
            mediaPlayer.volume = if (isDucked.get()) volume.get() / 2 else volume.get()
        }
        if (persist)
            settingsStore.update { it.copy(muted = isMuted) }
        stateChanged()
    }

    private fun stateChanged() {
        _state.value = getStateResponse()
    }

    private fun getStateResponse(): MediaPlayerStateResponse {
        return mediaPlayerStateResponse {
            key = this@MediaPlayerEntity.key
            state = this@MediaPlayerEntity.mediaPlayerState.get()
            volume = this@MediaPlayerEntity.volume.get()
            muted = this@MediaPlayerEntity.muted.get()
        }
    }

    override fun close() {
        ttsPlayer.close()
        mediaPlayer.close()
    }

    companion object {
        const val TAG = "MediaPlayerEntity"
        const val KEY = 0
        const val NAME = "Media Player"
        const val OBJECT_ID = "media_player"
    }
}