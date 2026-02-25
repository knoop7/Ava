package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream


@Serializable
data class BrowserSettings(
    val haRemoteUrlEnabled: Boolean = false,
    val enableBrowserDisplay: Boolean = false,
    val enableBrowserVisible: Boolean = false,
    val pullRefreshEnabled: Boolean = true,
    val initialScale: Int = 0,  
    val fontSize: Int = 100,  
    val touchEnabled: Boolean = true,
    val dragEnabled: Boolean = true,
    val hardwareAcceleration: Boolean = true,  
    val settingsButtonEnabled: Boolean = false,
    val advancedControlEnabled: Boolean = false,  
    val backKeyHideEnabled: Boolean = true,
    val tampermonkeyEnabled: Boolean = false,
    val userAgentMode: Int = 0
) {
    companion object {
        val DEFAULT = BrowserSettings()
    }
}


object BrowserSettingsSerializer : Serializer<BrowserSettings> {
    override val defaultValue: BrowserSettings = BrowserSettings.DEFAULT
    
    override suspend fun readFrom(input: InputStream): BrowserSettings {
        return try {
            Json.decodeFromString(
                BrowserSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    override suspend fun writeTo(t: BrowserSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(BrowserSettings.serializer(), t).toByteArray()
        )
    }
}

val Context.browserSettingsDataStore: DataStore<BrowserSettings> by dataStore(
    fileName = "browser_settings.json",
    serializer = BrowserSettingsSerializer
)


class BrowserSettingsStore(private val context: Context) {
    private val dataStore = context.browserSettingsDataStore
    
    fun getFlow(): Flow<BrowserSettings> = dataStore.data
    
    suspend fun get(): BrowserSettings = dataStore.data.first()
    
    suspend fun update(transform: (BrowserSettings) -> BrowserSettings) {
        dataStore.updateData(transform)
    }
    
    
    val haRemoteUrlEnabled: Flow<Boolean> = getFlow().map { it.haRemoteUrlEnabled }
    val pullRefreshEnabled: Flow<Boolean> = getFlow().map { it.pullRefreshEnabled }
    val initialScale: Flow<Int> = getFlow().map { it.initialScale }
    val fontSize: Flow<Int> = getFlow().map { it.fontSize }
    val touchEnabled: Flow<Boolean> = getFlow().map { it.touchEnabled }
    val dragEnabled: Flow<Boolean> = getFlow().map { it.dragEnabled }
    val hardwareAcceleration: Flow<Boolean> = getFlow().map { it.hardwareAcceleration }
    val settingsButtonEnabled: Flow<Boolean> = getFlow().map { it.settingsButtonEnabled }
    val advancedControlEnabled: Flow<Boolean> = getFlow().map { it.advancedControlEnabled }
    val backKeyHideEnabled: Flow<Boolean> = getFlow().map { it.backKeyHideEnabled }
    val tampermonkeyEnabled: Flow<Boolean> = getFlow().map { it.tampermonkeyEnabled }
    val userAgentMode: Flow<Int> = getFlow().map { it.userAgentMode }
    val enableBrowserDisplay: Flow<Boolean> = getFlow().map { it.enableBrowserDisplay }
    val enableBrowserVisible: SettingState<Boolean> = SettingState(
        getFlow().map { it.enableBrowserVisible },
        { value -> update { it.copy(enableBrowserVisible = value) } }
    )
    
    suspend fun setEnableBrowserDisplay(enabled: Boolean) {
        update { it.copy(enableBrowserDisplay = enabled) }
    }
    
    suspend fun setHaRemoteUrlEnabled(enabled: Boolean) {
        update { it.copy(haRemoteUrlEnabled = enabled) }
    }
    
    suspend fun setPullRefreshEnabled(enabled: Boolean) {
        update { it.copy(pullRefreshEnabled = enabled) }
    }
    
    suspend fun setInitialScale(scale: Int) {
        update { it.copy(initialScale = scale.coerceIn(0, 500)) }
    }
    
    suspend fun setFontSize(size: Int) {
        update { it.copy(fontSize = size.coerceIn(50, 300)) }
    }
    
    suspend fun setTouchEnabled(enabled: Boolean) {
        update { it.copy(touchEnabled = enabled) }
    }
    
    suspend fun setDragEnabled(enabled: Boolean) {
        update { it.copy(dragEnabled = enabled) }
    }
    
    suspend fun setHardwareAcceleration(enabled: Boolean) {
        update { it.copy(hardwareAcceleration = enabled) }
    }
    
    suspend fun setSettingsButtonEnabled(enabled: Boolean) {
        update { it.copy(settingsButtonEnabled = enabled) }
    }
    
    suspend fun setAdvancedControlEnabled(enabled: Boolean) {
        update { it.copy(advancedControlEnabled = enabled) }
    }
    
    suspend fun setBackKeyHideEnabled(enabled: Boolean) {
        update { it.copy(backKeyHideEnabled = enabled) }
    }
    
    suspend fun setTampermonkeyEnabled(enabled: Boolean) {
        update { it.copy(tampermonkeyEnabled = enabled) }
    }
    
    suspend fun setUserAgentMode(mode: Int) {
        update { it.copy(userAgentMode = mode.coerceIn(0, 3)) }
    }
}
