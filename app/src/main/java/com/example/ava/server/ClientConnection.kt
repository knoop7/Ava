package com.example.ava.server

import android.util.Log
import com.example.esphomeproto.AsynchronousCodedChannel
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class ClientConnection(socket: AsynchronousSocketChannel) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)
    private val channel = AsynchronousCodedChannel(socket)
    private val sendMutex = Mutex()

    fun readMessages() =
        flow {
            while (true) {
                emit(channel.readMessage())
            }
        }.catch {
            if (it !is IOException) throw it
            // Exception is expected if client was manually closed
            if (!isClosed.get())
                Log.e(TAG, "Error reading from socket", it)
        }

    suspend fun sendMessage(message: GeneratedMessage) {
        // Multiple send requests are not allowed at the same time so hold the lock until the send is complete
        sendMutex.withLock {
            try {
                channel.writeMessage(message)
            } catch (e: IOException) {
                // Exception is expected if client was manually closed
                if (!isClosed.get())
                    Log.e(TAG, "Error writing to socket", e)
            }
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true))
            channel.close()
    }

    companion object {
        const val TAG = "ClientConnection"
    }
}