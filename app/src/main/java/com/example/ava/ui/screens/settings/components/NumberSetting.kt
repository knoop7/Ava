package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun IntSetting(
    name: String,
    description: String = "",
    value: Int?,
    enabled: Boolean = true,
    validation: ((Int?) -> String?)? = null,
    onConfirmRequest: (Int?) -> Unit = {}
) {
    TextSetting(
        name = name,
        description = description,
        value = value?.toString() ?: "",
        enabled = enabled,
        validation = { validation?.invoke(it.toIntOrNull()) },
        inputTransformation = intOrEmptyInputTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        onConfirmRequest = { onConfirmRequest(it.toIntOrNull()) }
    )
}

val intOrEmptyInputTransformation: InputTransformation = InputTransformation {
    val text = toString()
    if (text.isNotEmpty()) {
        val value = text.toIntOrNull()
        if (value == null)
            revertAllChanges()
    }
}