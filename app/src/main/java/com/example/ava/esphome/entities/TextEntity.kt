package com.example.ava.esphome.entities

import com.example.esphomeproto.api.EntityCategory
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.ListEntitiesTextResponse
import com.example.esphomeproto.api.TextCommandRequest
import com.example.esphomeproto.api.TextStateResponse
import com.example.esphomeproto.api.TextMode
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class TextEntity(
    val key: Int,
    val name: String,
    val objectId: String,
    val icon: String = "",
    val getState: Flow<String>,
    val setState: suspend (String) -> Unit,
    val entityCategory: EntityCategory = EntityCategory.ENTITY_CATEGORY_NONE
) : Entity {
    override fun handleMessage(message: MessageLite) = flow {
        when (message) {
            is ListEntitiesRequest -> emit(ListEntitiesTextResponse.newBuilder()
                .setKey(this@TextEntity.key)
                .setName(this@TextEntity.name)
                .setObjectId(this@TextEntity.objectId)
                .also { builder ->
                    if (this@TextEntity.icon.isNotEmpty()) {
                        builder.setIcon(this@TextEntity.icon)
                    }
                    builder.setMode(TextMode.TEXT_MODE_TEXT)
                    builder.setMinLength(0)
                    builder.setMaxLength(65535)
                    builder.setEntityCategory(this@TextEntity.entityCategory)
                }
                .build())

            is TextCommandRequest -> {
                if (message.key == key)
                    setState(message.state)
            }
        }
    }

    override fun subscribe() = getState.map {
        TextStateResponse.newBuilder()
            .setKey(this@TextEntity.key)
            .setState(it)
            .setMissingState(false)
            .build()
    }
}
