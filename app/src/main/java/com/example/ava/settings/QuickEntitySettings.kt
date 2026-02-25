package com.example.ava.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class QuickEntitySlot(
    val entityId: String = "",
    val entityType: String = "switch",
    val icon: String = "mdi:home-assistant",
    val label: String = "",
    val size: String = "1x1",
    val color: String = ""
)

@Serializable
data class QuickEntitySettings(
    val enableQuickEntity: Boolean = false,
    val enableQuickEntityDisplay: Boolean = false,
    val enableHaSlots: Boolean = false,
    val slots: List<QuickEntitySlot> = List(6) { QuickEntitySlot() }
)

val Context.quickEntitySettingsStore: DataStore<QuickEntitySettings> by dataStore(
    fileName = "quick_entity_settings.json",
    serializer = SettingsSerializer(QuickEntitySettings.serializer(), QuickEntitySettings()),
    corruptionHandler = defaultCorruptionHandler(QuickEntitySettings())
)

class QuickEntitySettingsStore(dataStore: DataStore<QuickEntitySettings>) :
    SettingsStoreImpl<QuickEntitySettings>(dataStore, QuickEntitySettings()) {
    
    val enableQuickEntity = SettingState(getFlow().map { it.enableQuickEntity }) { value ->
        update { it.copy(enableQuickEntity = value) }
    }
    
    val enableQuickEntityDisplay = SettingState(getFlow().map { it.enableQuickEntityDisplay }) { value ->
        update { it.copy(enableQuickEntityDisplay = value) }
    }
    
    val enableHaSlots = SettingState(getFlow().map { it.enableHaSlots }) { value ->
        update { it.copy(enableHaSlots = value) }
    }
    
    val slots = SettingState(getFlow().map { it.slots }) { value ->
        update { it.copy(slots = value) }
    }
    
    suspend fun updateSlot(index: Int, slot: QuickEntitySlot) {
        update { settings ->
            val newSlots = settings.slots.toMutableList()
            if (index in newSlots.indices) {
                newSlots[index] = slot
            }
            settings.copy(slots = newSlots)
        }
    }
    
    fun slotEntityId(index: Int) = SettingState(
        getFlow().map { it.slots.getOrNull(index)?.entityId ?: "" }
    ) { entityId ->
        update { settings ->
            val newSlots = settings.slots.toMutableList()
            if (index in newSlots.indices) {
                if (entityId.isEmpty()) {
                    newSlots[index] = QuickEntitySlot()
                } else {
                    val type = when {
                        entityId.startsWith("sensor.") || entityId.startsWith("binary_sensor.") -> "sensor"
                        entityId.startsWith("button.") || entityId.startsWith("script.") || entityId.startsWith("scene.") -> "button"
                        else -> "switch"
                    }
                    val name = entityId.substringAfter(".").lowercase()
                    val icon = guessIconFromEntityId(entityId, name)
                    val rawName = entityId.substringAfter(".")
                    val fullLabel = rawName
                        .replace("_", " ")
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        .trim()
                        .ifEmpty { rawName }
                    val hasChinese = fullLabel.any { it.code > 0x4E00 && it.code < 0x9FFF }
                    val maxLen = if (hasChinese) 7 else 20
                    val label = if (fullLabel.length > maxLen) fullLabel.take(maxLen) else fullLabel
                    val autoColor = if (icon.contains("lightbulb") || icon.contains("lamp") || icon.contains("ceiling-light") || icon.contains("led-strip") || icon.contains("floor-lamp") || icon.contains("brightness")) {
                        "yellow"
                    } else {
                        com.example.ava.ui.components.MdiColorMapper.stableRandomSoftColor(entityId)
                    }
                    newSlots[index] = newSlots[index].copy(
                        entityId = entityId,
                        entityType = type,
                        icon = icon,
                        label = label,
                        color = autoColor
                    )
                }
            }
            settings.copy(slots = newSlots)
        }
    }
    
    private fun guessIconFromEntityId(entityId: String, name: String): String {
        val keywordIcons = listOf(
            "humidity" to "mdi:water-percent",
            "temperature" to "mdi:thermometer",
            "temp" to "mdi:thermometer",
            "illuminance" to "mdi:brightness-5",
            "lux" to "mdi:brightness-5",
            "power" to "mdi:flash",
            "energy" to "mdi:flash",
            "motion" to "mdi:motion-sensor",
            "occupancy" to "mdi:motion-sensor",
            "door" to "mdi:door",
            "window" to "mdi:window-closed",
            "smoke" to "mdi:smoke-detector",
            "fire" to "mdi:fire",
            "water" to "mdi:water",
            "moisture" to "mdi:water",
            "wifi" to "mdi:wifi",
            "bluetooth" to "mdi:bluetooth",
            "fan" to "mdi:fan",
            "light" to "mdi:lightbulb",
            "lamp" to "mdi:lamp",
            "bulb" to "mdi:lightbulb",
            "ceiling" to "mdi:ceiling-light",
            "led" to "mdi:led-strip",
            "floor_lamp" to "mdi:floor-lamp",
            "lock" to "mdi:lock",
            "plug" to "mdi:power-plug",
            "socket" to "mdi:power-socket",
            "camera" to "mdi:camera",
            "tv" to "mdi:television",
            "television" to "mdi:television",
            "speaker" to "mdi:speaker",
            "vacuum" to "mdi:robot-vacuum",
            "curtain" to "mdi:curtains",
            "blind" to "mdi:blinds",
            "garage" to "mdi:garage",
            "ac" to "mdi:air-conditioner",
            "air_condition" to "mdi:air-conditioner",
            "washer" to "mdi:washer",
            "washing" to "mdi:washing-machine",
            "fridge" to "mdi:fridge",
            "kettle" to "mdi:kettle",
            "bell" to "mdi:bell",
            "flower" to "mdi:flower",
            "cat" to "mdi:cat",
            "dog" to "mdi:dog",
            "shield" to "mdi:shield-check",
            "account" to "mdi:account",
            "person" to "mdi:account",
            "location" to "mdi:map-marker",
            "gps" to "mdi:map-marker"
        )
        for ((keyword, icon) in keywordIcons) {
            if (name.contains(keyword)) return icon
        }
        return when {
            entityId.startsWith("light.") -> "mdi:lightbulb"
            entityId.startsWith("switch.") -> "mdi:power"
            entityId.startsWith("fan.") -> "mdi:fan"
            entityId.startsWith("cover.") -> "mdi:blinds"
            entityId.startsWith("climate.") -> "mdi:air-conditioner"
            entityId.startsWith("button.") -> "mdi:gesture-tap-button"
            entityId.startsWith("script.") -> "mdi:gesture-tap-button"
            entityId.startsWith("scene.") -> "mdi:home"
            entityId.startsWith("camera.") -> "mdi:camera"
            entityId.startsWith("lock.") -> "mdi:lock"
            else -> "mdi:home-assistant"
        }
    }

    suspend fun clearAllSlots() {
        update { settings ->
            settings.copy(slots = List(6) { QuickEntitySlot() })
        }
    }
}
