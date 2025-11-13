package com.example.ava.esphome.VoiceSatellite

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.ava.esphome.EspHomeDevice
import com.example.ava.esphome.entities.Entity
import com.example.ava.esphome.entities.MediaPlayerEntity
import com.example.ava.microwakeword.WakeWordProvider
import com.example.ava.players.TtsPlayer
import com.example.ava.preferences.VoiceSatelliteSettings
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
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

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
        activeStopWords = listOf(settings.stopWord)
    }

    private var continueConversation = false
    private var timerFinished = false

    private val _satelliteState =
        MutableStateFlow<VoiceSatelliteState>(VoiceSatelliteState.Disconnected())
    val satelliteState = _satelliteState.asStateFlow()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        super.start()
        startConnectedChangedListener()
        startAudioInput()
    }

    private fun startConnectedChangedListener() = server.isConnected
        .onEach { isConnected ->
            if (isConnected) {
                _satelliteState.value = VoiceSatelliteState.Idle()
            } else {
                onSatelliteDisconnected()
            }
        }
        .launchIn(scope)

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

    private fun onSatelliteDisconnected() {
        audioInput.isStreaming = false
        continueConversation = false
        timerFinished = false
        ttsPlayer.stop()
        _satelliteState.value = VoiceSatelliteState.Disconnected()
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
                    mediaUrl = message.mediaId,
                    preannounceUrl = message.preannounceMediaId
                ) {
                    scope.launch { onTtsFinished() }
                }
            }

            is VoiceAssistantEventResponse -> handleVoiceAssistantMessage(message)

            is VoiceAssistantTimerEventResponse -> handleTimerMessage(message)

            else -> super.handleMessage(message)
        }
    }

    private fun handleTimerMessage(timerEvent: VoiceAssistantTimerEventResponse) {
        Log.d(TAG, "Timer event: ${timerEvent.eventType}")
        when (timerEvent.eventType) {
            VoiceAssistantTimerEvent.VOICE_ASSISTANT_TIMER_FINISHED -> {
                if (!timerFinished) {
                    timerFinished = true
                    ttsPlayer.playSound(settings.timerFinishedSound) {
                        scope.launch { onTimerFinished() }
                    }
                }
            }

            else -> {}
        }
    }

    private fun handleVoiceAssistantMessage(voiceEvent: VoiceAssistantEventResponse) {
        when (voiceEvent.eventType) {
            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_START -> {
                val ttsUrl = voiceEvent.dataList.firstOrNull { data -> data.name == "url" }?.value
                ttsPlayer.runStart(ttsStreamUrl = ttsUrl) {
                    scope.launch { onTtsFinished() }
                }
                audioInput.isStreaming = true
                continueConversation = false
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_END, VoiceAssistantEvent.VOICE_ASSISTANT_STT_END -> {
                audioInput.isStreaming = false
                _satelliteState.value = VoiceSatelliteState.Processing()
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_PROGRESS -> {
                if (voiceEvent.dataList.firstOrNull { data -> data.name == "tts_start_streaming" }?.value == "1") {
                    ttsPlayer.streamTts()
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
                    ttsPlayer.playTts(ttsUrl)
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

    private suspend fun handleAudioResult(audioResult: VoiceSatelliteAudioInput.AudioResult){
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
        } else if (_satelliteState.value !is VoiceSatelliteState.Listening) {
            wakeSatellite(wakeWordPhrase)
        }
    }

    private suspend fun onStopDetected() {
        if (timerFinished) {
            stopTimer()
        } else if (ttsPlayer.isPlaying) {
            stopSatellite()
        }
    }

    private suspend fun wakeSatellite(
        wakeWordPhrase: String = "",
        isContinueConversation: Boolean = false
    ) {
        Log.d(TAG, "Wake satellite")
        _satelliteState.value = VoiceSatelliteState.Listening()
        if (!isContinueConversation && settings.playWakeSound) {
            // Start streaming audio only after the wake sound has finished
            ttsPlayer.playSound(settings.wakeSound) {
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
        ttsPlayer.stop()
        sendMessage(voiceAssistantAnnounceFinished { })
    }

    private fun stopTimer() {
        Log.d(TAG, "Stop timer")
        if (timerFinished) {
            timerFinished = false
            ttsPlayer.stop()
        }
    }

    private suspend fun onTtsFinished() {
        Log.d(TAG, "TTS finished")
        sendMessage(voiceAssistantAnnounceFinished { })
        if (continueConversation) {
            Log.d(TAG, "Continuing conversation")
            wakeSatellite(isContinueConversation = true)
        } else {
            _satelliteState.value = VoiceSatelliteState.Idle()
        }
    }

    private suspend fun onTimerFinished() {
        if (timerFinished) {
            delay(1000)
            if (timerFinished) {
                ttsPlayer.playSound(settings.timerFinishedSound) {
                    scope.launch { onTimerFinished() }
                }
            }
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