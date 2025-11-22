package com.example.ava.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

abstract class SettingsStore<T>(val dataStore: DataStore<T>, private val default: T) {
    fun getFlow() = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading settings, returning defaults", exception)
                emit(default)
            } else throw exception
        }
        .onEach { Log.d(TAG, "Loaded settings: $it") }

    suspend fun get(): T = getFlow().first()

    suspend fun update(transform: suspend (T) -> T) {
        dataStore.updateData(transform)
    }

    companion object {
        private const val TAG = "SettingsStore"
    }
}