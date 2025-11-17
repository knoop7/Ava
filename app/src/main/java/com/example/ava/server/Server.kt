package com.example.ava.server

import android.util.Log
import com.example.ava.utils.acceptAsync
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicReference

class ServerException(message: String?, cause: Throwable? = null) :
    Throwable(message, cause)

class Server(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : AutoCloseable {
    private val serverRef = AtomicReference<AsynchronousServerSocketChannel?>(null)
    private val connection = MutableStateFlow<ClientConnection?>(null)
    val isConnected = connection.map { it != null }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(port: Int = DEFAULT_SERVER_PORT) = acceptClients(port)
        .catch { throw ServerException(it.message, it) }
        .flatMapConcat {
            connectClient(it)
        }
        .catch {
            if (it is ServerException) throw it
            Log.e(TAG, "Client connection error", it)
        }
        .flowOn(dispatcher)

    fun disconnectCurrentClient() {
        connection.value?.let { disconnectClient(it) }
    }

    private fun acceptClients(port: Int) = flow {
        var server: AsynchronousServerSocketChannel? = null
        try {
            server = AsynchronousServerSocketChannel.open()
            if (!serverRef.compareAndSet(null, server))
                error("Server already started")
            server.bind(InetSocketAddress(port))
            while (true) {
                val accepted = server.acceptAsync()
                emit(accepted)
            }
        } finally {
            server?.close()
            serverRef.compareAndSet(server, null)
        }
    }

    private fun connectClient(socket: AsynchronousSocketChannel): Flow<GeneratedMessage> {
        val client = ClientConnection(socket)
        connection.getAndUpdate { client }?.close()
        return client.readMessages().onCompletion {
            disconnectClient(client)
        }
    }

    private fun disconnectClient(client: ClientConnection) {
        client.close()
        val updated = connection.compareAndSet(client, null)
        Log.d(TAG, "Disconnected client: $updated")
    }

    suspend fun sendMessage(message: GeneratedMessage) = withContext(dispatcher) {
        connection.value?.sendMessage(message)
    }

    override fun close() {
        connection.getAndUpdate { null }?.close()
        serverRef.getAndSet(null)?.close()
    }

    companion object {
        const val TAG = "Server"
        const val DEFAULT_SERVER_PORT = 6053
    }
}