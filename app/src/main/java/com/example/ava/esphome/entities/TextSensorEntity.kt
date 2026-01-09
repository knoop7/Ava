package com.example.ava.esphome.entities

import com.example.esphomeproto.api.EntityCategory
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.listEntitiesTextSensorResponse
import com.example.esphomeproto.api.textSensorStateResponse
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class TextSensorEntity(
    val key: Int,
    val name: String,
    val objectId: String,
    val icon: String = "",
    val entityCategory: EntityCategory = EntityCategory.ENTITY_CATEGORY_NONE
) : Entity {
    
    private val _state = MutableStateFlow("")
    
    fun updateState(value: String) {
        _state.value = value
    }
    
    override fun handleMessage(message: MessageLite) = flow {
        when (message) {
            is ListEntitiesRequest -> emit(listEntitiesTextSensorResponse {
                key = this@TextSensorEntity.key
                name = this@TextSensorEntity.name
                objectId = this@TextSensorEntity.objectId
                if (this@TextSensorEntity.icon.isNotEmpty()) {
                    icon = this@TextSensorEntity.icon
                }
                entityCategory = this@TextSensorEntity.entityCategory
            })
        }
    }

    override fun subscribe(): Flow<MessageLite> = _state.map {
        textSensorStateResponse {
            key = this@TextSensorEntity.key
            state = it
            missingState = false
        }
    }
}
