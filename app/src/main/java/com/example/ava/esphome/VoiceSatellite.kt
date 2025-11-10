package com.example.ava.esphome

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.ava.esphome.entities.Entity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.microwakeword.WakeWordProvider
import com.example.ava.players.TtsPlayer
import com.example.ava.preferences.VoiceSatelliteSettings
import com.example.esphomeproto.VoiceAssistantAnnounceRequest
import com.example.esphomeproto.VoiceAssistantConfigurationRequest
import com.example.esphomeproto.VoiceAssistantEvent
import com.example.esphomeproto.VoiceAssistantEventResponse
import com.example.esphomeproto.VoiceAssistantFeature
import com.example.esphomeproto.VoiceAssistantSetConfiguration
import com.example.esphomeproto.deviceInfoResponse
import com.example.esphomeproto.voiceAssistantAnnounceFinished
import com.example.esphomeproto.voiceAssistantAudio
import com.example.esphomeproto.voiceAssistantConfigurationResponse
import com.example.esphomeproto.voiceAssistantRequest
import com.example.esphomeproto.voiceAssistantWakeWord
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

sealed class VoiceSatelliteState {
    class Stopped() : VoiceSatelliteState()
    class Disconnected() : VoiceSatelliteState()
    class Idle() : VoiceSatelliteState()
    class Listening() : VoiceSatelliteState()
    class Processing() : VoiceSatelliteState()
    class Responding() : VoiceSatelliteState()
}

