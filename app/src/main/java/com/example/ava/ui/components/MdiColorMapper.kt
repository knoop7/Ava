package com.example.ava.ui.components

import android.graphics.Color

object MdiColorMapper {
    
    data class TileColor(
        val topColor: Int,
        val bottomColor: Int,
        val glowColor: Int
    )

    val presetColors = listOf(
        "yellow" to TileColor(Color.parseColor("#FFD60A"), Color.parseColor("#FFB800"), Color.argb(100, 255, 214, 10)),
        "orange" to TileColor(Color.parseColor("#D4956B"), Color.parseColor("#C08060"), Color.argb(100, 212, 149, 107)),
        "red" to TileColor(Color.parseColor("#C47070"), Color.parseColor("#A85858"), Color.argb(100, 196, 112, 112)),
        "warm_sand" to TileColor(Color.parseColor("#C9A96E"), Color.parseColor("#B8956A"), Color.argb(100, 201, 169, 110)),
        "dusty_rose" to TileColor(Color.parseColor("#C48B9F"), Color.parseColor("#A8707E"), Color.argb(100, 196, 139, 159)),
        "sage" to TileColor(Color.parseColor("#8FAE8B"), Color.parseColor("#7A9A76"), Color.argb(100, 143, 174, 139)),
        "terracotta" to TileColor(Color.parseColor("#C27D5F"), Color.parseColor("#A86B50"), Color.argb(100, 194, 125, 95)),
        "mauve" to TileColor(Color.parseColor("#9B8EC1"), Color.parseColor("#8578A8"), Color.argb(100, 155, 142, 193)),
        "clay" to TileColor(Color.parseColor("#B5876B"), Color.parseColor("#9E7460"), Color.argb(100, 181, 135, 107)),
        "moss" to TileColor(Color.parseColor("#7D9B76"), Color.parseColor("#6B8A64"), Color.argb(100, 125, 155, 118)),
        "peach" to TileColor(Color.parseColor("#D4956B"), Color.parseColor("#C08060"), Color.argb(100, 212, 149, 107)),
        "lavender" to TileColor(Color.parseColor("#A89CC8"), Color.parseColor("#9488B0"), Color.argb(100, 168, 156, 200)),
        "stone" to TileColor(Color.parseColor("#A0998C"), Color.parseColor("#8C857A"), Color.argb(100, 160, 153, 140)),
        "copper" to TileColor(Color.parseColor("#B87850"), Color.parseColor("#A06840"), Color.argb(100, 184, 120, 80)),
        "olive" to TileColor(Color.parseColor("#9A9A60"), Color.parseColor("#858550"), Color.argb(100, 154, 154, 96)),
        "blush" to TileColor(Color.parseColor("#D4A0A0"), Color.parseColor("#C08888"), Color.argb(100, 212, 160, 160)),
        "wheat" to TileColor(Color.parseColor("#C8B080"), Color.parseColor("#B09868"), Color.argb(100, 200, 176, 128)),
        "plum" to TileColor(Color.parseColor("#9C7EA0"), Color.parseColor("#886C8C"), Color.argb(100, 156, 126, 160)),
        "amber" to TileColor(Color.parseColor("#D4A030"), Color.parseColor("#C09028"), Color.argb(100, 212, 160, 48)),
        "bright_purple" to TileColor(Color.parseColor("#B080D0"), Color.parseColor("#9868B8"), Color.argb(100, 176, 128, 208)),
        "ivory" to TileColor(Color.parseColor("#D8D0C0"), Color.parseColor("#C8C0B0"), Color.argb(100, 216, 208, 192)),
        "coral" to TileColor(Color.parseColor("#D08070"), Color.parseColor("#B86858"), Color.argb(100, 208, 128, 112)),
        "tawny" to TileColor(Color.parseColor("#C89050"), Color.parseColor("#B07840"), Color.argb(100, 200, 144, 80)),
        "sienna" to TileColor(Color.parseColor("#A06040"), Color.parseColor("#885030"), Color.argb(100, 160, 96, 64)),
        "lilac" to TileColor(Color.parseColor("#C0A0D0"), Color.parseColor("#A888B8"), Color.argb(100, 192, 160, 208)),
        "champagne" to TileColor(Color.parseColor("#D8C898"), Color.parseColor("#C8B880"), Color.argb(100, 216, 200, 152))
    )

    val colorMap = presetColors.toMap()

    private val softColors = listOf(
        "warm_sand", "dusty_rose", "sage", "terracotta", "mauve",
        "clay", "moss", "peach", "lavender", "stone", "copper",
        "olive", "blush", "wheat", "plum", "amber", "bright_purple",
        "ivory", "coral", "tawny", "sienna", "lilac", "champagne"
    )

    fun stableRandomSoftColor(seed: String): String {
        val idx = (seed.hashCode() and 0x7FFFFFFF) % softColors.size
        return softColors[idx]
    }

    private val iconDefaultColors = mapOf(
        "lightbulb" to "yellow",
        "light" to "yellow",
        "bulb" to "yellow",
        "ceiling" to "yellow",
        "lamp" to "yellow",
        "led" to "yellow",
        "flash" to "yellow",
        "energy" to "yellow",
        "brightness" to "yellow",
        "lux" to "yellow",
        "illuminance" to "yellow",
        "smoke" to "red",
        "fire" to "orange"
    )

    fun getDefaultColorForIcon(icon: String): String {
        val iconName = icon.removePrefix("mdi:").replace("-", "_").lowercase()
        for ((key, color) in iconDefaultColors) {
            if (iconName.contains(key)) return color
        }
        return stableRandomSoftColor(iconName)
    }

    fun getTileColor(colorName: String): TileColor {
        return colorMap[colorName] ?: colorMap["warm_sand"]!!
    }

    fun getTileColorForIcon(icon: String, customColor: String? = null): TileColor {
        if (customColor != null && customColor.startsWith("#")) {
            return try {
                val c = android.graphics.Color.parseColor(customColor)
                TileColor(c, c, c)
            } catch (e: Exception) {
                val colorName = getDefaultColorForIcon(icon)
                getTileColor(colorName)
            }
        }
        val colorName = customColor ?: getDefaultColorForIcon(icon)
        return getTileColor(colorName)
    }

    private val sensorTypeColors = mapOf(
        "illuminance" to "yellow",
        "lux" to "yellow",
        "pm25" to "red",
        "pm10" to "red",
        "smoke" to "red",
        "gas" to "red"
    )

    fun getColorForSensorType(sensorType: String): TileColor {
        val colorName = sensorTypeColors[sensorType.lowercase()] ?: stableRandomSoftColor(sensorType)
        return getTileColor(colorName)
    }
}
