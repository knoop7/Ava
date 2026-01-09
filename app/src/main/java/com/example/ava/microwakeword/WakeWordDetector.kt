package com.example.ava.microwakeword

import android.util.Log
import com.example.ava.utils.fillFrom
import com.example.microfeatures.MicroFrontend
import java.nio.ByteBuffer

private const val SAMPLES_PER_SECOND = 16000
private const val SAMPLES_PER_CHUNK = 160  
private const val BYTES_PER_SAMPLE = 2  
private const val BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * BYTES_PER_SAMPLE

class WakeWordDetector(private val wakeWordProvider: WakeWordProvider) : AutoCloseable {
    private val frontend = MicroFrontend()
    private val buffer = ByteBuffer.allocateDirect(BYTES_PER_CHUNK)
    private val wakeWords = wakeWordProvider.getWakeWords()
    private var activeWakeWords = listOf<MicroWakeWord>()

    data class DetectionResult(
        val wakeWordId: String,
        val wakeWordPhrase: String
    )

    fun detect(audio: ByteBuffer): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()
        buffer.fillFrom(audio)
        while (buffer.flip().remaining() == BYTES_PER_CHUNK) {
            val processOutput = frontend.processSamples(buffer)
            buffer.position(buffer.position() + processOutput.samplesRead * BYTES_PER_SAMPLE)
            buffer.compact()
            buffer.fillFrom(audio)
            if (processOutput.features.isEmpty())
                continue
            for (wakeWord in activeWakeWords) {
                val result = wakeWord.processAudioFeatures(processOutput.features)
                if (result && !detections.any { it.wakeWordId == wakeWord.id })
                    detections.add(DetectionResult(wakeWord.id, wakeWord.wakeWord))
            }
        }
        buffer.compact()
        return detections
    }

    fun setActiveWakeWords(wakeWordIds: List<String>) {
        for (wakeWord in activeWakeWords)
            wakeWord.close()
        activeWakeWords = buildList {
            for (wakeWordId in wakeWordIds) {
                val wakeWordWithId = wakeWords.firstOrNull { it.id == wakeWordId }
                if (wakeWordWithId == null) {
                    Log.w(TAG, "Wake word with id $wakeWordId not found")
                    continue
                }
                add(
                    MicroWakeWord(
                        wakeWordWithId.id,
                        wakeWordWithId.wakeWord.wake_word,
                        wakeWordProvider.loadWakeWordModel(wakeWordWithId.wakeWord.model),
                        wakeWordWithId.wakeWord.micro.probability_cutoff,
                        wakeWordWithId.wakeWord.micro.sliding_window_size
                    )
                )
            }
        }
    }

    fun reset() {
        for (wakeWord in activeWakeWords) {
            wakeWord.reset()
        }
    }

    override fun close() {
        frontend.close()
        for (model in activeWakeWords)
            model.close()
    }

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}