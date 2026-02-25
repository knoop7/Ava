package com.example.ava.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch

@Composable
fun DiagnosticSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val experimentalState by viewModel.experimentalSettingsState.collectAsStateWithLifecycle(null)
    
    fun restartService() {
        context.stopService(Intent(context, VoiceSatelliteService::class.java))
        val intent = Intent(context, VoiceSatelliteService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_diagnostic_sensor)
    ) {
        item {
            SimpleCard {
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_wifi)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticWifiEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticWifiEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_ip)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticIpEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticIpEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_storage)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticStorageEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticStorageEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_memory)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticMemoryEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticMemoryEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_uptime)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticUptimeEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticUptimeEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_kill_app)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticKillAppEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticKillAppEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_reboot)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticRebootEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticRebootEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_battery_level)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticBatteryLevelEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticBatteryLevelEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_battery_voltage)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticBatteryVoltageEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticBatteryVoltageEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_charging_status)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.diagnosticChargingStatusEnabled ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveDiagnosticChargingStatusEnabled(it)
                                restartService()
                            }
                        }
                    )
                }
            }
        }
    }
}
