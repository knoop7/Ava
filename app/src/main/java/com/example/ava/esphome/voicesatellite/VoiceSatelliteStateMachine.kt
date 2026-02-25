package com.example.ava.esphome.voicesatellite

import android.util.Log
import com.example.ava.audio.SilenceDetector
import com.example.ava.esphome.Connected
import com.example.ava.esphome.EspHomeState
import com.example.ava.utils.LightKeywordDetector
import com.example.esphomeproto.api.VoiceAssistantEvent
import com.example.esphomeproto.api.VoiceAssistantEventResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VoiceSatelliteStateMachine(
    private val scope: CoroutineScope,
    private val audioInput: VoiceSatelliteAudioInput,
    private val player: VoiceSatellitePlayer,
    private val state: MutableStateFlow<EspHomeState>,
    private val onStopSatellite: suspend () -> Unit,
    private val onTtsFinished: suspend () -> Unit,
    private val onConversationText: ((String, String) -> Unit)?,
    private val onProcessingStarted: (() -> Unit)? = null,
    private val onStreamingDelta: ((String) -> Unit)? = null,
    private val onStreamingFinished: (() -> Unit)? = null,
    private val onDeviceAction: ((LightKeywordDetector.DeviceAction) -> Unit)? = null,
    private val onSendAudioEnd: (suspend () -> Unit)? = null,
    private val onSttText: ((String) -> Unit)? = null,
    private val onTtsText: ((String) -> Unit)? = null,
    private val onPipelineError: ((code: String, message: String) -> Unit)? = null,
    private val onTtsDurationReady: ((durationMs: Long, text: String) -> Unit)? = null,
    private val onTtsPlaybackStarted: ((text: String) -> Unit)? = null,
    private val onTtsProgressUpdate: ((currentMs: Long, totalMs: Long, text: String) -> Unit)? = null
) {
    private var currentTtsText: String = ""
    private var pendingTtsDuration: Long = 0L
    private var wakeTimeoutJob: Job? = null
    private var silenceTimeoutJob: Job? = null
    private val silenceDetector = SilenceDetector()
    var isWaking = false
        private set
    var isWakePhase = false
        private set
    var continueConversation = true
    private var stopWordProtectionEndTime = 0L
    private var haVadStarted = false
    private var stopRequested = false
    private var streamingReceived = false
    var intentEnded = false
        private set

    companion object {
        private const val TAG = "VoiceSatelliteStateMachine"
    }

    fun handleVoiceEvent(voiceEvent: VoiceAssistantEventResponse) {
        Log.d(TAG, "Voice event: ${voiceEvent.eventType}, state: ${state.value}")
        when (voiceEvent.eventType) {
            VoiceAssistantEvent.VOICE_ASSISTANT_ERROR -> {
                val code = voiceEvent.dataList.firstOrNull { it.name == "code" }?.value ?: "unknown"
                val message = voiceEvent.dataList.firstOrNull { it.name == "message" }?.value ?: ""
                Log.e(TAG, "PIPELINE_ERROR: code=$code, message=$message, state=${state.value}")
                onPipelineError?.invoke(code, message)
                audioInput.isStreaming = false
                scope.launch { onStopSatellite() }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_START -> {
                Log.d(TAG, "RUN_START received, stopRequested=$stopRequested")
                
                if (stopRequested) {
                    Log.d(TAG, "RUN_START ignored, stop was requested")
                    return
                }
                
                wakeTimeoutJob?.cancel()
                wakeTimeoutJob = null
                silenceTimeoutJob?.cancel()
                silenceTimeoutJob = null
                silenceDetector.reset()
                isWaking = false
                haVadStarted = false
                streamingReceived = false
                intentEnded = false
                stopWordProtectionEndTime = System.currentTimeMillis() + 2000
                
                if (state.value == Connected) {
                    state.value = Listening
                }
                
                player.ttsPlayer.runStart {
                    scope.launch { onTtsFinished() }
                }
                audioInput.isStreaming = true
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_START -> {
                Log.d(TAG, "STT_VAD_START received, user started speaking")
                haVadStarted = true
                silenceDetector.reset()
                silenceTimeoutJob?.cancel()
                silenceTimeoutJob = null
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_VAD_END -> {
                Log.d(TAG, "STT_VAD_END received, switching to Processing")
                wakeTimeoutJob?.cancel()
                wakeTimeoutJob = null
                audioInput.isStreaming = false
                state.value = Processing
                onProcessingStarted?.invoke()
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_STT_END -> {
                val sttText = voiceEvent.dataList.firstOrNull { it.name == "text" }?.value
                Log.d(TAG, "STT_END received, text: $sttText")
                wakeTimeoutJob?.cancel()
                wakeTimeoutJob = null
                audioInput.isStreaming = false
                
                if (isSTTError(sttText)) {
                    Log.w(TAG, "STT returned error, stopping session: $sttText")
                    scope.launch { onStopSatellite() }
                    return
                }
                
                if (!sttText.isNullOrBlank()) {
                    onSttText?.invoke(sttText)
                }
                
                if (state.value == Listening) {
                    state.value = Processing
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_PROGRESS -> {
                val delta = voiceEvent.dataList.firstOrNull { it.name == "content" }?.value
                if (!delta.isNullOrBlank()) {
                    streamingReceived = true
                    onStreamingDelta?.invoke(delta)
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_INTENT_END -> {
                intentEnded = true
                if (streamingReceived) {
                    onStreamingFinished?.invoke()
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_START -> {
                Log.d(TAG, "TTS_START received, stopping audio input")
                audioInput.isStreaming = false
                silenceTimeoutJob?.cancel()
                silenceTimeoutJob = null
                
                val ttsText = voiceEvent.dataList.firstOrNull { it.name == "text" }?.value
                
                if (isTTSAboutError(ttsText)) {
                    Log.w(TAG, "TTS is about error, stopping session")
                    scope.launch { onStopSatellite() }
                    return
                }
                
                state.value = Responding
                if (!ttsText.isNullOrBlank()) {
                    currentTtsText = ttsText
                    onTtsText?.invoke(ttsText)
                    if (!streamingReceived) {
                        onConversationText?.invoke("assistant", ttsText)
                    }
                    LightKeywordDetector.detectDeviceAction(ttsText)?.let { action ->
                        onDeviceAction?.invoke(action)
                    }
                    
                    if (LightKeywordDetector.isExitKeyword(ttsText)) {
                        continueConversation = false
                    }
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_TTS_END -> {
                val ttsUrl = voiceEvent.dataList.firstOrNull { it.name == "url" }?.value
                Log.d(TAG, "TTS_END received, ttsUrl=$ttsUrl, ttsPlayed=${player.ttsPlayer.ttsPlayed}, state=${state.value}")
                if (state.value == Responding && !player.ttsPlayer.ttsPlayed) {
                    player.ttsPlayer.markAsPlayed()
                    Log.d(TAG, "TTS_END: playing url=$ttsUrl, text='${currentTtsText.take(20)}...'")
                    if (!ttsUrl.isNullOrBlank()) {
                        setupTtsCallbacks()
                        player.ttsPlayer.playTts(ttsUrl)
                    } else {
                        Log.d(TAG, "TTS_END: No URL, triggering completion")
                        player.ttsPlayer.triggerCompletion()
                    }
                } else {
                    Log.d(TAG, "TTS_END: skipped, state=${state.value}, ttsPlayed=${player.ttsPlayer.ttsPlayed}")
                }
            }

            VoiceAssistantEvent.VOICE_ASSISTANT_RUN_END -> {
                val wasTtsPlayed = player.ttsPlayer.ttsPlayed
                Log.d(TAG, "RUN_END received, current state: ${state.value}, isWaking: $isWaking, ttsPlayed: $wasTtsPlayed")
                
                if (isWaking) {
                    Log.d(TAG, "RUN_END: ignoring, new wake in progress")
                    return
                }
                
                audioInput.isStreaming = false
                
                when (state.value) {
                    is Listening, is Processing -> {
                        Log.d(TAG, "RUN_END: resetting stuck state (Listening/Processing)")
                        scope.launch { onStopSatellite() }
                    }
                    is Responding -> {
                        if (!wasTtsPlayed) {
                            Log.d(TAG, "RUN_END: Responding but no TTS played, resetting state")
                            scope.launch { onStopSatellite() }
                        } else {
                            Log.d(TAG, "RUN_END: Responding with TTS, waiting for playback completion")
                        }
                    }
                    else -> {
                        Log.d(TAG, "RUN_END: state is ${state.value}, no action needed")
                    }
                }
            }

            else -> {}
        }
    }

    fun processAudioEnergy(audioBytes: ByteArray) {
    }

    fun cancelWakeTimeout() {
        wakeTimeoutJob?.cancel()
        wakeTimeoutJob = null
    }

    fun setWaking(value: Boolean) {
        isWaking = value
        if (value) {
            silenceTimeoutJob?.cancel()
            silenceTimeoutJob = null
            silenceDetector.reset()
        }
    }

    fun setWakePhase(value: Boolean) {
        isWakePhase = value
    }

    fun reset() {
        wakeTimeoutJob?.cancel()
        wakeTimeoutJob = null
        silenceTimeoutJob?.cancel()
        silenceTimeoutJob = null
        silenceDetector.reset()
        isWaking = false
        isWakePhase = false
        continueConversation = true
        stopWordProtectionEndTime = 0L
        haVadStarted = false
        stopRequested = false
        streamingReceived = false
        intentEnded = false
        currentTtsText = ""
        pendingTtsDuration = 0L
    }
    
    fun setStopRequested(value: Boolean) {
        stopRequested = value
    }
    
    fun isStopWordProtected(): Boolean {
        return System.currentTimeMillis() < stopWordProtectionEndTime
    }

    private fun isSTTError(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lowerText = text.lowercase()
        return lowerText.contains("list index out of range") ||
               lowerText.contains("索引超出范围") ||
               lowerText.contains("索引错误") ||
               (lowerText.contains("index") && lowerText.contains("range") && lowerText.contains("error"))
    }

    private fun isTTSAboutError(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lowerText = text.lowercase()
        return lowerText.contains("list index out of range") ||
               lowerText.contains("索引超出范围") ||
               lowerText.contains("索引错误") ||
               (lowerText.contains("index") && lowerText.contains("range") && lowerText.contains("error"))
    }
    
    private fun setupTtsCallbacks() {
        val textForPlayback = currentTtsText
        player.ttsPlayer.onTtsDurationReady = { durationMs ->
            pendingTtsDuration = durationMs
        }
        player.ttsPlayer.onTtsPlaybackStarted = {
            Log.d(TAG, "TTS playback started, duration=$pendingTtsDuration, text='${textForPlayback.take(20)}...'")
            onTtsDurationReady?.invoke(pendingTtsDuration, textForPlayback)
        }
        player.ttsPlayer.onTtsProgressUpdate = { currentMs, totalMs ->
            onTtsProgressUpdate?.invoke(currentMs, totalMs, textForPlayback)
        }
    }
}
