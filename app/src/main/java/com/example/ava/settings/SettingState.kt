package com.example.ava.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class SettingState<T>(
    private val flow: Flow<T>,
    private val set: suspend (T) -> Unit
) : Flow<T> {
    suspend fun get() = flow.first()
    suspend fun set(value: T) = set.invoke(value)
    override suspend fun collect(collector: FlowCollector<T>) =
        flow.distinctUntilChanged().collect(collector)
}