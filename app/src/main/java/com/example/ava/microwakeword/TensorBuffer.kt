package com.example.ava.microwakeword

import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

abstract class TensorBuffer(
    dataType: DataType,
    shape: IntArray,
    val scale: Float,
    val zeroPoint: Int
) {
    private val _flatSize: Int = shape.reduce { acc, i -> acc * i }
    protected val buffer: ByteBuffer =
        ByteBuffer.allocateDirect(_flatSize * dataType.byteSize()).order(ByteOrder.nativeOrder())

    val flatSize get() = _flatSize
    val isComplete get() = !buffer.hasRemaining()

    abstract fun put(src: FloatArray)

    fun getTensor(): ByteBuffer {
        val tensor = buffer.duplicate()
            .order(ByteOrder.nativeOrder())
            .apply { flip() }
        return tensor
    }

    fun clear() {
        buffer.clear()
    }

    fun quantize(value: Float): Float {
        return (value / scale) + zeroPoint
    }

    companion object {
        fun create(
            dataType: DataType,
            shape: IntArray,
            scale: Float,
            zeroPoint: Int
        ): TensorBuffer {
            return when (dataType) {
                DataType.FLOAT32 -> TensorBufferFloat(shape, scale, zeroPoint)
                DataType.UINT8, DataType.INT8 -> TensorBufferUint8(shape, scale, zeroPoint)
                else -> throw IllegalArgumentException("Unsupported data type: $dataType")
            }
        }
    }
}

class TensorBufferUint8(shape: IntArray, scale: Float, zeroPoint: Int) :
    TensorBuffer(DataType.UINT8, shape, scale, zeroPoint) {
    override fun put(src: FloatArray) {
        for (value in src) {
            buffer.put(quantize(value).roundToInt().toByte())
        }
    }
}

class TensorBufferFloat(shape: IntArray, scale: Float, zeroPoint: Int) :
    TensorBuffer(DataType.FLOAT32, shape, scale, zeroPoint) {
    override fun put(src: FloatArray) {
        for (value in src) {
            buffer.putFloat(quantize(value))
        }
    }
}