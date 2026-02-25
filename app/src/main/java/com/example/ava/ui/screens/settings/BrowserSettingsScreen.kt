package com.example.ava.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.settings.BrowserSettingsStore
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch


enum class RenderMode(val displayName: String) {
    HARDWARE("Hardware"),
    SOFTWARE("Software")
}


@Composable
fun BrowserSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserSettingsStore = BrowserSettingsStore(context)
    
    
    val haRemoteUrlEnabled by browserSettingsStore.haRemoteUrlEnabled.collectAsState(initial = true)
    val enableBrowserDisplay by browserSettingsStore.enableBrowserDisplay.collectAsState(initial = false)
    val pullRefreshEnabled by browserSettingsStore.pullRefreshEnabled.collectAsState(initial = true)
    val initialScale by browserSettingsStore.initialScale.collectAsState(initial = 0)
    val fontSize by browserSettingsStore.fontSize.collectAsState(initial = 100)
    val touchEnabled by browserSettingsStore.touchEnabled.collectAsState(initial = true)
    val dragEnabled by browserSettingsStore.dragEnabled.collectAsState(initial = true)
    val hardwareAcceleration by browserSettingsStore.hardwareAcceleration.collectAsState(initial = true)
    val settingsButtonEnabled by browserSettingsStore.settingsButtonEnabled.collectAsState(initial = false)
    val advancedControlEnabled by browserSettingsStore.advancedControlEnabled.collectAsState(initial = false)
    val backKeyHideEnabled by browserSettingsStore.backKeyHideEnabled.collectAsState(initial = true)
    val tampermonkeyEnabled by browserSettingsStore.tampermonkeyEnabled.collectAsState(initial = false)
    val userAgentMode by browserSettingsStore.userAgentMode.collectAsState(initial = 0)
    var showAdvancedHelpDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    
    val renderMode = if (hardwareAcceleration) RenderMode.HARDWARE else RenderMode.SOFTWARE
    
    val hardwareLabel = stringResource(R.string.settings_browser_render_hardware)
    val softwareLabel = stringResource(R.string.settings_browser_render_software)
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_browser)
    ) {
        item {
            SimpleCard {
                
                val hasOverlayPermission = checkOverlayPermission(context)
                SettingRow(
                    label = stringResource(R.string.settings_browser_ha_remote_url),
                    subLabel = if (hasOverlayPermission)
                        stringResource(R.string.settings_browser_ha_remote_url_desc)
                    else
                        stringResource(R.string.settings_overlay_permission_required)
                ) {
                    ModernSwitch(
                        checked = haRemoteUrlEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasOverlayPermission) {
                                requestOverlayPermission(context)
                            } else {
                                scope.launch { 
                                    browserSettingsStore.setHaRemoteUrlEnabled(enabled)
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                            }
                        }
                    )
                }
                
                if (haRemoteUrlEnabled) {
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_browser_ha_display_switch),
                        subLabel = stringResource(R.string.settings_browser_ha_display_switch_desc)
                    ) {
                        ModernSwitch(
                            checked = enableBrowserDisplay,
                            onCheckedChange = { enabled ->
                                scope.launch { 
                                    browserSettingsStore.setEnableBrowserDisplay(enabled)
                                    if (!enabled) {
                                        com.example.ava.services.WebViewService.destroy(context)
                                    }
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_browser_advanced_control),
                        subLabel = stringResource(R.string.settings_browser_advanced_control_desc)
                    ) {
                        ModernSwitch(
                            checked = advancedControlEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { 
                                    browserSettingsStore.setAdvancedControlEnabled(enabled)
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                                if (enabled) {
                                    showAdvancedHelpDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (haRemoteUrlEnabled) {
            item {
                SimpleCard {
                    SettingRow(
                        label = stringResource(R.string.settings_browser_tampermonkey),
                        subLabel = stringResource(R.string.settings_browser_tampermonkey_desc)
                    ) {
                        ModernSwitch(
                            checked = tampermonkeyEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { 
                                    browserSettingsStore.setTampermonkeyEnabled(enabled)
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    val userAgentLabels = listOf(
                        stringResource(R.string.useragent_default),
                        stringResource(R.string.useragent_desktop),
                        stringResource(R.string.useragent_macos),
                        stringResource(R.string.useragent_ios)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUserAgentDialog = true }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_browser_useragent),
                                color = getTitleColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = userAgentLabels.getOrElse(userAgentMode) { userAgentLabels[0] },
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            
            item {
                SimpleCard {
                    SettingRow(
                        label = stringResource(R.string.settings_browser_pull_refresh),
                        subLabel = stringResource(R.string.settings_browser_pull_refresh_desc)
                    ) {
                        ModernSwitch(
                            checked = pullRefreshEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { browserSettingsStore.setPullRefreshEnabled(enabled) }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    IntSetting(
                        name = stringResource(R.string.settings_browser_initial_scale),
                        description = stringResource(R.string.settings_browser_initial_scale_desc),
                        value = initialScale,
                        enabled = true,
                        validation = { value -> if (value != null && value in 0..500) null else context.getString(R.string.validation_range, 0, 500) },
                        onConfirmRequest = { value ->
                            value?.let { scope.launch { browserSettingsStore.setInitialScale(it) } }
                        }
                    )
                    
                    SettingsDivider()
                    
                    IntSetting(
                        name = stringResource(R.string.settings_browser_font_size),
                        description = stringResource(R.string.settings_browser_font_size_desc),
                        value = fontSize,
                        enabled = true,
                        validation = { value -> if (value != null && value in 50..300) null else context.getString(R.string.validation_range, 50, 300) },
                        onConfirmRequest = { value ->
                            value?.let { scope.launch { browserSettingsStore.setFontSize(it) } }
                        }
                    )
                    
                    SettingsDivider()
                    
                    SelectSetting(
                        name = stringResource(R.string.settings_browser_render_mode),
                        selected = renderMode,
                        items = RenderMode.entries.toList(),
                        enabled = true,
                        key = { it.name },
                        value = { 
                            when (it) {
                                RenderMode.HARDWARE -> hardwareLabel
                                RenderMode.SOFTWARE -> softwareLabel
                                else -> ""
                            }
                        },
                        onConfirmRequest = { mode ->
                            scope.launch { browserSettingsStore.setHardwareAcceleration(mode == RenderMode.HARDWARE) }
                        }
                    )
                }
            }
            
            item {
                SimpleCard {
                    SettingRow(
                        label = stringResource(R.string.settings_browser_touch),
                        subLabel = stringResource(R.string.settings_browser_touch_desc)
                    ) {
                        ModernSwitch(
                            checked = touchEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { browserSettingsStore.setTouchEnabled(enabled) }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_browser_drag),
                        subLabel = stringResource(R.string.settings_browser_drag_desc)
                    ) {
                        ModernSwitch(
                            checked = dragEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { browserSettingsStore.setDragEnabled(enabled) }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_browser_settings_button),
                        subLabel = stringResource(R.string.settings_browser_settings_button_desc)
                    ) {
                        ModernSwitch(
                            checked = settingsButtonEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { browserSettingsStore.setSettingsButtonEnabled(enabled) }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_browser_back_key_hide),
                        subLabel = stringResource(R.string.settings_browser_back_key_hide_desc)
                    ) {
                        ModernSwitch(
                            checked = backKeyHideEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { browserSettingsStore.setBackKeyHideEnabled(enabled) }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.webkit.WebStorage.getInstance().deleteAllData()
                                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                android.webkit.CookieManager.getInstance().flush()
                                android.widget.Toast.makeText(context, context.getString(R.string.settings_browser_cache_cleared), android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_browser_clear_cache),
                                color = getLabelColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.settings_browser_clear_cache_desc),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.advanced_control_help_title)
                    ) {
                        TextButton(onClick = { showAdvancedHelpDialog = true }) {
                            Text("?", color = getAccentColor())
                        }
                    }
                }
            }
        }
    }
    
    
    val helpTitle = stringResource(R.string.advanced_control_help_title)
    val helpDesc = stringResource(R.string.advanced_control_help_desc)
    val callTitle = stringResource(R.string.advanced_control_call_title)
    val callContent = stringResource(R.string.advanced_control_call_content)
    val commandsTitle = stringResource(R.string.advanced_control_commands_title)
    val commandsContent = stringResource(R.string.advanced_control_commands_content)
    val dismissText = stringResource(R.string.advanced_control_dismiss)
    val copiedText = stringResource(R.string.copied_to_clipboard)
    val aiUrl = "https://gemini.google.com/gem/ee3cb858f9d0"
    
    if (showAdvancedHelpDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAdvancedHelpDialog = false },
            title = {
                Text(
                    text = helpTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = getTitleColor()
                )
            },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = helpDesc,
                            fontSize = 13.sp,
                            color = getLabelColor(),
                            lineHeight = 18.sp
                        )
                    }
                    item {
                        Text(
                            text = callTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = getTitleColor()
                        )
                    }
                    item {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = callContent,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 17.sp
                            )
                        }
                    }
                    item {
                        Text(
                            text = commandsTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = getTitleColor()
                        )
                    }
                    item {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = commandsContent,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 17.sp
                            )
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.advanced_control_ai_link_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = getTitleColor()
                        )
                    }
                    item {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            androidx.compose.foundation.text.ClickableText(
                                text = androidx.compose.ui.text.AnnotatedString(aiUrl),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = getAccentColor(),
                                    lineHeight = 17.sp
                                ),
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("AI URL", aiUrl)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, copiedText, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdvancedHelpDialog = false }) {
                    Text(
                        text = dismissText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = getAccentColor()
                    )
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            containerColor = getDialogBackground()
        )
    }
    
    if (showUserAgentDialog) {
        val options = listOf(
            stringResource(R.string.useragent_default),
            stringResource(R.string.useragent_desktop),
            stringResource(R.string.useragent_macos),
            stringResource(R.string.useragent_ios)
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUserAgentDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_browser_useragent),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = getTitleColor()
                )
            },
            text = {
                Column {
                    options.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { browserSettingsStore.setUserAgentMode(index) }
                                    showUserAgentDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = userAgentMode == index,
                                onClick = {
                                    scope.launch { browserSettingsStore.setUserAgentMode(index) }
                                    showUserAgentDialog = false
                                },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = getAccentColor(),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = getTitleColor()
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            containerColor = getDialogBackground()
        )
    }
}
