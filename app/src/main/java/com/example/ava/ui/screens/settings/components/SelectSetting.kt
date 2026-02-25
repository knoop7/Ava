package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.home.KEY_DARK_MODE
import com.example.ava.ui.screens.home.PREFS_NAME

@Composable
fun <T> SelectSetting(
    name: String,
    description: String = "",
    selected: T?,
    items: List<T>?,
    enabled: Boolean = true,
    key: ((T) -> Any)? = null,
    value: (T?) -> String = { it.toString() },
    onConfirmRequest: (T?) -> Unit = {}
) {
    DialogSettingItem(
        name = name,
        description = description,
        value = value(selected),
        enabled = enabled,
    ) {
        SelectDialog(
            title = name,
            description = description,
            selected = selected,
            items = items,
            key = key,
            value = value,
            onConfirmRequest = onConfirmRequest
        )
    }
}

@Composable
fun <T> DialogScope.SelectDialog(
    title: String = "",
    description: String = "",
    selected: T?,
    items: List<T>?,
    key: ((T) -> Any)? = null,
    value: (T) -> String = { it.toString() },
    onConfirmRequest: (T?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)
    
    val accentColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFFA78B73) else androidx.compose.ui.graphics.Color(0xFF0417E0) 
    val labelColor = if (isDarkMode) androidx.compose.ui.graphics.Color(0xFFF1F5F9) else androidx.compose.ui.graphics.Color(0xFF334155)
    
    var selectedItem by remember { mutableStateOf(selected) }
    ActionDialog(
        title = title,
        description = description,
        onConfirmRequest = {
            onConfirmRequest(selectedItem)
        }
    ) {
        if (items != null) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(
                    items = items,
                    key = key
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .selectable(
                                selected = (item == selectedItem),
                                onClick = { selectedItem = item },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            selected = item == selectedItem,
                            onClick = null,
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = labelColor.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = value(item),
                            fontSize = 14.sp,
                            color = if (item == selectedItem) accentColor else labelColor
                        )
                    }
                }
            }
        }
    }
}
