package com.example.ava.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

interface SettingsStore<T> {
    fun getFlow(): Flow<T>
    suspend fun get(): T
    suspend fun update(transform: suspend (T) -> T)
}

abstract class SettingsStoreImpl<T>(val dataStore: DataStore<T>, private val default: T) :
    SettingsStore<T> {
    override fun getFlow() = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading settings, returning defaults", exception)
                emit(default)
            } else throw exception
        }
        .onEach { Log.d(TAG, "Loaded settings: $it") }

    override suspend fun get(): T = getFlow().first()

    override suspend fun update(transform: suspend (T) -> T) {
        dataStore.updateData(transform)
    }

    companion object {
        private const val TAG = "SettingsStore"
    }
}