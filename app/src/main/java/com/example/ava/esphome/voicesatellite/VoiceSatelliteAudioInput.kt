package com.example.ava.esphome.voicesatellite

import android.Manifest
import androidx.annotation.RequiresPermission
import com.example.ava.audio.MicrophoneInput
import com.example.ava.microwakeword.WakeWordDetector
import com.example.ava.microwakeword.WakeWordProvider
import com.google.protobuf.ByteString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean

class VoiceSatelliteAudioInput(
    activeWakeWords: List<String>,
    activeStopWords: List<String>,
    private val wakeWordProvider: WakeWordProvider,
    private val stopWordProvider: WakeWordProvider
) {
    val availableWakeWords = wakeWordProvider.getWakeWords()
    val availableStopWords = stopWordProvider.getWakeWords()

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

    private val _isStreaming = AtomicBoolean(false)
    var isStreaming: Boolean
        get() = _isStreaming.get()
        set(value) = _isStreaming.set(value)

    sealed class AudioResult {
        data class Audio(val audio: ByteString) : AudioResult()
        data class WakeDetected(val wakeWord: String) : AudioResult()
        data class StopDetected(val stopWord: String) : AudioResult()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() = flow {
        val microphoneInput = MicrophoneInput()
        var wakeWords = activeWakeWords.value
        var stopWords = activeStopWords.value

        val wakeWordDetector = WakeWordDetector(wakeWordProvider).apply {
            setActiveWakeWords(wakeWords)
        }
        val stopWordDetector = WakeWordDetector(stopWordProvider).apply {
            setActiveWakeWords(stopWords)
        }
        try {
            microphoneInput.start()
            while (true) {
                if (wakeWords != activeWakeWords.value) {
                    wakeWords = activeWakeWords.value
                    wakeWordDetector.setActiveWakeWords(wakeWords)
                }

                if (stopWords != activeStopWords.value) {
                    stopWords = activeStopWords.value
                    stopWordDetector.setActiveWakeWords(stopWords)
                }

                val audio = microphoneInput.read()
                if (isStreaming) {
                    emit(AudioResult.Audio(ByteString.copyFrom(audio)))
                    audio.rewind()
                }

                // Always run audio through the models to keep
                // their internal state up to date
                val wakeDetections = wakeWordDetector.detect(audio)
                audio.rewind()
                if (wakeDetections.isNotEmpty()) {
                    emit(AudioResult.WakeDetected(wakeDetections.first().wakeWordPhrase))
                }

                val stopDetections = stopWordDetector.detect(audio)
                audio.rewind()
                if (stopDetections.isNotEmpty()) {
                    emit(AudioResult.StopDetected(stopDetections.first().wakeWordPhrase))
                }

                // yield to ensure upstream emissions and
                // cancellation have a chance to occur
                yield()
            }
        } finally {
            microphoneInput.close()
            wakeWordDetector.close()
            stopWordDetector.close()
        }
    }
}