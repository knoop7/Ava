package com.example.ava.ui.screens.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.ava.services.VoiceSatelliteService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ava.R
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.settings.components.*
import com.example.ava.settings.PlayerSettings
import com.example.ava.settings.NotificationSettings
import com.example.ava.utils.BatteryOptimizationHelper
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.ava.ui.theme.SlateBorder as CardBorder
import com.example.ava.ui.theme.SlateText as TitleColor
import com.example.ava.ui.theme.SlateLabel as LabelColor
import com.example.ava.ui.theme.SlateTertiary as SubLabelColor
import com.example.ava.ui.theme.AccentBlue as AccentColor
import com.example.ava.ui.theme.IconBackground


private val CardBackgroundLight = Color.White
private val CardBackgroundDark = Color(0xFF1F1F1F)

@Composable
private fun isDarkModeEnabled(): Boolean {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            com.example.ava.ui.screens.home.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
    }
    val isDarkMode by rememberBooleanPreference(
        prefs,
        com.example.ava.ui.screens.home.KEY_DARK_MODE,
        false
    )
    return isDarkMode
}

@Composable
fun getDialogBackground(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) CardBackgroundDark else CardBackgroundLight
}

@Composable
fun getLabelColor(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) LabelColorDark else LabelColor
}

@Composable
fun getTitleColor(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) Color(0xFFF1F5F9) else TitleColor
}

@Composable
fun getAccentColor(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) Color(0xFFA78B73) else AccentColor
}

@Composable
fun getSliderInactiveColor(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFE2E8F0)
}

@Composable
fun getInputBackground(): Color {
    val isDarkMode = isDarkModeEnabled()
    return if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF8FAFC)
} 
private val IconColor = AccentColor
private val LabelColorDark = Color(0xFFF1F5F9)


fun checkOverlayPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}


fun requestOverlayPermission(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}


@Composable
fun SectionCard(
    title: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDarkMode = isDarkModeEnabled()
    val cardBackground = if (isDarkMode) CardBackgroundDark else CardBackgroundLight
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = cardBackground,
        shadowElevation = if (isDarkMode) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isDarkMode) Color(0xFF2D2D2D) else IconBackground,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconResId),
                        contentDescription = title,
                        tint = IconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTitleColor()
                )
            }
            
            
            content()
        }
    }
}


@Composable
fun SimpleCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDarkMode = isDarkModeEnabled()
    val cardBackground = if (isDarkMode) CardBackgroundDark else CardBackgroundLight
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = cardBackground,
        shadowElevation = if (isDarkMode) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}


@Composable
fun SettingRow(
    label: String,
    subLabel: String = "",
    iconResId: Int? = null,
    action: @Composable () -> Unit
) {
    val isDarkMode = isDarkModeEnabled()
    val labelColor = if (isDarkMode) LabelColorDark else LabelColor
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconResId != null) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = label,
                tint = SubLabelColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    color = SubLabelColor,
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
fun SettingsDivider() {
    val isDarkMode = isDarkModeEnabled()
    val dividerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF1F5F9)
    
    androidx.compose.material3.HorizontalDivider(color = dividerColor, thickness = 1.dp)
}

