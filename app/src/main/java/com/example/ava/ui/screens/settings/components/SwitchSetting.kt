package com.example.ava.ui.screens.settings.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

@Composable
fun SwitchSetting(
    name: String,
    description: String,
    value: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val modifier = if (enabled) Modifier else Modifier.alpha(0.5f)
    SettingItem(
        name = name,
        description = description,
        modifier = modifier
    ) {
        Switch(
            checked = value,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}