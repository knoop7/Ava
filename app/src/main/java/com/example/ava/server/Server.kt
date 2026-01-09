package com.example.ava.server

import android.util.Log
import com.google.protobuf.MessageLite
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ServerException(message: String?, cause: Throwable? = null) :
    Throwable(message, cause)

class Server(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : AutoCloseable {
    private val serverRef = AtomicReference<ServerSocket?>(null)
    private val connection = MutableStateFlow<ClientConnection?>(null)
    val isConnected = connection.map { it != null }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(port: Int = DEFAULT_SERVER_PORT) = acceptClients(port)
        .catch { throw ServerException(it.message, it) }
        .flatMapMerge {
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
        var server: ServerSocket? = null
        try {
            server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress("0.0.0.0", port))
            if (!serverRef.compareAndSet(null, server))
                error("Server already started")
            Log.i(TAG, "Server listening on 0.0.0.0:$port")

            while (true) {
                try {
                    
                    val clientSocket = withContext(Dispatchers.IO) {
                        server.accept()
                    }
                    emit(clientSocket)
                } catch (e: SocketException) {
                    if (server.isClosed) break
                    throw e
                }
            }
        } finally {
            server?.close()
            serverRef.compareAndSet(server, null)
        }
    }

    private fun connectClient(socket: Socket): Flow<MessageLite> {
        val client = ClientConnection(socket)
        connection.getAndUpdate { client }?.close()
        return client.readMessages().onCompletion {
            disconnectClient(client)
        }
    }

    private fun disconnectClient(client: ClientConnection) {
        client.close()
        connection.compareAndSet(client, null)
    }

    suspend fun sendMessage(message: MessageLite) = withContext(dispatcher) {
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