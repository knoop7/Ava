package com.example.ava.esphome.voicesatellite

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.ava.esphome.Connected
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.EspHomeState
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.esphome.entities.SwitchEntity
import com.example.ava.settings.VoiceSatelliteSettingsStore
import com.example.esphomeproto.api.DeviceInfoResponse
import com.example.esphomeproto.api.VoiceAssistantAnnounceRequest
import com.example.esphomeproto.api.VoiceAssistantConfigurationRequest
import com.example.esphomeproto.api.VoiceAssistantEvent
import com.example.esphomeproto.api.VoiceAssistantEventResponse
import com.example.esphomeproto.api.VoiceAssistantFeature
import com.example.esphomeproto.api.VoiceAssistantSetConfiguration
import com.example.esphomeproto.api.VoiceAssistantTimerEvent
import com.example.esphomeproto.api.VoiceAssistantTimerEventResponse
import com.example.esphomeproto.api.deviceInfoResponse
import com.example.esphomeproto.api.voiceAssistantAnnounceFinished
import com.example.esphomeproto.api.voiceAssistantAudio
import com.example.esphomeproto.api.voiceAssistantConfigurationResponse
import com.example.esphomeproto.api.voiceAssistantRequest
import com.example.esphomeproto.api.voiceAssistantWakeWord
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data object Listening : EspHomeState
data object Responding : EspHomeState
data object Processing : EspHomeState

