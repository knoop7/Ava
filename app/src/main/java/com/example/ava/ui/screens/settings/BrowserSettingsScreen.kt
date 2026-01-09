package com.example.ava.ui.screens.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
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
    
    
    val pullRefreshEnabled by browserSettingsStore.pullRefreshEnabled.collectAsState(initial = true)
    val initialScale by browserSettingsStore.initialScale.collectAsState(initial = 0)
    val fontSize by browserSettingsStore.fontSize.collectAsState(initial = 100)
    val touchEnabled by browserSettingsStore.touchEnabled.collectAsState(initial = true)
    val dragEnabled by browserSettingsStore.dragEnabled.collectAsState(initial = true)
    val hardwareAcceleration by browserSettingsStore.hardwareAcceleration.collectAsState(initial = true)
    val settingsButtonEnabled by browserSettingsStore.settingsButtonEnabled.collectAsState(initial = false)
    val advancedControlEnabled by browserSettingsStore.advancedControlEnabled.collectAsState(initial = false)
    val backKeyHideEnabled by browserSettingsStore.backKeyHideEnabled.collectAsState(initial = true)
    var showAdvancedHelpDialog by remember { mutableStateOf(false) }
    
    val renderMode = if (hardwareAcceleration) RenderMode.HARDWARE else RenderMode.SOFTWARE
    
    val hardwareLabel = stringResource(R.string.settings_browser_render_hardware)
    val softwareLabel = stringResource(R.string.settings_browser_render_software)
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_browser)
    ) {
        item {
            SimpleCard {
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
            }
        }
    }
    
    
    val helpContent = stringResource(R.string.advanced_control_help_content)
    val helpTitle = stringResource(R.string.advanced_control_help_title)
    val copyText = stringResource(R.string.advanced_control_copy)
    val dismissText = stringResource(R.string.advanced_control_dismiss)
    val copiedText = stringResource(R.string.copied_to_clipboard)
    val aiUrl = "https://gemini.google.com/gem/ee3cb858f9d0"
    
    if (showAdvancedHelpDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAdvancedHelpDialog = false },
            title = {
                androidx.compose.material3.Text(
                    text = helpTitle,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            },
            text = {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    androidx.compose.foundation.lazy.LazyColumn {
                        item {
                            androidx.compose.material3.Text(
                                text = helpContent.replace(aiUrl, ""),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        item {
                            androidx.compose.foundation.text.ClickableText(
                                text = androidx.compose.ui.text.AnnotatedString(aiUrl),
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF4F46E5)
                                ),
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("AI URL", aiUrl)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "AI URL copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAdvancedHelpDialog = false }) {
                    androidx.compose.material3.Text(dismissText)
                }
            }
        )
    }
}
