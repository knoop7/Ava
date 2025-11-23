package com.example.ava.esphome

import android.util.Log
import com.example.ava.esphome.entities.Entity
import com.example.ava.server.Server
import com.example.ava.server.ServerException
import com.example.esphomeproto.api.ConnectRequest
import com.example.esphomeproto.api.DeviceInfoRequest
import com.example.esphomeproto.api.DeviceInfoResponse
import com.example.esphomeproto.api.DisconnectRequest
import com.example.esphomeproto.api.HelloRequest
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.MediaPlayerCommandRequest
import com.example.esphomeproto.api.PingRequest
import com.example.esphomeproto.api.SubscribeHomeAssistantStatesRequest
import com.example.esphomeproto.api.connectResponse
import com.example.esphomeproto.api.disconnectResponse
import com.example.esphomeproto.api.helloResponse
import com.example.esphomeproto.api.listEntitiesDoneResponse
import com.example.esphomeproto.api.pingResponse
import com.google.protobuf.GeneratedMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext

interface EspHomeState
data object Connected : EspHomeState
data object Disconnected : EspHomeState
data object Stopped : EspHomeState
data class ServerError(val message: String) : EspHomeState

abstract class EspHomeDevice(
    coroutineContext: CoroutineContext,
    protected val name: String,
    protected val port: Int = Server.DEFAULT_SERVER_PORT,
    entities: Iterable<Entity> = emptyList()
) : AutoCloseable {
    protected val server = Server()
    protected val entities = entities.toList()
    protected val _state = MutableStateFlow<EspHomeState>(Disconnected)
    val state = _state.asStateFlow()
    protected val isSubscribedToEntityState = MutableStateFlow(false)

    protected val scope = CoroutineScope(
        coroutineContext + Job(coroutineContext.job) + CoroutineName("${this.javaClass.simpleName} Scope")
    )

    open fun start() {
        startServer()
        startConnectedChangedListener()
        listenForEntityStateChanges()
    }

    protected abstract suspend fun getDeviceInfo(): DeviceInfoResponse

    private fun startServer() {
        server.start(port)
            .onEach { handleMessageInternal(it) }
            .catch { e ->
                if (e !is ServerException) throw e
                _state.value = ServerError(e.message ?: "Unknown error")
            }
            .launchIn(scope)
    }

    private fun startConnectedChangedListener() = server.isConnected
        .onEach { isConnected ->
            if (isConnected)
                onConnected()
            else
                onDisconnected()
        }
        .launchIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenForEntityStateChanges() = isSubscribedToEntityState
        .onStart {
            entities.forEach { it.start() }
        }
        .flatMapLatest { subscribed ->
            if (!subscribed)
                emptyFlow()
            else
                entities
                    .map { it.state }
                    .merge()
                    .onEach { sendMessage(it) }
        }.launchIn(scope)

    private suspend fun handleMessageInternal(message: GeneratedMessage) {
        Log.d(TAG, "Received message: ${message.javaClass.simpleName} $message")
        handleMessage(message)
    }

    protected open suspend fun handleMessage(message: GeneratedMessage) {
        when (message) {
            is HelloRequest -> sendMessage(helloResponse {
                name = this@EspHomeDevice.name
                apiVersionMajor = 1
                apiVersionMinor = 10
            })

            is ConnectRequest -> sendMessage(connectResponse { })

            is DisconnectRequest -> {
                sendMessage(disconnectResponse { })
                server.disconnectCurrentClient()
            }

            is DeviceInfoRequest -> sendMessage(getDeviceInfo())

            is PingRequest -> sendMessage(pingResponse { })

            is ListEntitiesRequest, is SubscribeHomeAssistantStatesRequest, is MediaPlayerCommandRequest -> {
                if (message is SubscribeHomeAssistantStatesRequest)
                    isSubscribedToEntityState.value = true
                entities.map { it.handleMessage(message) }.merge()
                    .collect { response -> sendMessage(response) }
                if (message is ListEntitiesRequest)
                    sendMessage(listEntitiesDoneResponse { })
            }
        }
    }

    protected suspend fun sendMessage(message: GeneratedMessage) {
        Log.d(TAG, "Sending message: ${message.javaClass.simpleName} $message")
        server.sendMessage(message)
    }

    protected open suspend fun onConnected() {
        _state.value = Connected
    }

    protected open suspend fun onDisconnected() {
        isSubscribedToEntityState.value = false
        _state.value = Disconnected
    }

    override fun close() {
        scope.cancel()
        server.close()
    }

    companion object {
        val TAG: String = this::class.java.simpleName
    }
}