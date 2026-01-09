package com.example.ava.esphome.entities

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.ava.esphome.voicesatellite.VoiceSatellitePlayer
import com.example.ava.players.AudioPlayerState
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.MediaPlayerCommand
import com.example.esphomeproto.api.MediaPlayerCommandRequest
import com.example.esphomeproto.api.MediaPlayerState
import com.example.esphomeproto.api.listEntitiesMediaPlayerResponse
import com.example.esphomeproto.api.mediaPlayerStateResponse
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

@OptIn(UnstableApi::class)
class MediaPlayerEntity(
    val key: Int,
    val name: String,
    val objectId: String,
    val player: VoiceSatellitePlayer
) : Entity {

    override fun handleMessage(message: MessageLite) = flow {
        when (message) {
            is ListEntitiesRequest -> emit(listEntitiesMediaPlayerResponse {
                key = this@MediaPlayerEntity.key
                name = this@MediaPlayerEntity.name
                objectId = this@MediaPlayerEntity.objectId
                supportsPause = true
            })

            is MediaPlayerCommandRequest -> {
                if (message.key == key) {
                    if (message.hasMediaUrl) {
                        player.mediaPlayer.play(message.mediaUrl)
                        player.onMediaPlay?.invoke(message.mediaUrl)
                    } else if (message.hasCommand) {
                        when (message.command) {
                            MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PAUSE -> {
                                player.mediaPlayer.pause()
                                player.onMediaPause?.invoke()
                            }
                            MediaPlayerCommand.MEDIA_PLAYER_COMMAND_PLAY -> {
                                player.mediaPlayer.unpause()
                                player.onMediaResume?.invoke()
                            }
                            MediaPlayerCommand.MEDIA_PLAYER_COMMAND_STOP -> {
                                player.mediaPlayer.stop()
                                player.onMediaStop?.invoke()
                            }
                            MediaPlayerCommand.MEDIA_PLAYER_COMMAND_MUTE -> player.setMuted(true)
                            MediaPlayerCommand.MEDIA_PLAYER_COMMAND_UNMUTE -> player.setMuted(false)
                            else -> {}
                        }
                    } else if (message.hasVolume) {
                        player.setVolume(message.volume)
                    }
                }
            }
        }
    }

    override fun subscribe() = combine(
        player.mediaPlayer.state,
        player.volume,
        player.muted,
    ) { state, volume, muted ->
        mediaPlayerStateResponse {
            key = this@MediaPlayerEntity.key
            this.state = getState(state)
            this.volume = volume
            this.muted = muted
        }
    }

    private fun getState(state: AudioPlayerState) = when (state) {
        AudioPlayerState.PLAYING -> MediaPlayerState.MEDIA_PLAYER_STATE_PLAYING
        AudioPlayerState.PAUSED -> MediaPlayerState.MEDIA_PLAYER_STATE_PAUSED
        AudioPlayerState.IDLE -> MediaPlayerState.MEDIA_PLAYER_STATE_IDLE
    }
}