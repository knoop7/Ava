package com.example.ava.ui.screens.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.ui.screens.settings.components.*
import com.example.ava.ui.screens.home.PREFS_NAME
import com.example.ava.ui.screens.home.KEY_DARK_MODE
import com.example.ava.ui.screens.home.KEY_HIDE_HEADER
import com.example.ava.settings.PlayerSettings
import com.example.ava.ui.prefs.rememberBooleanPreference
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.ava.ui.theme.SlateText as TitleColor
import com.example.ava.ui.theme.SlateLabel as LabelColor
import com.example.ava.ui.theme.SlateTertiary as SubLabelColor
import com.example.ava.ui.theme.AccentBlue as AccentColor
import com.example.ava.ui.theme.IconBackground


@Composable
fun InteractionSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.satelliteSettingsState.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerSettingsState.collectAsStateWithLifecycle(null)
    val notificationState by viewModel.notificationSettingsState.collectAsStateWithLifecycle(com.example.ava.settings.NotificationSettings())
    val context = LocalContext.current
    val enabled = uiState != null
    val isChinese = remember {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        language.startsWith("zh") || country in listOf("CN", "TW", "HK", "MO")
    }
    
    val homePrefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    var hideHeader by remember { mutableStateOf(homePrefs.getBoolean(KEY_HIDE_HEADER, false)) }
    val isDarkMode by rememberBooleanPreference(homePrefs, KEY_DARK_MODE, false)
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_interaction)
    ) {
        item(key = "display_settings") {
            SimpleCard {
                SettingRow(
                    label = stringResource(R.string.settings_hide_header),
                    subLabel = stringResource(R.string.settings_hide_header_desc)
                ) {
                    ModernSwitch(
                        checked = hideHeader,
                        enabled = true,
                        onCheckedChange = {
                            hideHeader = it
                            homePrefs.edit().putBoolean(KEY_HIDE_HEADER, it).apply()
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_dark_mode),
                    subLabel = stringResource(R.string.settings_dark_mode_desc)
                ) {
                    ModernSwitch(
                        checked = isDarkMode,
                        enabled = true,
                        onCheckedChange = {
                            homePrefs.edit().putBoolean(KEY_DARK_MODE, it).apply()
                        }
                    )
                }
            }
        }
        
        item(key = "quick_entity") {
            QuickEntitySettingsCard(
                enabled = enabled,
                coroutineScope = coroutineScope,
                onEditSlot = { slotIndex ->
                    navController.navigate("quick_entity_edit/$slotIndex") { launchSingleTop = true }
                }
            )
        }
        
        item(key = "dream_clock") {
            SimpleCard {
                SettingRow(
                    label = stringResource(R.string.settings_dream_clock),
                    subLabel = stringResource(R.string.settings_dream_clock_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableDreamClock ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            if (it && !checkOverlayPermission(context)) {
                                requestOverlayPermission(context)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveDreamClock(it)
                                }
                            }
                        }
                    )
                }
                
                val dreamClockEnabled = playerState?.enableDreamClock ?: false
                if (dreamClockEnabled) {
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingRow(
                        label = stringResource(R.string.settings_dream_clock_ha_switch),
                        subLabel = stringResource(R.string.settings_dream_clock_ha_switch_desc)
                    ) {
                        ModernSwitch(
                            checked = playerState?.enableDreamClockDisplay ?: false,
                            enabled = enabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.saveDreamClockDisplay(it)
                                    kotlinx.coroutines.delay(100)
                                    context.stopService(Intent(context, VoiceSatelliteService::class.java))
                                    kotlinx.coroutines.delay(600)
                                    val intent = Intent(context, VoiceSatelliteService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        
        item(key = "vinyl_cover") {
            SimpleCard {
                SettingRow(
                    label = stringResource(R.string.settings_vinyl_cover),
                    subLabel = stringResource(R.string.settings_vinyl_cover_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableVinylCover ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            if (it && !checkOverlayPermission(context)) {
                                requestOverlayPermission(context)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveVinylCover(it)
                                }
                            }
                        }
                    )
                }
                

                val vinylCoverEnabled = playerState?.enableVinylCover ?: false
                if (vinylCoverEnabled) {
                    SettingsDivider()
                    HaMediaPlayerSetting(
                        viewModel = viewModel,
                        uiState = uiState,
                        enabled = enabled,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
        
        item(key = "floating_wake") {
            SimpleCard {
                val hasOverlayPermission = checkOverlayPermission(context)
                SettingRow(
                    label = stringResource(R.string.settings_floating_window),
                    subLabel = stringResource(R.string.settings_floating_window_desc)
                ) {
                    ModernSwitch(
                        checked = (playerState?.enableFloatingWindow == true) && hasOverlayPermission,
                        enabled = enabled,
                        onCheckedChange = {
                            if (it && !checkOverlayPermission(context)) {
                                requestOverlayPermission(context)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveFloatingWindow(it)
                                }
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                SettingRow(
                    label = stringResource(R.string.settings_ha_switch_overlay),
                    subLabel = stringResource(R.string.settings_ha_switch_overlay_desc)
                ) {
                    ModernSwitch(
                        checked = (playerState?.enableHaSwitchOverlay == true) && hasOverlayPermission,
                        enabled = enabled,
                        onCheckedChange = {
                            if (it && !checkOverlayPermission(context)) {
                                requestOverlayPermission(context)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveHaSwitchOverlay(it)
                                }
                            }
                        }
                    )
                }
            }
        }
        
        item(key = "weather") {
            SimpleCard {
                SettingRow(
                    label = stringResource(R.string.settings_weather_overlay),
                    subLabel = stringResource(R.string.settings_weather_overlay_realtime_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableWeatherOverlay ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            if (it && !checkOverlayPermission(context)) {
                                requestOverlayPermission(context)
                            } else {
                                coroutineScope.launch {
                                    viewModel.saveWeatherOverlay(it)
                                }
                            }
                        }
                    )
                }
                
                val weatherEnabled = playerState?.enableWeatherOverlay ?: false
                if (weatherEnabled) {
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    

                    SettingRow(
                        label = stringResource(R.string.settings_weather_overlay_ha_switch),
                        subLabel = stringResource(R.string.settings_weather_overlay_ha_switch_desc)
                    ) {
                        ModernSwitch(
                            checked = playerState?.enableWeatherOverlayDisplay ?: false,
                            enabled = enabled,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    viewModel.saveWeatherOverlayDisplay(it)
                                    kotlinx.coroutines.delay(100)
                                    context.stopService(Intent(context, VoiceSatelliteService::class.java))
                                    kotlinx.coroutines.delay(600)
                                    val intent = Intent(context, VoiceSatelliteService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                        )
                    }
                    
                    SettingsDivider()
                    HaWeatherEntitySetting(
                        viewModel = viewModel,
                        playerState = playerState,
                        enabled = enabled,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
        
        
        item(key = "notification_scene") {
            SimpleCard {
                val displayDuration = (notificationState?.sceneDisplayDuration ?: 5000) / 1000
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_scene_display_duration),
                        color = getLabelColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getSliderInactiveColor()
                    ) {
                        Text(
                            text = "${displayDuration}s",
                            color = getAccentColor(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Slider(
                    value = displayDuration.toFloat(),
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            viewModel.saveSceneDisplayDuration(newValue.toInt() * 1000)
                        }
                    },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = getAccentColor(),
                        activeTrackColor = getAccentColor(),
                        inactiveTrackColor = getSliderInactiveColor(),
                        activeTickColor = getAccentColor(),
                        inactiveTickColor = getSliderInactiveColor()
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                
                NotificationSoundSection(
                    viewModel = viewModel,
                    notificationState = notificationState,
                    enabled = enabled,
                    coroutineScope = coroutineScope,
                    context = context
                )
                
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsDivider()
                
                CustomSceneSection(
                    viewModel = viewModel,
                    notificationState = notificationState,
                    enabled = enabled,
                    coroutineScope = coroutineScope,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun NotificationSoundSection(
    viewModel: SettingsViewModel,
    notificationState: com.example.ava.settings.NotificationSettings,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val soundUri = notificationState.soundUri
    val noneLabel = stringResource(R.string.sound_none)
    val unknownLabel = stringResource(R.string.sound_unknown)
    
    var showRingtonePicker by remember { mutableStateOf(false) }
    
    val audioFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            coroutineScope.launch {
                viewModel.saveSoundUri(uri.toString())
                viewModel.saveSoundEnabled(true)
            }
            showRingtonePicker = false
        }
    }
    
    val ringtones = remember(noneLabel) {
        val list = mutableListOf<Pair<String, String>>()
        list.add(noneLabel to "")
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position).toString()
            list.add(title to uri)
        }
        list
    }
    
    val soundName = remember(soundUri, noneLabel, unknownLabel) {
        if (soundUri.isEmpty()) {
            noneLabel
        } else {
            try {
                val uri = Uri.parse(soundUri)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: unknownLabel
            } catch (e: Exception) {
                unknownLabel
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showRingtonePicker = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.notification_sound),
                fontSize = 14.sp,
                color = getLabelColor(),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.notification_sound_desc),
                fontSize = 12.sp,
                color = SubLabelColor
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SubLabelColor
        )
    }
    
    if (showRingtonePicker) {
        SharedRingtonePickerDialog(
            ringtones = ringtones,
            currentUri = soundUri,
            context = context,
            title = stringResource(R.string.select_sound),
            onDismiss = { showRingtonePicker = false },
            onConfirm = { uri ->
                coroutineScope.launch {
                    viewModel.saveSoundUri(uri)
                    viewModel.saveSoundEnabled(uri.isNotEmpty())
                }
                showRingtonePicker = false
            },
            onSelectExternal = {
                audioFileLauncher.launch("audio/*")
            }
        )
    }
}

@Composable
private fun CustomSceneSection(
    viewModel: SettingsViewModel,
    notificationState: com.example.ava.settings.NotificationSettings,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val currentUrl = notificationState.customSceneUrl
    val _refreshSignal = com.example.ava.notifications.NotificationScenes.refreshCount.value
    val customSceneCount = com.example.ava.notifications.NotificationScenes.customSceneCount
    val loadState = com.example.ava.notifications.NotificationScenes.loadState
    
    val statusText = when (loadState) {
        is com.example.ava.notifications.NotificationScenes.SceneLoadState.Idle -> {
            if (currentUrl.isEmpty()) stringResource(R.string.settings_custom_scene_not_configured)
            else stringResource(R.string.settings_custom_scene_loading)
        }
        is com.example.ava.notifications.NotificationScenes.SceneLoadState.Loading -> stringResource(R.string.settings_custom_scene_loading)
        is com.example.ava.notifications.NotificationScenes.SceneLoadState.Success -> stringResource(R.string.settings_custom_scene_loaded, customSceneCount)
        is com.example.ava.notifications.NotificationScenes.SceneLoadState.Error -> {
            val errorDetail = loadState.detail
            if (errorDetail != null) stringResource(loadState.resId, errorDetail)
            else stringResource(loadState.resId)
        }
    }
    
    var showTutorialDialog by remember { mutableStateOf(false) }
    val prefs = context.getSharedPreferences("ava_prefs", android.content.Context.MODE_PRIVATE)
    val hasSeenTutorial = prefs.getBoolean("custom_scene_tutorial_seen", false)
    
    CustomSceneUrlSetting(
        currentUrl = currentUrl,
        statusText = statusText,
        enabled = enabled,
        onClickWithTutorialCheck = {
            if (!hasSeenTutorial && currentUrl.isEmpty()) {
                showTutorialDialog = true
                true
            } else {
                false
            }
        },
        onConfirmRequest = { url ->
            coroutineScope.launch {
                viewModel.saveCustomSceneUrl(url)
            }
        }
    )
    
    if (showTutorialDialog) {
        CustomSceneTutorialDialog(
            onDismiss = { showTutorialDialog = false },
            onConfirm = {
                prefs.edit().putBoolean("custom_scene_tutorial_seen", true).apply()
                showTutorialDialog = false
                coroutineScope.launch {
                    viewModel.saveCustomSceneUrl("https://ghfast.top/https://raw.githubusercontent.com/knoop7/Ava/refs/heads/master/custom_scenes.json")
                }
            }
        )
    }
    
    SettingsDivider()
    
    SettingRow(
        label = stringResource(R.string.custom_scene_config)
    ) {
        TextButton(onClick = { showTutorialDialog = true }) {
            Text("?", color = getAccentColor())
        }
    }
}

@Composable
private fun HaMediaPlayerSetting(
    viewModel: SettingsViewModel,
    uiState: UIState?,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val currentEntity = uiState?.haMediaPlayerEntity ?: ""
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_ha_media_player),
                fontSize = 14.sp,
                color = getLabelColor(),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.settings_ha_media_player_desc),
                fontSize = 12.sp,
                color = SubLabelColor
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SubLabelColor
        )
    }
    
    if (showDialog) {
        HaMediaPlayerDialog(
            currentValue = currentEntity,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                coroutineScope.launch {
                    viewModel.saveHaMediaPlayerEntity(newValue)
                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun HaWeatherEntitySetting(
    viewModel: SettingsViewModel,
    playerState: PlayerSettings?,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val currentEntity = playerState?.haWeatherEntity ?: ""
    var showDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_ha_weather_entity),
                fontSize = 14.sp,
                color = getLabelColor(),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.settings_ha_weather_entity_short_desc),
                fontSize = 12.sp,
                color = SubLabelColor
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SubLabelColor
        )
    }
    
    if (showDialog) {
        HaWeatherEntityDialog(
            currentValue = currentEntity,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                coroutineScope.launch {
                    viewModel.saveHaWeatherEntity(newValue)
                    com.example.ava.services.VoiceSatelliteService.getInstance()?.restartVoiceSatellite()
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun HaWeatherEntityDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_ha_weather_entity),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = getTitleColor()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_ha_weather_entity_desc),
                    fontSize = 11.sp,
                    color = SubLabelColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                androidx.compose.material3.TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("weather.xxx", color = SubLabelColor, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = getLabelColor()),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = getInputBackground(),
                        unfocusedContainerColor = getInputBackground(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = getAccentColor()
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok), color = getAccentColor())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = SubLabelColor)
            }
        },
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun HaMediaPlayerDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_ha_media_player),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = getTitleColor()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_ha_media_player_dialog_desc),
                    fontSize = 11.sp,
                    color = SubLabelColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                androidx.compose.material3.TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("media_player.xxx", color = SubLabelColor, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = getLabelColor()),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = getInputBackground(),
                        unfocusedContainerColor = getInputBackground(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = getAccentColor()
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(android.R.string.ok), color = getAccentColor())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel), color = SubLabelColor)
            }
        },
        containerColor = getDialogBackground(),
        shape = RoundedCornerShape(16.dp)
    )
}
