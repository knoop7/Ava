package com.example.ava.esphome.entities

import androidx.media3.common.Player
import com.example.ava.players.MediaPlayer
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.MediaPlayerCommand
import com.example.esphomeproto.api.MediaPlayerCommandRequest
import com.example.esphomeproto.api.MediaPlayerState
import com.example.esphomeproto.api.MediaPlayerStateResponse
import com.example.esphomeproto.api.SubscribeHomeAssistantStatesRequest
import com.example.esphomeproto.api.listEntitiesMediaPlayerResponse
import com.example.esphomeproto.api.mediaPlayerStateResponse
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MediaPlayerEntity(
    val ttsPlayer: MediaPlayer,
    val key: Int = KEY,
    val name: String = NAME,
    val objectId: String = OBJECT_ID,
) : Entity {

    private val mediaPlayerState = AtomicReference(MediaPlayerState.MEDIA_PLAYER_STATE_IDLE)
    private val muted = AtomicBoolean(false)
    private val volume = AtomicReference(1.0f)

    private val _state = MutableSharedFlow<GeneratedMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val state = _state.asSharedFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED)
                setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_IDLE)
        }
    }

    init {
        ttsPlayer.addListener(playerListener)
        ttsPlayer.volume = volume.get()
    }

    override suspend fun handleMessage(message: GeneratedMessage) = sequence {
        when (message) {
            is ListEntitiesRequest -> yield(listEntitiesMediaPlayerResponse {
                key = this@MediaPlayerEntity.key
                name = this@MediaPlayerEntity.name
                objectId = this@MediaPlayerEntity.objectId
                supportsPause = true
            })

            is MediaPlayerCommandRequest -> {
                if (message.hasMediaUrl) {
                    setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PLAYING)
                } else if (message.hasCommand) {
                    if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PAUSE) {
                        setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PAUSED)
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PLAY) {
                        setMediaPlayerState(MediaPlayerState.MEDIA_PLAYER_STATE_PLAYING)
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_MUTE) {
                        setIsMuted(true)
                    } else if (message.command == MediaPlayerCommand.MEDIA_PLAYER_COMMAND_UNMUTE) {
                        setIsMuted(false)
                    }
                } else if (message.hasVolume) {
                    setVolume(message.volume)
                }
            }

            is SubscribeHomeAssistantStatesRequest -> {
                yield(getStateResponse())
            }
        }
    }

    private fun setMediaPlayerState(state: MediaPlayerState) {
        this.mediaPlayerState.set(state)
        stateChanged()
    }

    private fun setVolume(volume: Float) {
        this.volume.set(volume)
        if (!muted.get())
            ttsPlayer.volume = volume
        stateChanged()
    }

    private fun setIsMuted(isMuted: Boolean) {
        this.muted.set(isMuted)
        ttsPlayer.volume = if (isMuted) 0.0f else this.volume.get()
        stateChanged()
    }

    private fun stateChanged() {
        _state.tryEmit(getStateResponse())
    }

    private fun getStateResponse(): MediaPlayerStateResponse {
        return mediaPlayerStateResponse {
            key = this@MediaPlayerEntity.key
            state = this@MediaPlayerEntity.mediaPlayerState.get()
            volume = this@MediaPlayerEntity.volume.get()
            muted = this@MediaPlayerEntity.muted.get()
        }
    }

    companion object {
        const val TAG = "MediaPlayerEntity"
        const val KEY = 0
        const val NAME = "Media Player"
        const val OBJECT_ID = "media_player"
    }
}