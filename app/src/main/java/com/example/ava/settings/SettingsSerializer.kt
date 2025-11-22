package com.example.ava.settings

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "SettingsCorruptionHandler"

fun <T> defaultCorruptionHandler(default: T) = ReplaceFileCorruptionHandler { exception ->
    Log.e(TAG, "Error reading settings, returning defaults", exception)
    default
}

class SettingsSerializer<T>(val serializer: KSerializer<T>, override val defaultValue: T) :
    Serializer<T> {
    override suspend fun readFrom(input: InputStream): T =
        try {
            Json.decodeFromString(
                serializer,
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Settings", serialization)
        }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(
            Json.encodeToString(serializer, t)
                .encodeToByteArray()
        )
    }
}
