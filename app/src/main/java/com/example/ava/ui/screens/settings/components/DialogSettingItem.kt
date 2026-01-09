package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DialogSettingItem(
    name: String,
    description: String = "",
    value: String,
    enabled: Boolean = true,
    content: @Composable DialogScope.() -> Unit
) {
    val dialogScope = remember { DialogScope() }
    val isDialogOpen by dialogScope.isDialogOpen.collectAsStateWithLifecycle()
    val modifier =
        if (enabled) Modifier.clickable { dialogScope.openDialog() } else Modifier.alpha(0.5f)
    SettingItem(
        modifier = modifier,
        name = name,
        description = description,
        value = value
    )
    if (isDialogOpen) {
        content(dialogScope)
    }
}