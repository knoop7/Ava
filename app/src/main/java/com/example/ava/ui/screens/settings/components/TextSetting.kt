package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map

@Composable
fun TextSetting(
    name: String,
    description: String = "",
    value: String,
    placeholder: String = "",
    enabled: Boolean = true,
    validation: ((String) -> String?)? = null,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onConfirmRequest: (String) -> Unit = {}
) {
    DialogSettingItem(
        name = name,
        description = description,
        value = value,
        enabled = enabled
    ) {
        TextDialog(
            title = name,
            description = "",
            value = value,
            placeholder = placeholder,
            onConfirmRequest = onConfirmRequest,
            validation = validation,
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions
        )
    }
}

@Composable
fun DialogScope.TextDialog(
    title: String = "",
    description: String = "",
    value: String = "",
    placeholder: String = "",
    onConfirmRequest: (String) -> Unit,
    validation: ((String) -> String?)? = null,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val textFieldState = rememberTextFieldState(value)
    val validationState by snapshotFlow { textFieldState.text }
        .map { validation?.invoke(it.toString()) }
        .collectAsStateWithLifecycle(null)
    ActionDialog(
        title = title,
        description = description,
        confirmEnabled = validationState.isNullOrBlank(),
        onConfirmRequest = {
            onConfirmRequest(textFieldState.text.toString())
        }
    ) {
        ValidatedTextField(
            state = textFieldState,
            placeholder = placeholder,
            isValid = validationState.isNullOrBlank(),
            validationText = validationState ?: "",
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions
        )
    }
}

@Composable
fun ValidatedTextField(
    state: TextFieldState,
    label: String = "",
    placeholder: String = "",
    isValid: Boolean = true,
    validationText: String = "",
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    
    val accentColor = Color(0xFF4F46E5) 
    val labelColor = Color(0xFF334155) 
    val subLabelColor = Color(0xFF94A3B8) 
    
    TextField(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        placeholder = {
            Text(
                text = placeholder,
                color = subLabelColor,
                fontSize = 14.sp
            )
        },
        isError = !isValid,
        supportingText = if (validationText.isNotEmpty()) {
             @Composable { Text(text = validationText, fontSize = 12.sp) }
        } else null,
        inputTransformation = inputTransformation,
        keyboardOptions = keyboardOptions,
        lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = labelColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            errorContainerColor = Color(0xFFFEF2F2),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = accentColor
        )
    )
}