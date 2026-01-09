package com.example.esphomeproto

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.EOFException
import java.lang.AutoCloseable
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousByteChannel
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class AsynchronousBufferedByteChannel<T : AsynchronousByteChannel>(
    val baseChannel: T,
    bufferSize: Int = 4096
) : AutoCloseable {
    val buffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize).flip()

    suspend fun writeFully(buffer: ByteBuffer) {
        while (buffer.remaining() > 0)
            baseChannel.writeAsync(buffer)
    }

    suspend fun readByte(): Byte {
        while (buffer.remaining() == 0) {
            if (fillBuffer() == -1) {
                throw EOFException()
            }
        }
        return buffer.get()
    }

    suspend fun readFully(destination: ByteArray, offset: Int, length: Int) {
        if (length == 0) {
            return
        }
        var read = readBuffer(destination, offset, length)
        while (read < length) {
            if (fillBuffer() == -1) {
                throw EOFException()
            }
            read += readBuffer(
                destination,
                offset + read,
                length - read
            )
        }
    }

    private fun readBuffer(destination: ByteArray, offset: Int, length: Int): Int {
        val read = length.coerceAtMost(buffer.remaining())
        if (read > 0) {
            buffer.get(destination, offset, read)
        }
        return read
    }

    private suspend fun fillBuffer(): Int {
        buffer.clear()
        try {
            return baseChannel.readAsync(buffer)
        } finally {
            buffer.flip()
        }
    }

    override fun close() {
        baseChannel.close()
    }
}

suspend fun AsynchronousByteChannel.readAsync(buffer: ByteBuffer) =
    suspendCancellableCoroutine { cont ->
        read(buffer, cont, asyncByteChannelHandler())
    }

suspend fun AsynchronousByteChannel.writeAsync(buffer: ByteBuffer) =
    suspendCancellableCoroutine { cont ->
        write(buffer, cont, asyncByteChannelHandler())
    }

private fun asyncByteChannelHandler(): CompletionHandler<Int, CancellableContinuation<Int>> =
    object : CompletionHandler<Int, CancellableContinuation<Int>> {
        override fun completed(result: Int, cont: CancellableContinuation<Int>) {
            cont.resume(result)
        }

        override fun failed(ex: Throwable, cont: CancellableContinuation<Int>) {
            
            if (ex is AsynchronousCloseException && cont.isCancelled) return
            cont.resumeWithException(ex)
        }
    }