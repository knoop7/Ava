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
import androidx.compose.runtime.remember
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.home.KEY_DARK_MODE
import com.example.ava.ui.screens.home.PREFS_NAME

@Composable
fun TextSetting(
    name: String,
    description: String = "",
    dialogHint: String = "",
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
            description = dialogHint,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)
    
    val accentColor = if (isDarkMode) Color(0xFFA78B73) else Color(0xFF0417E0) 
    val labelColor = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF334155)
    val subLabelColor = Color(0xFF94A3B8)
    val containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF8FAFC)
    
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
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            errorContainerColor = Color(0xFFFEF2F2),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = accentColor
        )
    )
}
