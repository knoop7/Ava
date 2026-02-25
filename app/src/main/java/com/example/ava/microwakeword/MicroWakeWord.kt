package com.example.ava.microwakeword

import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.lang.RuntimeException

private const val SAMPLES_PER_SECOND = 16000
private const val SAMPLES_PER_CHUNK = 160  
private const val BYTES_PER_SAMPLE = 2  
private const val BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * BYTES_PER_SAMPLE
private const val SECONDS_PER_CHUNK = SAMPLES_PER_CHUNK / SAMPLES_PER_SECOND
private const val STRIDE = 3
private const val DEFAULT_REFRACTORY_SECONDS = 0.3f
private const val CHUNKS_PER_SECOND = SAMPLES_PER_SECOND.toFloat() / SAMPLES_PER_CHUNK

class MicroWakeWord(
    val id: String,
    val wakeWord: String,
    private val model: ByteBuffer,
    private val probabilityCutoff: Float,
    private val slidingWindowSize: Int
) : AutoCloseable {
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var inputTensorBuffer: TensorBuffer? = null
    private var outputScale: Float = 0f
    private var outputZeroPoint: Int = 0
    private val probabilities = ArrayDeque<Float>(slidingWindowSize)
    private var refractoryCounter = 0
    private val refractoryChunks = (DEFAULT_REFRACTORY_SECONDS * CHUNKS_PER_SECOND).toInt()

    private fun initializeIfNeeded() {
        if (!isInitialized) {
            try {
                
                val tempModel = model.duplicate()
                tempModel.rewind()
                interpreter = Interpreter(tempModel)

                interpreter?.allocateTensors()
                val inputDetails = interpreter!!.getInputTensor(0)
                val inputQuantParams = inputDetails.quantizationParams()
                inputTensorBuffer = TensorBuffer.create(
                    inputDetails.dataType(),
                    inputDetails.shape(),
                    inputQuantParams.scale,
                    inputQuantParams.zeroPoint
                )

                val outputDetails = interpreter!!.getOutputTensor(0)
                val outputQuantParams = outputDetails.quantizationParams()
                outputScale = outputQuantParams.scale
                outputZeroPoint = outputQuantParams.zeroPoint

                isInitialized = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "TensorFlow Lite native library not available", e)
                throw RuntimeException("TensorFlow Lite is not supported on this device", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TensorFlow Lite interpreter", e)
                throw e
            }
        }
    }

    fun processAudioFeatures(features: FloatArray): Boolean {
        if (features.isEmpty())
            return false

        initializeIfNeeded()

        if (!isInitialized) {
            Log.w(TAG, "Interpreter not initialized, returning false")
            return false
        }

        val tensorBuffer = inputTensorBuffer ?: return false
        if (features.size * STRIDE != tensorBuffer.flatSize)
            error("Unexpected feature size ${features.size} for stride $STRIDE and tensor size ${tensorBuffer.flatSize}")

        tensorBuffer.put(features)
        if (!tensorBuffer.isComplete)
            return false

        val probability = getWakeWordProbability(tensorBuffer.getTensor())
        tensorBuffer.clear()
        return isWakeWordDetected(probability)
    }

    private fun getWakeWordProbability(input: ByteBuffer): Float {
        val output = Array(1) { ByteArray(1) }
        interpreter?.run(input, output) ?: return 0f
        val probability = (output[0][0].toUByte().toFloat() - outputZeroPoint) * outputScale
        return probability
    }

    private fun isWakeWordDetected(probability: Float): Boolean {
        if (refractoryCounter > 0) {
            refractoryCounter--
            return false
        }
        
        if (probabilities.size == slidingWindowSize)
            probabilities.removeFirst()
        probabilities.add(probability)
        
        val detected = probabilities.size == slidingWindowSize && probabilities.average() > probabilityCutoff
        if (detected) {
            probabilities.clear()
            refractoryCounter = refractoryChunks
        }
        return detected
    }

    fun reset() {
        probabilities.clear()
        refractoryCounter = 0
    }

    override fun close() {
        interpreter?.close()
    }

    companion object {
        private const val TAG = "MicroWakeWord"
    }
}