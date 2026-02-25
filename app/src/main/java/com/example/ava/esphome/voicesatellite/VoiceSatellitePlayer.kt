package com.example.ava.esphome.voicesatellite

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.ava.players.AudioPlayer
import com.example.ava.players.TtsPlayer
import com.example.ava.settings.SettingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class VoiceSatellitePlayer(
    val ttsPlayer: TtsPlayer,
    val mediaPlayer: AudioPlayer,
    val wakeSoundPlayer: AudioPlayer,  
    volume: Float = 1.0f,
    muted: Boolean = false,
    val enableWakeSound: SettingState<Boolean>,
    val enableScreenOff: SettingState<Boolean>,
    val wakeSound: SettingState<String>,
    val wakeSound2: SettingState<String>,
    val timerFinishedSound: SettingState<String>,
    val stopSound: SettingState<String>,
    val enableStopSound: SettingState<Boolean>,
    val continuousPromptSound: SettingState<String>,
    val enableContinuousConversation: SettingState<Boolean>,
    private val duckMultiplier: Float = 0.5f
) : AutoCloseable {
    
    
    private val _haRemoteUrl = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val haRemoteUrl: kotlinx.coroutines.flow.Flow<String> = _haRemoteUrl
    private val haRemoteUrlScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    init {
        _haRemoteUrl.tryEmit("")
    }
    
    
    private val _notificationScene = MutableStateFlow("")
    val notificationScene = _notificationScene.asStateFlow()
    
    var onHaRemoteUrlChanged: ((String) -> Unit)? = null
    var onMediaPlay: ((String) -> Unit)? = null
    var onMediaPause: (() -> Unit)? = null
    var onMediaResume: (() -> Unit)? = null
    var onMediaStop: (() -> Unit)? = null
    var onMediaDuration: ((Long) -> Unit)? = null
        set(value) {
            field = value
            Log.d(TAG, "Setting onMediaDuration: ${value != null}")
            mediaPlayer.onDurationChanged = value
            Log.d(TAG, "mediaPlayer.onDurationChanged set to: ${mediaPlayer.onDurationChanged != null}")
        }
    var onMediaCover: ((String?) -> Unit)? = null
        set(value) {
            field = value
            Log.d(TAG, "Setting onMediaCover: ${value != null}")
            mediaPlayer.onMediaMetadataChanged = value
            Log.d(TAG, "mediaPlayer.onMediaMetadataChanged set to: ${mediaPlayer.onMediaMetadataChanged != null}")
        }
    var onHaCoverUrl: ((String) -> Unit)? = null
    var onPlaybackEnded: (() -> Unit)? = null
        set(value) {
            field = value
            Log.d(TAG, "Setting onPlaybackEnded: ${value != null}")
            mediaPlayer.onPlaybackEnded = value
            Log.d(TAG, "mediaPlayer.onPlaybackEnded set to: ${mediaPlayer.onPlaybackEnded != null}")
        }
    
    
    val currentPosition: Long get() = mediaPlayer.currentPosition
    
    
    fun seekTo(positionMs: Long) = mediaPlayer.seekTo(positionMs)

    private var _isDucked = false
    private val _volume = MutableStateFlow(volume)
    private val _muted = MutableStateFlow(muted)

    val volume get() = _volume.asStateFlow()
    fun setVolume(value: Float) {
        _volume.value = value
        if (!_muted.value) {
            ttsPlayer.volume = value
            mediaPlayer.volume = if (_isDucked) value * duckMultiplier else value
        }
    }

    val muted get() = _muted.asStateFlow()
    fun setMuted(value: Boolean) {
        _muted.value = value
        if (value) {
            mediaPlayer.volume = 0.0f
            ttsPlayer.volume = 0.0f
        } else {
            ttsPlayer.volume = _volume.value
            mediaPlayer.volume = if (_isDucked) _volume.value * duckMultiplier else _volume.value
        }
    }

    suspend fun playWakeSound(wakeWordIndex: Int = 0, onCompletion: () -> Unit = {}) {
        val enabled = enableWakeSound.get()
        val sound = if (wakeWordIndex == 1) wakeSound2.get() else wakeSound.get()
        Log.d(TAG, "playWakeSound: enabled=$enabled, wakeWordIndex=$wakeWordIndex, sound=$sound")
        if (enabled) {
            wakeSoundPlayer.play(sound, onCompletion)
        } else {
            onCompletion()
        }
    }
    
    init {
        Log.d(TAG, "VoiceSatellitePlayer[${this.hashCode()}] init, mediaPlayer[${mediaPlayer.hashCode()}]")
    }
    
    companion object {
        private const val TAG = "VoiceSatellitePlayer"
    }

    suspend fun playTimerFinishedSound(onCompletion: () -> Unit = {}) {
        ttsPlayer.playSound(timerFinishedSound.get(), onCompletion)
    }
    
    suspend fun playStopSound(onCompletion: () -> Unit = {}) {
        val enabled = enableStopSound.get()
        val sound = stopSound.get()
        Log.d(TAG, "playStopSound: enabled=$enabled, sound=$sound")
        if (enabled) {
            wakeSoundPlayer.play(sound, onCompletion)
        } else {
            onCompletion()
        }
    }
    
    suspend fun playContinuousPromptSound(onCompletion: () -> Unit = {}) {
        val sound = continuousPromptSound.get()
        Log.d(TAG, "playContinuousPromptSound: sound=$sound")
        wakeSoundPlayer.play(sound, onCompletion)
    }
    
    fun setHaRemoteUrl(url: String) {
        if (!_haRemoteUrl.tryEmit(url)) {
            haRemoteUrlScope.launch {
                _haRemoteUrl.emit(url)
            }
        }
        onHaRemoteUrlChanged?.invoke(url)
    }
    
    var onHaMediaTitle: ((String) -> Unit)? = null
    var onHaMediaArtist: ((String) -> Unit)? = null
    
    var onHaMediaPlayPause: (() -> Unit)? = null
    var onHaMediaPrevious: (() -> Unit)? = null
    var onHaMediaNext: (() -> Unit)? = null
    
    fun haMediaPlayPause() {
        onHaMediaPlayPause?.invoke()
    }
    
    fun haMediaPrevious() {
        onHaMediaPrevious?.invoke()
    }
    
    fun haMediaNext() {
        onHaMediaNext?.invoke()
    }
    
    var onHaVolumeLevel: ((Float) -> Unit)? = null
    var onHaRepeatMode: ((String) -> Unit)? = null
    var onHaShuffle: ((Boolean) -> Unit)? = null
    
    private val _haVolumeLevel = MutableStateFlow(1.0f)
    val haVolumeLevel = _haVolumeLevel.asStateFlow()
    
    private val _haRepeatMode = MutableStateFlow("off")
    val haRepeatMode = _haRepeatMode.asStateFlow()
    
    private val _haShuffle = MutableStateFlow(false)
    val haShuffle = _haShuffle.asStateFlow()
    
    fun setHaVolumeLevel(volume: Float) {
        _haVolumeLevel.value = volume
        onHaVolumeLevel?.invoke(volume)
    }
    
    fun setHaRepeatMode(mode: String) {
        _haRepeatMode.value = mode
        onHaRepeatMode?.invoke(mode)
    }
    
    fun setHaShuffle(shuffle: Boolean) {
        _haShuffle.value = shuffle
        onHaShuffle?.invoke(shuffle)
    }
    
    var onHaSetVolume: ((Float) -> Unit)? = null
    var onHaSetRepeat: ((String) -> Unit)? = null
    var onHaSetShuffle: ((Boolean) -> Unit)? = null
    
    fun haSetVolume(volume: Float) {
        onHaSetVolume?.invoke(volume)
    }
    
    fun haSetRepeat(mode: String) {
        onHaSetRepeat?.invoke(mode)
    }
    
    fun haSetShuffle(shuffle: Boolean) {
        onHaSetShuffle?.invoke(shuffle)
    }
    
    var onHaPlaybackStateWithMetadata: ((Boolean, Boolean) -> Unit)? = null
    
    private val _haPlaybackState = MutableStateFlow(false)
    val haPlaybackState = _haPlaybackState.asStateFlow()
    
    private var _haMediaTitleCache: String = ""
    val haMediaTitleCache: String get() = _haMediaTitleCache
    
    private var _haMediaArtistCache: String = ""
    val haMediaArtistCache: String get() = _haMediaArtistCache
    
    private var _haMediaCoverCache: String = ""
    val haMediaCoverCache: String get() = _haMediaCoverCache
    
    fun setHaPlaybackState(isPlaying: Boolean) {
        _haPlaybackState.value = isPlaying
        val hasMetadata = _haMediaTitleCache.isNotEmpty()
        onHaPlaybackStateWithMetadata?.invoke(isPlaying, hasMetadata)
    }
    
    fun clearHaMediaCache() {
        _haMediaTitleCache = ""
        _haMediaArtistCache = ""
        _haMediaCoverCache = ""
    }
    
    fun setHaMediaTitle(title: String) {
        _haMediaTitleCache = title
        onHaMediaTitle?.invoke(title)
        if (title.isNotEmpty() && _haPlaybackState.value) {
            onHaPlaybackStateWithMetadata?.invoke(true, true)
        }
    }
    
    fun setHaMediaArtist(artist: String) {
        _haMediaArtistCache = artist
        onHaMediaArtist?.invoke(artist)
    }
    
    fun setHaCoverUrl(coverUrl: String) {
        _haMediaCoverCache = coverUrl
        onHaCoverUrl?.invoke(coverUrl)
    }
    
    var onNotificationSceneChanged: ((String) -> Unit)? = null
    
    fun setNotificationScene(sceneId: String) {
        _notificationScene.value = sceneId
        onNotificationSceneChanged?.invoke(sceneId)
    }

    private var fadeJob: Job? = null
    
    fun duck() {
        _isDucked = true
        if (!_muted.value) {
            fadeJob?.cancel()
            fadeJob = CoroutineScope(Dispatchers.Main).launch {
                val startVolume = mediaPlayer.volume
                val targetVolume = _volume.value * duckMultiplier
                val steps = 10
                val stepDelay = 30L
                for (i in 1..steps) {
                    val progress = i.toFloat() / steps
                    mediaPlayer.volume = startVolume + (targetVolume - startVolume) * progress
                    delay(stepDelay)
                }
            }
        }
    }

    fun unDuck() {
        _isDucked = false
        if (!_muted.value) {
            fadeJob?.cancel()
            fadeJob = CoroutineScope(Dispatchers.Main).launch {
                val startVolume = mediaPlayer.volume
                val targetVolume = _volume.value
                val steps = 15
                val stepDelay = 40L
                for (i in 1..steps) {
                    val progress = i.toFloat() / steps
                    mediaPlayer.volume = startVolume + (targetVolume - startVolume) * progress
                    delay(stepDelay)
                }
            }
        }
    }

    override fun close() {
        haRemoteUrlScope.cancel()
        ttsPlayer.close()
        mediaPlayer.close()
        wakeSoundPlayer.close()
    }
}
