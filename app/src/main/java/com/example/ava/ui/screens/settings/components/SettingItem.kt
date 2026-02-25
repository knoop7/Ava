package com.example.ava.ui.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.home.KEY_DARK_MODE
import com.example.ava.ui.screens.home.PREFS_NAME

private val LabelColorLight = Color(0xFF334155)
private val LabelColorDark = Color(0xFFF1F5F9)
private val SubLabelColor = Color(0xFF94A3B8)
private val ValueColor = Color(0xFF64748B)

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String = "",
    value: String = "",
    icon: Painter? = null,
    action: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)
    val labelColor = if (isDarkMode) LabelColorDark else LabelColorLight
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = name,
                tint = SubLabelColor, 
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = labelColor, 
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = SubLabelColor, 
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    color = ValueColor, 
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(15.dp))
        
        action()
    }
}

@Composable
fun RowScope.Details(name: String, description: String = "", value: String = "") {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)
    val labelColor = if (isDarkMode) LabelColorDark else LabelColorLight
    
    Column(Modifier.weight(1f)) {
        Text(
            text = name,
            color = labelColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = SubLabelColor,
                fontSize = 12.sp
            )
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = ValueColor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ActionContainer(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