class VoiceSatellite(
    coroutineContext: CoroutineContext,
    val settings: VoiceSatelliteSettings,
    wakeWordProvider: WakeWordProvider,
    stopWordProvider: WakeWordProvider,
    val ttsPlayer: TtsPlayer
) : EspHomeDevice(
    coroutineContext,
    settings.name,
    settings.serverPort,
    createDeviceInfo(settings),
    listOf<Entity>(MediaPlayerEntity(ttsPlayer))
) {
    private val audioInput = VoiceSatelliteAudioInput(wakeWordProvider, stopWordProvider).apply {
        activeWakeWords = listOf(settings.wakeWord)
        activeStopWords = stopWordProvider.getWakeWords()
            .take(1)
            .map { it.id }
    }

    private var continueConversation = false

    private val _satelliteState = MutableStateFlow<VoiceSatelliteState>(VoiceSatelliteState.Disconnected())
    val satelliteState = combine(_satelliteState, server.isConnected) { state, isConnected ->
        if (!isConnected) {
            VoiceSatelliteState.Disconnected()
        } else {
            state
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        super.start()
        startAudioInput()
    }

    override suspend fun onConnected() {
        super.onConnected()
        _satelliteState.value = VoiceSatelliteState.Idle()
        audioInput.isStreaming = false
        ttsPlayer.runStopped()
    }

    override suspend fun handleMessage(message: GeneratedMessage) {
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
                    activeWakeWords += audioInput.activeWakeWords
                    maxActiveWakeWords = 1
                })

            is VoiceAssistantSetConfiguration -> {
                val activeWakeWords =
                    message.activeWakeWordsList.filter { audioInput.availableWakeWords.any { wakeWord -> wakeWord.id == it } }
                Log.d(TAG, "Setting active wake words: $activeWakeWords")
                audioInput.activeWakeWords = activeWakeWords
                val ignoredWakeWords =
                    message.activeWakeWordsList.filter { !activeWakeWords.contains(it) }
                if (ignoredWakeWords.isNotEmpty())
                    Log.w(TAG, "Ignoring wake words: $ignoredWakeWords")
            }

            is VoiceAssistantAnnounceRequest -> {
                continueConversation = message.startConversation
                _satelliteState.value = VoiceSatelliteState.Responding()
                ttsPlayer.playAnnouncement(
                    message.mediaId,
                    message.preannounceMediaId,
                    ::ttsFinishedCallback
                )
            }

            is VoiceAssistantEventResponse -> handleVoiceAssistantMessage(message)

            else -> super.handleMessage(message)
        }
    }

    private fun handleVoiceAssistantMessage(voiceEvent: VoiceAssistantEventResponse) {
        when (voiceEvent.eventType) {
            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_START -> {
                val ttsUrl = voiceEvent.dataList.firstOrNull { data -> data.name == "url" }?.value
                ttsPlayer.runStart(ttsUrl)
                audioInput.isStreaming = true
                continueConversation = false
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_END, VoiceAssistantEvent.VOICE_ASSISTANT_STT_END -> {
                audioInput.isStreaming = false
                _satelliteState.value = VoiceSatelliteState.Processing()
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_PROGRESS -> {
                if (voiceEvent.dataList.firstOrNull { data -> data.name == "tts_start_streaming" }?.value == "1") {
                    ttsPlayer.streamTts(::ttsFinishedCallback)
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_END -> {
                if (voiceEvent.dataList.firstOrNull { data -> data.name == "continue_conversation" }?.value == "1") {
                    continueConversation = true
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_START -> {
                _satelliteState.value = VoiceSatelliteState.Responding()
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_END -> {
                if (!ttsPlayer.ttsPlayed) {
                    val ttsUrl =
                        voiceEvent.dataList.firstOrNull { data -> data.name == "url" }?.value
                    ttsPlayer.playTts(ttsUrl, ::ttsFinishedCallback)
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_END -> {
                audioInput.isStreaming = false
                ttsPlayer.runEnd()
            }

            else -> {
                Log.d(TAG, "Unhandled voice assistant event: ${voiceEvent.eventType}")
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioInput() = server.isConnected
            .flatMapLatest { isConnected ->
                if (isConnected) {
                    audioInput.start()
                } else {
                    emptyFlow()
                }
            }
            .flowOn(Dispatchers.IO)
            .onEach {
                when (it) {
                    is VoiceSatelliteAudioInput.AudioResult.Audio -> sendMessage(voiceAssistantAudio { data = it.audio })
                    is VoiceSatelliteAudioInput.AudioResult.WakeDetected -> if (!audioInput.isStreaming) wakeSatellite(it.wakeWord)
                    is VoiceSatelliteAudioInput.AudioResult.StopDetected -> if (ttsPlayer.isPlaying) stopSatellite()
                }
            }
            .launchIn(scope)

    private suspend fun wakeSatellite(wakeWordPhrase: String = "") {
        Log.d(TAG, "Wake satellite")
        audioInput.isStreaming = true
        _satelliteState.value = VoiceSatelliteState.Listening()
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
        ttsPlayer.runStopped()
        sendMessage(voiceAssistantAnnounceFinished { })
    }

    private fun ttsFinishedCallback() {
        scope.launch { ttsFinished() }
    }

    private suspend fun ttsFinished() {
        Log.d(TAG, "TTS finished")
        sendMessage(voiceAssistantAnnounceFinished { })
        if (continueConversation) {
            Log.d(TAG, "Continuing conversation")
            wakeSatellite()
        } else {
            _satelliteState.value = VoiceSatelliteState.Idle()
        }
    }

    override fun close() {
        super.close()
        ttsPlayer.close()
    }

    companion object {
        private const val TAG = "VoiceSatellite"
        private fun createDeviceInfo(settings: VoiceSatelliteSettings) = deviceInfoResponse {
            name = settings.name
            usesPassword = false
            macAddress = settings.macAddress
            voiceAssistantFeatureFlags = VoiceAssistantFeature.VOICE_ASSISTANT.flag or
                    VoiceAssistantFeature.API_AUDIO.flag or
                    VoiceAssistantFeature.TIMERS.flag or
                    VoiceAssistantFeature.ANNOUNCE.flag or
                    VoiceAssistantFeature.START_CONVERSATION.flag
        }
    }
}