package com.example.ava.esphome.entities

import com.example.esphomeproto.api.CameraImageRequest
import com.example.esphomeproto.api.EntityCategory
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.cameraImageResponse
import com.example.esphomeproto.api.listEntitiesCameraResponse
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class CameraEntity(
    val key: Int,
    val name: String,
    val objectId: String,
    val icon: String = "",
    val entityCategory: EntityCategory = EntityCategory.ENTITY_CATEGORY_NONE
) : Entity {
    
    private val _imageFlow = MutableSharedFlow<ByteArray>(replay = 1)
    @Volatile private var isStreaming = false
    @Volatile private var isSending = false
    
    fun sendImage(jpegData: ByteArray) {
        _imageFlow.tryEmit(jpegData)
    }
    
    override fun handleMessage(message: MessageLite) = flow {
        when (message) {
            is ListEntitiesRequest -> emit(listEntitiesCameraResponse {
                key = this@CameraEntity.key
                name = this@CameraEntity.name
                objectId = this@CameraEntity.objectId
                if (this@CameraEntity.icon.isNotEmpty()) {
                    icon = this@CameraEntity.icon
                }
                entityCategory = this@CameraEntity.entityCategory
            })
            
            is CameraImageRequest -> {
                
                if (message.single) {
                    _imageFlow.replayCache.lastOrNull()?.let { imageData ->
                        emitImageChunks(imageData)
                    }
                }
                
                if (message.stream) {
                    isStreaming = true
                }
            }
        }
    }
    
    override fun subscribe(): Flow<MessageLite> = flow {
        _imageFlow.collect { imageData ->
            
            if (!isSending) {
                isSending = true
                try {
                    emitImageChunks(imageData)
                } finally {
                    isSending = false
                }
            }
        }
    }
    
    private suspend fun kotlinx.coroutines.flow.FlowCollector<MessageLite>.emitImageChunks(imageData: ByteArray) {
        val chunkSize = 1024
        val chunks = imageData.toList().chunked(chunkSize)
        chunks.forEachIndexed { index, chunk ->
            emit(cameraImageResponse {
                key = this@CameraEntity.key
                data = ByteString.copyFrom(chunk.toByteArray())
                done = (index == chunks.lastIndex)
            })
        }
    }
}
