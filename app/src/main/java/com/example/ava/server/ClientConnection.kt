package com.example.ava.server

import android.util.Log
import com.example.esphomeproto.MESSAGE_PARSERS
import com.example.esphomeproto.MESSAGE_TYPES
import com.google.protobuf.MessageLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ClientConnection(private val socket: Socket) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)
    private val sendMutex = Mutex()
    private val inputStream: InputStream = socket.getInputStream()
    private val outputStream: OutputStream = socket.getOutputStream()
    private val bufferedReader = BufferedInputStream(inputStream)

    fun readMessages() = flow {
        while (true) {
            try {
                val message = readMessage()
                if (message != null) {
                    emit(message)
                }
            } catch (e: Exception) {
                if (isClosed.get()) {
                    break
                }
                Log.e(TAG, "Error in read loop", e)
                throw e
            }
        }
    }.catch { e ->
        if (e !is IOException) {
            Log.e(TAG, "Non-IO error in readMessages", e)
            throw e
        }
        
        if (!isClosed.get())
            Log.e(TAG, "IOException reading from socket: ${e.message}", e)
        else
            Log.d(TAG, "Connection closed (expected)")
    }

    private fun readMessage(): MessageLite? {
        var message: MessageLite?
        do {
            message = readMessageInternal()
        } while (message == null)
        return message
    }

    private fun readMessageInternal(): MessageLite? {
        
        val indicator = bufferedReader.read()
        if (indicator == -1) {
            throw IOException("EOF reading indicator")
        }
        if (indicator != 0) {
            Log.w(TAG, "Unsupported indicator: $indicator")
            return null
        }

        
        val length = readVarUInt().toInt()

        
        val messageType = readVarUInt().toInt()

        
        val messageBytes = ByteArray(length)
        var bytesRead = 0
        while (bytesRead < length) {
            val read = bufferedReader.read(messageBytes, bytesRead, length - bytesRead)
            if (read == -1) {
                throw IOException("Connection closed while reading message")
            }
            bytesRead += read
        }

        
        val parser = MESSAGE_PARSERS[messageType]
        if (parser == null) {
            Log.w(TAG, "Unknown message type: $messageType")
            return null
        }
        return parser.parseFrom(messageBytes) as MessageLite
    }

    private fun readVarUInt(): UInt {
        var result = 0u
        var shift = 0

        while (shift < 32) {
            val byteVal = bufferedReader.read()
            if (byteVal == -1) {
                throw IOException("EOF while reading varuint")
            }
            val unsignedByte = byteVal.toUInt()
            result = result or ((unsignedByte and 0x7Fu) shl shift)
            if (unsignedByte and 0x80u == 0u) {
                return result
            }
            shift += 7
        }

        throw IOException("VarUInt value too large")
    }

    suspend fun sendMessage(message: MessageLite) {
        sendMutex.withLock {
            try {
                withContext(Dispatchers.IO) {
                    val messageType = MESSAGE_TYPES[message::class.java]
                        ?: throw IllegalArgumentException("Unknown message type: ${message::class}")

                    val messageBytes = message.toByteArray()

                    
                    val byteStream = ByteArrayOutputStream()

                    
                    byteStream.write(0)

                    
                    writeVarUInt(byteStream, messageBytes.size)

                    
                    writeVarUInt(byteStream, messageType)

                    
                    byteStream.write(messageBytes)

                    
                    outputStream.write(byteStream.toByteArray())
                    outputStream.flush()
                }
            } catch (e: IOException) {
                if (!isClosed.get())
                    Log.e(TAG, "Error writing to socket", e)
            }
        }
    }

    private fun writeVarUInt(stream: OutputStream, value: Int) {
        var v = value
        while ((v and 0xFFFFFF80.toInt()) != 0) {
            stream.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        stream.write(v and 0x7F)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket", e)
            }
        }
    }

    companion object {
        const val TAG = "ClientConnection"
    }
}