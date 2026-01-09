package com.example.ava.esphome.entities

import com.example.esphomeproto.api.ExecuteServiceRequest
import com.example.esphomeproto.api.ListEntitiesRequest
import com.example.esphomeproto.api.ListEntitiesServicesArgument
import com.example.esphomeproto.api.ListEntitiesServicesResponse
import com.example.esphomeproto.api.ServiceArgType
import com.google.protobuf.MessageLite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow


data class ServiceArg(
    val name: String,
    val type: ServiceArgType = ServiceArgType.SERVICE_ARG_TYPE_STRING
)


class ServiceEntity(
    val key: Int,
    val name: String,
    val args: List<ServiceArg> = emptyList(),
    val onExecute: suspend (Map<String, Any>) -> Unit
) : Entity {
    override fun handleMessage(message: MessageLite) = flow {
        when (message) {
            is ListEntitiesRequest -> {
                
                val sanitizedName = name.replace(" ", "").replace("-", "_").lowercase()
                emit(ListEntitiesServicesResponse.newBuilder()
                    .setKey(key)
                    .setName(sanitizedName)
                    .addAllArgs(args.map { arg ->
                        ListEntitiesServicesArgument.newBuilder()
                            .setName(arg.name)
                            .setType(arg.type)
                            .build()
                    })
                    .build())
            }

            is ExecuteServiceRequest -> {
                if (message.key == key) {
                    val argsMap = mutableMapOf<String, Any>()
                    message.argsList.forEachIndexed { index, arg ->
                        if (index < args.size) {
                            val argDef = args[index]
                            val value: Any = when (argDef.type) {
                                ServiceArgType.SERVICE_ARG_TYPE_BOOL -> arg.bool
                                ServiceArgType.SERVICE_ARG_TYPE_INT -> arg.int
                                ServiceArgType.SERVICE_ARG_TYPE_FLOAT -> arg.float
                                ServiceArgType.SERVICE_ARG_TYPE_STRING -> arg.string
                                else -> arg.string
                            }
                            argsMap[argDef.name] = value
                        }
                    }
                    onExecute(argsMap)
                }
            }
        }
    }

    override fun subscribe(): Flow<MessageLite> = emptyFlow()
}
