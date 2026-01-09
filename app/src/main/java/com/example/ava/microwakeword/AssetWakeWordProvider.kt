package com.example.ava.microwakeword

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class AssetWakeWordProvider(val assets: AssetManager, val path: String = DEFAULT_WAKE_WORD_PATH) :
    WakeWordProvider {
    override fun getWakeWords(): List<WakeWordWithId> {
        val gson = Gson()
        val wakeWords = buildList {
            val assetsList = assets.list(path)
            if (assetsList == null)
                return emptyList()

            for (asset in assetsList) {
                if (!asset.endsWith(".json"))
                    continue

                runCatching {
                    val json =
                        assets.open("$path/$asset").bufferedReader().use { it.readText() }
                    val wakeWord: WakeWord =
                        gson.fromJson(json, object : TypeToken<WakeWord>() {}.type)
                    add(WakeWordWithId(asset.substring(0, asset.lastIndexOf(".json")), wakeWord))
                }.onFailure {
                    Log.e(TAG, "Error loading wake word: $asset", it)
                }
            }
        }
        return wakeWords
    }

    override fun loadWakeWordModel(model: String): ByteBuffer {
        assets.open("$path/$model").use { inputStream ->
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            
            buffer.rewind()
            return buffer
        }
    }

    companion object {
        private const val TAG = "AssetWakeWordProvider"
        const val DEFAULT_WAKE_WORD_PATH = "wakeWords"
    }
}