@Composable
fun ModernSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val isDarkMode = isDarkModeEnabled()
    
    androidx.compose.runtime.key(checked) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (isDarkMode) Color(0xFFA78B73) else AccentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFE2E8F0),
                uncheckedBorderColor = Color.Transparent,
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
                disabledCheckedTrackColor = if (isDarkMode) Color(0xFFA78B73).copy(alpha = 0.4f) else AccentColor.copy(alpha = 0.4f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                disabledUncheckedTrackColor = if (isDarkMode) Color(0xFF3D3D3D).copy(alpha = 0.4f) else Color(0xFFE2E8F0).copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun VoiceSatelliteSettings(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.satelliteSettingsState.collectAsStateWithLifecycle(null)
    val microphoneState by viewModel.microphoneSettingsState.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerSettingsState.collectAsStateWithLifecycle(null)
    val notificationState by viewModel.notificationSettingsState.collectAsStateWithLifecycle(NotificationSettings())
    val experimentalState by viewModel.experimentalSettingsState.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    
    val enabled = uiState != null
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        
        item {
            SectionCard(
                title = stringResource(R.string.section_core_connection),
                iconResId = R.drawable.wifi_24px
            ) {
                
                SelectSetting(
                    name = stringResource(R.string.label_voice_satellite_wake_word),
                    selected = microphoneState?.wakeWord,
                    items = microphoneState?.wakeWords,
                    enabled = enabled,
                    key = { it.id },
                    value = { it?.wakeWord?.wake_word ?: "" },
                    onConfirmRequest = {
                        if (it != null) {
                            coroutineScope.launch {
                                viewModel.saveWakeWord(it.id)
                            }
                        }
                    }
                )
                
                SettingsDivider()
                
                val noneText = stringResource(R.string.label_voice_satellite_wake_word_2_none)
                SelectSetting(
                    name = stringResource(R.string.label_voice_satellite_wake_word_2),
                    selected = microphoneState?.wakeWord2,
                    items = microphoneState?.wakeWords,
                    enabled = enabled,
                    key = { it?.id ?: "" },
                    value = { it?.wakeWord?.wake_word ?: noneText },
                    onConfirmRequest = {
                        coroutineScope.launch {
                            viewModel.saveWakeWord2(it?.id)
                        }
                    }
                )
                
                SettingsDivider()
                
                
                TextSetting(
                    name = stringResource(R.string.label_voice_satellite_name),
                    value = uiState?.serverName ?: "",
                    enabled = enabled,
                    validation = { viewModel.validateName(it) },
                    onConfirmRequest = {
                        coroutineScope.launch {
                            viewModel.saveServerName(it)
                        }
                    }
                )
                
                SettingsDivider()
                
                
                IntSetting(
                    name = stringResource(R.string.label_voice_satellite_port),
                    description = stringResource(R.string.settings_port_description),
                    value = uiState?.serverPort,
                    enabled = enabled,
                    validation = { viewModel.validatePort(it) },
                    onConfirmRequest = {
                        coroutineScope.launch {
                            viewModel.saveServerPort(it)
                        }
                    }
                )
                
                SettingsDivider()
                
                
                SettingRow(
                    label = stringResource(R.string.settings_auto_restart),
                    subLabel = stringResource(R.string.settings_auto_restart_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableAutoRestart ?: false,
                        enabled = enabled,
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
            }
        }
        
        item {
            SectionCard(
                title = stringResource(R.string.section_interaction_visual),
                iconResId = R.drawable.star_24px
            ) {
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
                
                SettingsDivider()
                
                
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
                
                SettingsDivider()
                
                
                SettingRow(
                    label = stringResource(R.string.settings_floating_window),
                    subLabel = stringResource(R.string.settings_floating_window_desc)
                ) {
                    val hasOverlayPermission = checkOverlayPermission(context)
                    ModernSwitch(
                        checked = playerState?.enableFloatingWindow ?: false,
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
                    label = stringResource(R.string.label_voice_satellite_enable_wake_sound),
                    subLabel = stringResource(R.string.description_voice_satellite_play_wake_sound)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableWakeSound ?: true,
                        enabled = enabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveEnableWakeSound(it)
                            }
                        }
                    )
                }
                
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                val displayDuration = notificationState.sceneDisplayDuration / 1000
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
                
                val soundEnabled = notificationState.soundEnabled
                val soundUri = notificationState.soundUri
                
                
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
                
                
                val ringtones = remember {
                    val list = mutableListOf<Pair<String, String>>() 
                    list.add(context.getString(R.string.sound_none) to "")
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
                
                
                val soundName = remember(soundUri) {
                    if (soundUri.isEmpty()) {
                        context.getString(R.string.sound_none)
                    } else {
                        try {
                            val uri = Uri.parse(soundUri)
                            val ringtone = RingtoneManager.getRingtone(context, uri)
                            ringtone?.getTitle(context) ?: context.getString(R.string.sound_unknown)
                        } catch (e: Exception) {
                            context.getString(R.string.sound_unknown)
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
                    
                    var tempSelectedUri by remember { mutableStateOf(soundUri) }
                    
                    var currentRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }
                    
                    AlertDialog(
                        onDismissRequest = { 
                            currentRingtone?.stop()
                            showRingtonePicker = false 
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.select_sound),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = getTitleColor()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                ringtones.forEach { (title, uri) ->
                                    val isSelected = uri == tempSelectedUri
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                
                                                currentRingtone?.stop()
                                                
                                                tempSelectedUri = uri
                                                
                                                if (uri.isNotEmpty()) {
                                                    try {
                                                        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                            ringtone?.isLooping = false
                                                        }
                                                        ringtone?.play()
                                                        currentRingtone = ringtone
                                                    } catch (e: Exception) {
                                                        try {
                                                            val mediaPlayer = android.media.MediaPlayer()
                                                            mediaPlayer.setDataSource(context, Uri.parse(uri))
                                                            mediaPlayer.setAudioAttributes(
                                                                android.media.AudioAttributes.Builder()
                                                                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                                    .build()
                                                            )
                                                            mediaPlayer.prepare()
                                                            mediaPlayer.start()
                                                            mediaPlayer.setOnCompletionListener { it.release() }
                                                        } catch (e2: Exception) {}
                                                    }
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            modifier = Modifier.padding(end = 8.dp),
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = getAccentColor(),
                                                unselectedColor = SubLabelColor
                                            )
                                        )
                                        Text(
                                            text = title,
                                            fontSize = 14.sp,
                                            color = if (isSelected) getAccentColor() else getLabelColor(),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (title != ringtones.last().first) {
                                        SettingsDivider()
                                    }
                                }
                                
                                
                                SettingsDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentRingtone?.stop()
                                            audioFileLauncher.launch("audio/*")
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ Select Audio",
                                        fontSize = 14.sp,
                                        color = getAccentColor(),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { 
                                currentRingtone?.stop()
                                coroutineScope.launch {
                                    viewModel.saveSoundUri(tempSelectedUri)
                                    viewModel.saveSoundEnabled(tempSelectedUri.isNotEmpty())
                                }
                                showRingtonePicker = false 
                            }) {
                                Text(
                                    text = stringResource(R.string.label_ok),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getAccentColor()
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                currentRingtone?.stop()
                                showRingtonePicker = false 
                            }) {
                                Text(
                                    text = stringResource(R.string.label_cancel),
                                    fontSize = 14.sp,
                                    color = SubLabelColor
                                )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = getDialogBackground()
                    )
                }
                
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsDivider()
                
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
        }
        
        
        item {
            val hasCamera = viewModel.hasCamera()
            val hasBackCamera = viewModel.hasBackCamera()
            val hasFrontCamera = viewModel.hasFrontCamera()
            
            SectionCard(
                title = stringResource(R.string.settings_experimental),
                iconResId = R.drawable.experiment_24px
            ) {
                
                SettingRow(
                    label = stringResource(R.string.settings_camera_enabled),
                    subLabel = if (hasCamera) 
                        stringResource(R.string.settings_camera_enabled_desc)
                    else 
                        stringResource(R.string.settings_no_camera)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.cameraEnabled ?: false,
                        enabled = hasCamera && enabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveCameraEnabled(it)
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.cameraEnabled == true && hasCamera) {
                    SettingsDivider()
                    
                    val currentPosition = try {
                        com.example.ava.settings.CameraPosition.valueOf(
                            experimentalState?.cameraPosition ?: "FRONT"
                        )
                    } catch (e: Exception) {
                        com.example.ava.settings.CameraPosition.FRONT
                    }
                    
                    
                    val cameraOptions = buildList {
                        if (hasFrontCamera) add(com.example.ava.settings.CameraPosition.FRONT)
                        if (hasBackCamera) add(com.example.ava.settings.CameraPosition.BACK)
                    }
                    
                    val backCameraLabel = stringResource(R.string.settings_camera_back)
                    val frontCameraLabel = stringResource(R.string.settings_camera_front)
                    
                    SelectSetting(
                        name = stringResource(R.string.settings_camera_position),
                        selected = currentPosition,
                        items = cameraOptions,
                        enabled = enabled && cameraOptions.size > 1,
                        key = { it.name },
                        value = {
                            when (it) {
                                com.example.ava.settings.CameraPosition.BACK -> backCameraLabel
                                com.example.ava.settings.CameraPosition.FRONT -> frontCameraLabel
                                else -> ""
                            }
                        },
                        onConfirmRequest = {
                            if (it != null) {
                                coroutineScope.launch {
                                    viewModel.saveCameraPosition(it)
                                }
                            }
                        }
                    )
                }
                
                SettingsDivider()
                
                
                SettingRow(
                    label = stringResource(R.string.settings_environment_sensor),
                    subLabel = stringResource(R.string.settings_environment_sensor_desc)
                ) {
                    ModernSwitch(
                        checked = experimentalState?.environmentSensorEnabled ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveEnvironmentSensorEnabled(it)
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.environmentSensorEnabled == true) {
                    SettingsDivider()
                    
                    val sensorInterval = experimentalState?.sensorUpdateInterval ?: 35
                    
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    color = SubLabelColor
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
                            enabled = enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = getAccentColor(),
                                activeTrackColor = getAccentColor(),
                                inactiveTrackColor = getSliderInactiveColor(),
                                activeTickColor = getAccentColor(),
                                inactiveTickColor = getSliderInactiveColor()
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        
        item {
            SectionCard(
                title = stringResource(R.string.section_local_service),
                iconResId = R.drawable.cloud_24px
            ) {
                SettingRow(
                    label = stringResource(R.string.settings_weather_overlay),
                    subLabel = ""
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
                    

                    SettingItem(
                        name = stringResource(R.string.settings_weather_overlay_ha_switch),
                        description = stringResource(R.string.settings_weather_overlay_ha_switch_desc)
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
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    val currentEntity = playerState?.haWeatherEntity ?: ""
                    val notConfiguredText = stringResource(R.string.settings_custom_scene_not_configured)
                    
                    TextSetting(
                        name = stringResource(R.string.settings_ha_weather_entity),
                        description = stringResource(R.string.settings_ha_weather_entity_desc),
                        value = if (currentEntity.isEmpty()) notConfiguredText else currentEntity,
                        placeholder = "weather.xxx",
                        enabled = enabled,
                        onConfirmRequest = { newValue ->
                            coroutineScope.launch {
                                viewModel.saveHaWeatherEntity(newValue)
                            }
                        }
                    )
                }
            }
        }
        
        
        item {
            AboutSection()
        }
    }
}


@Composable
fun CustomSceneUrlSetting(
    currentUrl: String,
    statusText: String,
    enabled: Boolean,
    onClickWithTutorialCheck: () -> Boolean, 
    onConfirmRequest: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    
    SettingItem(
        name = stringResource(R.string.settings_custom_scene_url),
        description = statusText,
        value = if (currentUrl.isEmpty()) stringResource(R.string.settings_custom_scene_url_placeholder) else currentUrl,
        modifier = if (enabled) {
            Modifier.clickable {
                
                val handled = onClickWithTutorialCheck()
                if (!handled) {
                    showDialog = true
                }
            }
        } else {
            Modifier.alpha(0.5f)
        }
    )
    
    
    if (showDialog) {
        var textValue by remember { mutableStateOf(currentUrl) }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_custom_scene_url),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = getTitleColor()
                )
            },
            text = {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = { 
                        Text(
                            text = stringResource(R.string.settings_custom_scene_url_placeholder),
                            fontSize = 13.sp,
                            color = SubLabelColor
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = getLabelColor()),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = getInputBackground(),
                        unfocusedContainerColor = getInputBackground(),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = getAccentColor()
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmRequest(textValue)
                        showDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.label_ok), 
                        color = getAccentColor(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = stringResource(R.string.label_cancel), 
                        color = SubLabelColor,
                        fontSize = 14.sp
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = getDialogBackground()
        )
    }
}


@Composable
fun CustomSceneTutorialDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.custom_scene_config),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = getTitleColor()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_scene_desc),
                    fontSize = 13.sp,
                    color = getLabelColor(),
                    lineHeight = 18.sp
                )
                Text(
                    text = stringResource(R.string.custom_scene_json_format),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = getTitleColor()
                )
                Text(
                    text = stringResource(R.string.custom_scene_json_requirements),
                    fontSize = 12.sp,
                    color = SubLabelColor,
                    lineHeight = 17.sp
                )
                Text(
                    text = stringResource(R.string.custom_scene_example_hint),
                    fontSize = 12.sp,
                    color = SubLabelColor,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.custom_scene_use_example),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = getAccentColor()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.custom_scene_i_know),
                    fontSize = 14.sp,
                    color = SubLabelColor
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = getDialogBackground()
    )
}
