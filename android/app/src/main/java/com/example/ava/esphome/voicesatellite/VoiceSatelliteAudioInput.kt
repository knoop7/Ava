package com.example.ava.esphome.voicesatellite

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.ava.audio.AudioRecordDeadObjectException
import com.example.ava.audio.MicrophoneInput
import com.example.ava.microwakeword.WakeWordDetector
import com.example.ava.microwakeword.WakeWordProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

class VoiceSatelliteAudioInput(
    activeWakeWords: List<String>,
    activeStopWords: List<String>,
    private val wakeWordProvider: WakeWordProvider,
    private val stopWordProvider: WakeWordProvider,
    muted: Boolean = false
) {
    val availableWakeWords by lazy { wakeWordProvider.getWakeWords() }
    val availableStopWords by lazy { stopWordProvider.getWakeWords() }
    private var currentWakeWordDetector: WakeWordDetector? = null

    private val _activeWakeWords = MutableStateFlow(activeWakeWords)
    val activeWakeWords = _activeWakeWords.asStateFlow()
    fun setActiveWakeWords(value: List<String>) {
        _activeWakeWords.value = value
    }

    private val _activeStopWords = MutableStateFlow(activeStopWords)
    val activeStopWords = _activeStopWords.asStateFlow()
    fun setActiveStopWords(value: List<String>) {
        _activeStopWords.value = value
    }

    private val _muted = MutableStateFlow(muted)
    val muted = _muted.asStateFlow()
    fun setMuted(value: Boolean) {
        _muted.value = value
    }

    private val _microphoneVolume = MutableStateFlow(1.0f)
    val microphoneVolume = _microphoneVolume.asStateFlow()
    fun setMicrophoneVolume(value: Float) {
        _microphoneVolume.value = value.coerceIn(0.0f, 2.0f)
    }

    private val _isStreaming = AtomicBoolean(false)
    var isStreaming: Boolean
        get() = _isStreaming.get()
        set(value) = _isStreaming.set(value)

    fun resetWakeWordDetector() {
        currentWakeWordDetector?.reset()
    }

    sealed class AudioResult {
        data class Audio(val audio: ByteString) : AudioResult()
        data class WakeDetected(val wakeWord: String, val wakeWordId: String = "") : AudioResult()
        data class StopDetected(val stopWord: String) : AudioResult()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() = muted.flatMapLatest {
        
        if (it) emptyFlow()
        else flow {
            var microphoneInput: MicrophoneInput? = null
            var recoveringFromAudioServerDeath = false
            var retryDelayMs = AUDIO_SERVER_RETRY_INITIAL_DELAY_MS
            var wakeWords = activeWakeWords.value
            var stopWords = activeStopWords.value

            val wakeWordDetector = WakeWordDetector(wakeWordProvider).apply {
                setActiveWakeWords(wakeWords)
            }
            currentWakeWordDetector = wakeWordDetector
            val stopWordDetector = WakeWordDetector(stopWordProvider).apply {
                setActiveWakeWords(stopWords)
            }
            try {
                while (currentCoroutineContext().isActive) {
                    if (microphoneInput == null) {
                        var candidate: MicrophoneInput? = null
                        try {
                            val newMicrophone = MicrophoneInput()
                            candidate = newMicrophone
                            newMicrophone.start()
                            microphoneInput = newMicrophone
                        } catch (e: Exception) {
                            candidate?.close()
                            if (!recoveringFromAudioServerDeath) throw e

                            Log.w(
                                TAG,
                                "Audio server is not ready; retrying microphone in ${retryDelayMs}ms",
                                e
                            )
                            delay(retryDelayMs)
                            retryDelayMs = (retryDelayMs * 2).coerceAtMost(
                                AUDIO_SERVER_RETRY_MAX_DELAY_MS
                            )
                            continue
                        }
                    }

                    if (wakeWords != activeWakeWords.value) {
                        wakeWords = activeWakeWords.value
                        wakeWordDetector.setActiveWakeWords(wakeWords)
                    }

                    if (stopWords != activeStopWords.value) {
                        stopWords = activeStopWords.value
                        stopWordDetector.setActiveWakeWords(stopWords)
                    }

                    val audio = try {
                        checkNotNull(microphoneInput).read()
                    } catch (e: AudioRecordDeadObjectException) {
                        Log.w(
                            TAG,
                            "Audio server died; closing the invalid microphone before retrying",
                            e
                        )
                        recoveringFromAudioServerDeath = true
                        microphoneInput?.close()
                        microphoneInput = null
                        delay(retryDelayMs)
                        retryDelayMs = (retryDelayMs * 2).coerceAtMost(
                            AUDIO_SERVER_RETRY_MAX_DELAY_MS
                        )
                        continue
                    }

                    if (recoveringFromAudioServerDeath) {
                        Log.i(TAG, "Microphone recovered after audio server restart")
                        recoveringFromAudioServerDeath = false
                        retryDelayMs = AUDIO_SERVER_RETRY_INITIAL_DELAY_MS
                        wakeWordDetector.reset()
                        stopWordDetector.reset()
                    }

                    if (isStreaming) {
                        val volume = _microphoneVolume.value
                        if (volume != 1.0f) {
                            val bytes = ByteArray(audio.remaining())
                            audio.get(bytes)
                            audio.rewind()
                            for (i in 0 until bytes.size - 1 step 2) {
                                val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
                                val signedSample = if (sample > 32767) sample - 65536 else sample
                                val amplified = (signedSample * volume).toInt().coerceIn(-32768, 32767)
                                bytes[i] = (amplified and 0xFF).toByte()
                                bytes[i + 1] = ((amplified shr 8) and 0xFF).toByte()
                            }
                            emit(AudioResult.Audio(ByteString.copyFrom(bytes)))
                        } else {
                            emit(AudioResult.Audio(ByteString.copyFrom(audio)))
                        }
                        audio.rewind()
                    }

                    
                    
                    val wakeDetections = wakeWordDetector.detect(audio)
                    audio.rewind()
                    if (wakeDetections.isNotEmpty()) {
                        
                        for (detection in wakeDetections) {
                            emit(AudioResult.WakeDetected(detection.wakeWordPhrase, detection.wakeWordId))
                        }
                    }

                    val stopDetections = stopWordDetector.detect(audio)
                    audio.rewind()
                    if (stopDetections.isNotEmpty()) {
                        emit(AudioResult.StopDetected(stopDetections.first().wakeWordPhrase))
                    }

                    
                    
                    yield()
                }
            } finally {
                microphoneInput?.close()
                wakeWordDetector.close()
                stopWordDetector.close()
            }
        }
    }

    companion object {
        private const val TAG = "VoiceSatelliteAudioInput"
        private const val AUDIO_SERVER_RETRY_INITIAL_DELAY_MS = 250L
        private const val AUDIO_SERVER_RETRY_MAX_DELAY_MS = 5_000L
    }
}
