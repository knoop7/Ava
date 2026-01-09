package com.example.ava.ui.screens.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


@Composable
fun ScreensaverSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val screensaverState by viewModel.screensaverSettingsState.collectAsStateWithLifecycle(null)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val enabled = screensaverState != null
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_screensaver)
    ) {
        item {
            SimpleCard {
                
                SettingRow(
                    label = stringResource(R.string.settings_screensaver_enable),
                    subLabel = stringResource(R.string.settings_screensaver_enable_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.enabled ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveScreensaverEnabled(it)
                            }
                        }
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                
                SettingRow(
                    label = stringResource(R.string.settings_xiaomi_wallpaper),
                    subLabel = stringResource(R.string.settings_xiaomi_wallpaper_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.xiaomiWallpaperEnabled ?: false,
                        enabled = enabled && (screensaverState?.enabled == true),
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveXiaomiWallpaperEnabled(it)
                            }
                        }
                    )
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                TextSetting(
                    name = stringResource(R.string.settings_screensaver_url),
                    description = stringResource(R.string.settings_screensaver_url_desc),
                    value = screensaverState?.screensaverUrl ?: "",
                    enabled = enabled && (screensaverState?.enabled == true) && !(screensaverState?.xiaomiWallpaperEnabled ?: false),
                    onConfirmRequest = { 
                        coroutineScope.launch {
                            viewModel.saveScreensaverUrl(it)
                        }
                    }
                )
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                SettingRow(
                    label = stringResource(R.string.settings_screensaver_url_visible),
                    subLabel = stringResource(R.string.settings_screensaver_url_visible_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.screensaverUrlVisible ?: false,
                        enabled = enabled && (screensaverState?.enabled == true),
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveScreensaverUrlVisible(it)
                            }
                        }
                    )
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                IntSetting(
                    name = stringResource(R.string.settings_screensaver_timeout),
                    description = stringResource(R.string.settings_screensaver_timeout_desc),
                    value = screensaverState?.timeoutSeconds,
                    enabled = enabled && (screensaverState?.enabled == true),
                    validation = { value -> if (value != null && value in 10..3600) null else context.getString(R.string.validation_range, 10, 3600) },
                    onConfirmRequest = { 
                        coroutineScope.launch {
                            if (it != null) {
                                viewModel.saveScreensaverTimeout(it)
                            }
                        }
                    }
                )
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                SettingRow(
                    label = stringResource(R.string.settings_screensaver_dark_off),
                    subLabel = stringResource(R.string.settings_screensaver_dark_off_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.darkOffEnabled ?: false,
                        enabled = enabled && (screensaverState?.enabled == true),
                        onCheckedChange = { 
                            coroutineScope.launch {
                                viewModel.saveScreensaverDarkOff(it)
                            }
                        }
                    )
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                SettingRow(
                    label = stringResource(R.string.settings_screensaver_background_pause),
                    subLabel = stringResource(R.string.settings_screensaver_background_pause_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.backgroundPauseEnabled ?: false,
                        enabled = enabled && (screensaverState?.enabled == true),
                        onCheckedChange = { 
                            coroutineScope.launch {
                                viewModel.saveScreensaverBackgroundPause(it)
                            }
                        }
                    )
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                SettingRow(
                    label = stringResource(R.string.settings_screensaver_motion_on),
                    subLabel = stringResource(R.string.settings_screensaver_motion_on_desc)
                ) {
                    ModernSwitch(
                        checked = screensaverState?.motionOnEnabled ?: false,
                        enabled = enabled && (screensaverState?.enabled == true),
                        onCheckedChange = { 
                            coroutineScope.launch {
                                viewModel.saveScreensaverMotionOn(it)
                            }
                        }
                    )
                }
            }
        }
    }
}
