package com.example.ava.ui.screens.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
                                if (!it) {
                                    kotlinx.coroutines.delay(100)
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                            }
                        }
                    )
                }
                
                if (screensaverState?.enabled == true) {
                    SettingsDivider()

                    SettingRow(
                        label = stringResource(R.string.settings_screensaver_ha_display),
                        subLabel = stringResource(R.string.settings_screensaver_ha_display_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.enableHaDisplay ?: false,
                            enabled = enabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.saveScreensaverHaDisplay(it)
                                    kotlinx.coroutines.delay(100)
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                            }
                        )
                    }

                    SettingsDivider()

                    IntSetting(
                        name = stringResource(R.string.settings_screensaver_timeout),
                        description = stringResource(R.string.settings_screensaver_timeout_desc),
                        value = screensaverState?.timeoutSeconds,
                        enabled = enabled,
                        validation = { value -> if (value != null && value in 10..3600) null else context.getString(R.string.validation_range, 10, 3600) },
                        onConfirmRequest = { 
                            coroutineScope.launch {
                                if (it != null) {
                                    viewModel.saveScreensaverTimeout(it)
                                }
                            }
                        }
                    )
                }
            }
        }
        

        if (screensaverState?.enabled == true) {
            item {
                SimpleCard {
                    SettingRow(
                        label = stringResource(R.string.settings_xiaomi_wallpaper),
                        subLabel = stringResource(R.string.settings_xiaomi_wallpaper_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.xiaomiWallpaperEnabled ?: false,
                            enabled = enabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.saveXiaomiWallpaperEnabled(it)
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    TextSetting(
                        name = stringResource(R.string.settings_screensaver_url),
                        description = stringResource(R.string.settings_screensaver_url_desc),
                        dialogHint = stringResource(R.string.settings_screensaver_url_hint),
                        value = screensaverState?.screensaverUrl ?: "",
                        enabled = enabled && !(screensaverState?.xiaomiWallpaperEnabled ?: false),
                        onConfirmRequest = { 
                            coroutineScope.launch {
                                viewModel.saveScreensaverUrl(it)
                                kotlinx.coroutines.delay(100)
                                com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                            }
                        }
                    )
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_screensaver_url_visible),
                        subLabel = stringResource(R.string.settings_screensaver_url_visible_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.screensaverUrlVisible ?: false,
                            enabled = enabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.saveScreensaverUrlVisible(it)
                                    kotlinx.coroutines.delay(100)
                                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                                }
                            }
                        )
                    }
                }
            }
        

            item {
                SimpleCard {
                    SettingRow(
                        label = stringResource(R.string.settings_screensaver_dark_off),
                        subLabel = stringResource(R.string.settings_screensaver_dark_off_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.darkOffEnabled ?: false,
                            enabled = enabled,
                            onCheckedChange = { 
                                coroutineScope.launch {
                                    viewModel.saveScreensaverDarkOff(it)
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_screensaver_background_pause),
                        subLabel = stringResource(R.string.settings_screensaver_background_pause_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.backgroundPauseEnabled ?: false,
                            enabled = enabled,
                            onCheckedChange = { 
                                coroutineScope.launch {
                                    viewModel.saveScreensaverBackgroundPause(it)
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    SettingRow(
                        label = stringResource(R.string.settings_screensaver_motion_on),
                        subLabel = stringResource(R.string.settings_screensaver_motion_on_desc)
                    ) {
                        ModernSwitch(
                            checked = screensaverState?.motionOnEnabled ?: false,
                            enabled = enabled,
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
}
