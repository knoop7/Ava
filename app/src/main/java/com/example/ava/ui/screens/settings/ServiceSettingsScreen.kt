package com.example.ava.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.ui.screens.settings.components.*
import com.example.ava.utils.BatteryOptimizationHelper
import com.example.ava.utils.DeviceCapabilities
import kotlinx.coroutines.launch


@Composable
fun ServiceSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val comingSoonText = stringResource(R.string.settings_coming_soon)
    val coroutineScope = rememberCoroutineScope()
    
    
    val experimentalState by viewModel.experimentalSettingsState.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerSettingsState.collectAsStateWithLifecycle(null)
    
    
    
    fun restartService() {
        context.stopService(Intent(context, VoiceSatelliteService::class.java))
        coroutineScope.launch {
            kotlinx.coroutines.delay(600)
            val intent = Intent(context, VoiceSatelliteService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    
    fun showComingSoon() {
        Toast.makeText(context, comingSoonText, Toast.LENGTH_SHORT).show()
    }
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_service)
    ) {
        item {
            SimpleCard {
                
                SettingRow(
                    label = stringResource(R.string.settings_auto_restart),
                    subLabel = stringResource(R.string.settings_auto_restart_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableAutoRestart ?: false,
                        onCheckedChange = {
                            if (it && !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
                                val intent = BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                                context.startActivity(intent)
                            }
                            coroutineScope.launch {
                                viewModel.saveAutoRestart(it)
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                
                val hasWriteSettings = android.provider.Settings.System.canWrite(context)
                SettingRow(
                    label = stringResource(R.string.settings_screen_brightness),
                    subLabel = if (hasWriteSettings) 
                        stringResource(R.string.settings_screen_brightness_desc)
                    else 
                        stringResource(R.string.settings_write_settings_permission_required)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.screenBrightnessEnabled ?: false,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasWriteSettings) {
                                
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveScreenBrightnessEnabled(enabled)
                                    restartService()
                                }
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                
                val diagnosticEnabled = experimentalState?.diagnosticSensorEnabled ?: false
                val enabledCount = listOf(
                    experimentalState?.diagnosticWifiEnabled ?: false,
                    experimentalState?.diagnosticIpEnabled ?: false,
                    experimentalState?.diagnosticStorageEnabled ?: false,
                    experimentalState?.diagnosticMemoryEnabled ?: false,
                    experimentalState?.diagnosticUptimeEnabled ?: false,
                    experimentalState?.diagnosticKillAppEnabled ?: false,
                    experimentalState?.diagnosticRebootEnabled ?: false,
                    experimentalState?.diagnosticBatteryLevelEnabled ?: false,
                    experimentalState?.diagnosticBatteryVoltageEnabled ?: false,
                    experimentalState?.diagnosticChargingStatusEnabled ?: false
                ).count { it }
                
                SettingRow(
                    label = stringResource(R.string.settings_diagnostic_sensor),
                    subLabel = if (diagnosticEnabled) 
                        stringResource(R.string.settings_diagnostic_sensor_desc) + " ($enabledCount/10)"
                    else 
                        stringResource(R.string.settings_diagnostic_sensor_desc)
                ) {
                    ModernSwitch(
                        checked = diagnosticEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                viewModel.saveDiagnosticSensorEnabled(enabled)
                                restartService()
                            }
                            if (enabled) {
                                navController.navigate(com.example.ava.ui.Screen.SETTINGS_DIAGNOSTIC) { launchSingleTop = true }
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                
                val hasEnvironmentSensor = DeviceCapabilities.hasAnyEnvironmentSensor(context)
                val deviceNotSupportedText = stringResource(R.string.settings_device_not_supported)
                SettingRow(
                    label = stringResource(R.string.settings_sensor_enabled),
                    subLabel = if (hasEnvironmentSensor)
                        stringResource(R.string.settings_sensor_enabled_desc)
                    else
                        deviceNotSupportedText
                ) {
                    ModernSwitch(
                        checked = if (hasEnvironmentSensor) experimentalState?.environmentSensorEnabled ?: false else false,
                        enabled = hasEnvironmentSensor,
                        onCheckedChange = { 
                            if (hasEnvironmentSensor) {
                                coroutineScope.launch {
                                    viewModel.saveEnvironmentSensorEnabled(it)
                                    restartService()
                                }
                            } else {
                                Toast.makeText(context, deviceNotSupportedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.environmentSensorEnabled == true) {
                    SettingsDivider()
                    
                    val sensorInterval = experimentalState?.sensorUpdateInterval ?: 35
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_sensor_update_interval),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = getTitleColor()
                                )
                                Text(
                                    text = stringResource(R.string.settings_sensor_update_interval_desc),
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${sensorInterval}s",
                                fontSize = 14.sp,
                                color = getAccentColor(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = sensorInterval.toFloat(),
                            onValueChange = { 
                                coroutineScope.launch {
                                    viewModel.saveSensorUpdateInterval(it.toInt())
                                }
                            },
                            valueRange = 10f..60f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = getAccentColor(),
                                activeTrackColor = getAccentColor(),
                                inactiveTrackColor = getSliderInactiveColor(),
                                activeTickColor = getAccentColor(),
                                inactiveTickColor = getSliderInactiveColor()
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                
                SettingsDivider()
                
                
                val hasProximitySensor = DeviceCapabilities.hasProximitySensor(context)
                SettingRow(
                    label = stringResource(R.string.settings_proximity_sensor),
                    subLabel = if (hasProximitySensor)
                        stringResource(R.string.settings_proximity_sensor_desc)
                    else
                        deviceNotSupportedText
                ) {
                    ModernSwitch(
                        checked = if (hasProximitySensor) experimentalState?.proximitySensorEnabled ?: false else false,
                        enabled = hasProximitySensor,
                        onCheckedChange = { 
                            if (hasProximitySensor) {
                                coroutineScope.launch {
                                    viewModel.saveProximitySensorEnabled(it)
                                    restartService()
                                }
                            } else {
                                Toast.makeText(context, deviceNotSupportedText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.proximitySensorEnabled == true) {
                    SettingsDivider()
                    
                    
                    SettingRow(
                        label = stringResource(R.string.settings_proximity_send_to_hass),
                        subLabel = stringResource(R.string.settings_proximity_send_to_hass_desc)
                    ) {
                        ModernSwitch(
                            checked = experimentalState?.proximitySendToHass ?: false,
                            onCheckedChange = { 
                                coroutineScope.launch {
                                    viewModel.saveProximitySendToHass(it)
                                    restartService()
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    
                    SettingRow(
                        label = stringResource(R.string.settings_proximity_wake_screen),
                        subLabel = stringResource(R.string.settings_proximity_wake_screen_desc)
                    ) {
                        ModernSwitch(
                            checked = experimentalState?.proximityWakeScreen ?: true,
                            onCheckedChange = { 
                                coroutineScope.launch {
                                    viewModel.saveProximityWakeScreen(it)
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    
                    
                    val proximityAwayDelay = experimentalState?.proximityAwayDelay ?: 30
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_proximity_away_delay),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = getTitleColor()
                                )
                                Text(
                                    text = stringResource(R.string.settings_proximity_away_delay_desc),
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                text = "${proximityAwayDelay}s",
                                fontSize = 14.sp,
                                color = getAccentColor(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = proximityAwayDelay.toFloat(),
                            onValueChange = { 
                                coroutineScope.launch {
                                    viewModel.saveProximityAwayDelay(it.toInt())
                                }
                            },
                            valueRange = 10f..120f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = getAccentColor(),
                                activeTrackColor = getAccentColor(),
                                inactiveTrackColor = getSliderInactiveColor(),
                                activeTickColor = getAccentColor(),
                                inactiveTickColor = getSliderInactiveColor()
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                
            }
        }
        
        
        item {
            SimpleCard {
                
                SettingRow(
                    label = stringResource(R.string.settings_auto_unlock),
                    subLabel = stringResource(R.string.settings_auto_unlock_desc)
                ) {
                    val proximityWakeEnabled = experimentalState?.proximityWakeScreen != false
                    val proximitySensorEnabled = experimentalState?.proximitySensorEnabled == true
                    val autoUnlockAvailable = proximityWakeEnabled && proximitySensorEnabled
                    ModernSwitch(
                        checked = autoUnlockAvailable && (experimentalState?.proximityAutoUnlock == true),
                        enabled = autoUnlockAvailable,
                        onCheckedChange = { 
                            coroutineScope.launch {
                                viewModel.saveProximityAutoUnlock(it)
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                
                val hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
                SettingRow(
                    label = stringResource(R.string.settings_force_orientation),
                    subLabel = if (hasOverlayPermission)
                        stringResource(R.string.settings_force_orientation_desc)
                    else
                        stringResource(R.string.settings_overlay_permission_required)
                ) {
                    ModernSwitch(
                        checked = (experimentalState?.forceOrientationEnabled == true) && hasOverlayPermission,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasOverlayPermission) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                intent.data = android.net.Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveForceOrientationEnabled(enabled)
                                    restartService()
                                }
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.forceOrientationEnabled == true) {
                    SettingsDivider()
                    
                    val currentMode = experimentalState?.forceOrientationMode ?: "portrait"
                    val portraitLabel = stringResource(R.string.settings_orientation_portrait)
                    val landscapeLabel = stringResource(R.string.settings_orientation_landscape)
                    val autoLabel = stringResource(R.string.settings_orientation_auto)
                    val modes = listOf("portrait" to portraitLabel, "landscape" to landscapeLabel, "auto" to autoLabel)
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_force_orientation_mode),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = getTitleColor()
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            modes.forEach { (mode, label) ->
                                val isSelected = currentMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            coroutineScope.launch {
                                                viewModel.saveForceOrientationMode(mode)
                                                restartService()
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) getAccentColor() else getSliderInactiveColor()
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
