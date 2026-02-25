package com.example.ava.audio

import kotlin.math.abs

class SilenceDetector(
    private val silenceThreshold: Float = 0.008f,
    private val silenceDurationMs: Long = 1200,
    private val minSpeechDurationMs: Long = 500
) {
    private var lastSoundTime: Long = 0
    private var speechStartTime: Long = 0
    private var isSpeaking: Boolean = false

    fun reset() {
        lastSoundTime = 0
        speechStartTime = 0
        isSpeaking = false
    }

    fun processAudio(audioBytes: ByteArray): Boolean {
        val currentTime = System.currentTimeMillis()
        val silence = checkSilence(audioBytes)

        if (silence) {
            if (isSpeaking && lastSoundTime > 0) {
                val silenceDur = currentTime - lastSoundTime
                if (silenceDur >= silenceDurationMs) {
                    val speechDur = lastSoundTime - speechStartTime
                    if (speechDur >= minSpeechDurationMs) {
                        isSpeaking = false
                        return true
                    }
                    isSpeaking = false
                }
            }
        } else {
            lastSoundTime = currentTime
            if (!isSpeaking) {
                isSpeaking = true
                speechStartTime = currentTime
            }
        }

        return false
    }

    private fun checkSilence(audioBytes: ByteArray): Boolean {
        if (audioBytes.size < 2) return true

        var sum = 0L
        var count = 0

        var i = 0
        while (i < audioBytes.size - 1) {
            val lo = audioBytes[i].toInt() and 0xFF
            val hi = audioBytes[i + 1].toInt()
            val sample = lo or (hi shl 8)
            val signed = if (sample > 32767) sample - 65536 else sample
            sum += abs(signed)
            count++
            i += 2
        }

        if (count == 0) return true

        val volume = sum.toFloat() / count
        return volume < (silenceThreshold * 32768)
    }
}