class VoiceSatellite(
    coroutineContext: CoroutineContext,
    name: String,
    port: Int,
    val audioInput: VoiceSatelliteAudioInput,
    val player: VoiceSatellitePlayer,
    val settingsStore: VoiceSatelliteSettingsStore
) : EspHomeDevice(
    coroutineContext,
    name,
    port,
    listOf(
        MediaPlayerEntity(0, "Media Player", "media_player", player),
        SwitchEntity(
            1,
            "Play Wake Sound",
            "play_wake_sound",
            player.enableWakeSound
        ) { player.enableWakeSound.set(it) }
    )
) {
    private var continueConversation = false
    private var timerFinished = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        super.start()
        startAudioInput()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioInput() = server.isConnected
        .flatMapLatest { isConnected ->
            if (isConnected) audioInput.start() else emptyFlow()
        }
        .flowOn(Dispatchers.IO)
        .onEach {
            handleAudioResult(audioResult = it)
        }
        .launchIn(scope)

    override suspend fun onDisconnected() {
        super.onDisconnected()
        audioInput.isStreaming = false
        continueConversation = false
        timerFinished = false
        player.ttsPlayer.stop()
    }

    override suspend fun getDeviceInfo(): DeviceInfoResponse = deviceInfoResponse {
        val settings = settingsStore.get()
        name = settings.name
        usesPassword = false
        macAddress = settings.macAddress
        voiceAssistantFeatureFlags = VoiceAssistantFeature.VOICE_ASSISTANT.flag or
                VoiceAssistantFeature.API_AUDIO.flag or
                VoiceAssistantFeature.TIMERS.flag or
                VoiceAssistantFeature.ANNOUNCE.flag or
                VoiceAssistantFeature.START_CONVERSATION.flag
    }

    override suspend fun handleMessage(message: MessageLite) {
        when (message) {
            is VoiceAssistantConfigurationRequest -> sendMessage(
                voiceAssistantConfigurationResponse {
                    availableWakeWords += audioInput.availableWakeWords.map {
                        voiceAssistantWakeWord {
                            id = it.id
                            wakeWord = it.wakeWord.wake_word
                            trainedLanguages += it.wakeWord.trained_languages.toList()
                        }
                    }
                    activeWakeWords += audioInput.activeWakeWords.value
                    maxActiveWakeWords = 1
                })

            is VoiceAssistantSetConfiguration -> {
                val activeWakeWords =
                    message.activeWakeWordsList.filter { audioInput.availableWakeWords.any { wakeWord -> wakeWord.id == it } }
                Log.d(TAG, "Setting active wake words: $activeWakeWords")
                if (activeWakeWords.isNotEmpty()) {
                    audioInput.setActiveWakeWords(activeWakeWords)
                }
                val ignoredWakeWords =
                    message.activeWakeWordsList.filter { !activeWakeWords.contains(it) }
                if (ignoredWakeWords.isNotEmpty())
                    Log.w(TAG, "Ignoring wake words: $ignoredWakeWords")
            }

            is VoiceAssistantAnnounceRequest -> handleAnnouncement(
                startConversation = message.startConversation,
                mediaId = message.mediaId,
                preannounceId = message.preannounceMediaId
            )

            is VoiceAssistantEventResponse -> handleVoiceAssistantMessage(message)

            is VoiceAssistantTimerEventResponse -> handleTimerMessage(message)

            else -> super.handleMessage(message)
        }
    }

    private suspend fun handleTimerMessage(timerEvent: VoiceAssistantTimerEventResponse) {
        Log.d(TAG, "Timer event: ${timerEvent.eventType}")
        when (timerEvent.eventType) {
            VoiceAssistantTimerEvent.VOICE_ASSISTANT_TIMER_FINISHED -> {
                if (!timerFinished) {
                    timerFinished = true
                    player.duck()
                    player.playTimerFinishedSound {
                        scope.launch { onTimerFinished() }
                    }
                }
            }

            else -> {}
        }
    }

    private fun handleAnnouncement(
        startConversation: Boolean,
        mediaId: String,
        preannounceId: String
    ) {
        continueConversation = startConversation
        _state.value = Responding
        player.duck()
        player.ttsPlayer.playAnnouncement(
            mediaUrl = mediaId,
            preannounceUrl = preannounceId
        ) {
            scope.launch { onTtsFinished() }
        }
    }

    private fun handleVoiceAssistantMessage(voiceEvent: VoiceAssistantEventResponse) {
        when (voiceEvent.eventType) {
            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_START -> {
                val ttsUrl = voiceEvent.dataList.firstOrNull { data -> data.name == "url" }?.value
                player.ttsPlayer.runStart(ttsStreamUrl = ttsUrl) {
                    scope.launch { onTtsFinished() }
                }
                audioInput.isStreaming = true
                continueConversation = false
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_END, VoiceAssistantEvent.VOICE_ASSISTANT_STT_END -> {
                audioInput.isStreaming = false
                _state.value = Processing
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_PROGRESS -> {
                if (voiceEvent.dataList.firstOrNull { data -> data.name == "tts_start_streaming" }?.value == "1") {
                    player.ttsPlayer.streamTts()
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_END -> {
                if (voiceEvent.dataList.firstOrNull { data -> data.name == "continue_conversation" }?.value == "1") {
                    continueConversation = true
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_START -> {
                _state.value = Responding
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_END -> {
                if (!player.ttsPlayer.ttsPlayed) {
                    val ttsUrl =
                        voiceEvent.dataList.firstOrNull { data -> data.name == "url" }?.value
                    player.ttsPlayer.playTts(ttsUrl)
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_END -> {
                audioInput.isStreaming = false
                player.ttsPlayer.runEnd()
            }

            else -> {
                Log.d(TAG, "Unhandled voice assistant event: ${voiceEvent.eventType}")
            }
        }
    }

    private suspend fun handleAudioResult(audioResult: VoiceSatelliteAudioInput.AudioResult) {
        when (audioResult) {
            is VoiceSatelliteAudioInput.AudioResult.Audio ->
                sendMessage(voiceAssistantAudio { data = audioResult.audio })

            is VoiceSatelliteAudioInput.AudioResult.WakeDetected ->
                onWakeDetected(audioResult.wakeWord)

            is VoiceSatelliteAudioInput.AudioResult.StopDetected ->
                onStopDetected()
        }
    }

    private suspend fun onWakeDetected(wakeWordPhrase: String) {
        if (timerFinished) {
            stopTimer()
        } else if (_state.value !is Listening) {
            wakeSatellite(wakeWordPhrase)
        }
    }

    private suspend fun onStopDetected() {
        if (timerFinished) {
            stopTimer()
        } else if (player.ttsPlayer.isPlaying) {
            stopSatellite()
        }
    }

    private suspend fun wakeSatellite(
        wakeWordPhrase: String = "",
        isContinueConversation: Boolean = false
    ) {
        Log.d(TAG, "Wake satellite")
        _state.value = Listening
        player.duck()
        if (!isContinueConversation) {
            // Start streaming audio only after the wake sound has finished
            player.playWakeSound {
                scope.launch { sendVoiceAssistantStartRequest(wakeWordPhrase) }
            }
        } else {
            sendVoiceAssistantStartRequest(wakeWordPhrase)
        }
    }

    private suspend fun sendVoiceAssistantStartRequest(wakeWordPhrase: String = "") {
        sendMessage(
            voiceAssistantRequest
            {
                start = true
                this.wakeWordPhrase = wakeWordPhrase
            })
    }

    private suspend fun stopSatellite() {
        Log.d(TAG, "Stop satellite")
        audioInput.isStreaming = false
        continueConversation = false
        player.ttsPlayer.stop()
        _state.value = Connected
        sendMessage(voiceAssistantAnnounceFinished { })
    }

    private fun stopTimer() {
        Log.d(TAG, "Stop timer")
        if (timerFinished) {
            timerFinished = false
            player.ttsPlayer.stop()
        }
    }

    private suspend fun onTtsFinished() {
        Log.d(TAG, "TTS finished")
        sendMessage(voiceAssistantAnnounceFinished { })
        if (continueConversation) {
            Log.d(TAG, "Continuing conversation")
            wakeSatellite(isContinueConversation = true)
        } else {
            player.unDuck()
            _state.value = Connected
        }
    }

    private suspend fun onTimerFinished() {
        delay(1000)
        if (timerFinished) {
            player.playTimerFinishedSound {
                scope.launch { onTimerFinished() }
            }
        } else {
            player.unDuck()
        }
    }

    override fun close() {
        super.close()
        player.close()
    }

    companion object {
        private const val TAG = "VoiceSatellite"
    }
}