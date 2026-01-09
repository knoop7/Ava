package com.example.ava.ui.screens.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch
import java.util.Locale


private val LabelColor = Color(0xFF334155)
private val SubLabelColor = Color(0xFF94A3B8)
private val AccentColor = Color(0xFF4F46E5)
private val IconBackground = Color(0xFFEEF2FF)
private val TitleColor = Color(0xFF1E293B)


@Composable
fun InteractionSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.satelliteSettingsState.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerSettingsState.collectAsStateWithLifecycle(null)
    val notificationState by viewModel.notificationSettingsState.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    val enabled = uiState != null
    val isChinese = remember {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        val timezone = java.util.TimeZone.getDefault().id
        language.startsWith("zh") || 
        country in listOf("CN", "TW", "HK", "MO") ||
        timezone.startsWith("Asia/Shanghai") || 
        timezone.startsWith("Asia/Chongqing") ||
        timezone.startsWith("Asia/Hong_Kong") ||
        timezone.startsWith("Asia/Taipei")
    }
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_interaction)
    ) {
        item {
            SimpleCard {
                
                SettingRow(
                    label = stringResource(R.string.settings_continuous_conversation),
                    subLabel = stringResource(R.string.settings_continuous_conversation_desc)
                ) {
                    ModernSwitch(
                        checked = playerState?.enableContinuousConversation ?: false,
                        enabled = enabled,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.saveContinuousConversation(it)
                            }
                        }
                    )
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
                SettingRow(
                    label = stringResource(R.string.settings_floating_window),
                    subLabel = stringResource(R.string.settings_floating_window_desc)
                ) {
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                if (isChinese) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    
                    
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
                        Spacer(modifier = Modifier.height(8.dp))
                        val areasData = loadAreasData(context)
                        val savedDistrict = playerState?.weatherCity
                        val initialSelection = remember(savedDistrict, areasData) {
                            if (savedDistrict != null) {
                                for (province in areasData) {
                                    for (city in province.cities) {
                                        if (city.districts.contains(savedDistrict)) {
                                            return@remember Pair(province, city)
                                        }
                                    }
                                }
                            }
                            Pair(null, null)
                        }
                        
                        var selectedProvince by remember(initialSelection) { mutableStateOf(initialSelection.first) }
                        var selectedCity by remember(initialSelection) { mutableStateOf(initialSelection.second) }
                        val pleaseSelectText = stringResource(R.string.settings_please_select)
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                
                                SelectSetting(
                                    name = stringResource(R.string.settings_province),
                                    selected = selectedProvince,
                                    items = areasData,
                                    enabled = enabled,
                                    key = { it.name },
                                    value = { it?.name ?: pleaseSelectText },
                                    onConfirmRequest = {
                                        selectedProvince = it
                                        selectedCity = null
                                    }
                                )
                                
                                
                                SelectSetting(
                                    name = stringResource(R.string.settings_city),
                                    selected = selectedCity,
                                    items = selectedProvince?.cities ?: emptyList(),
                                    enabled = enabled && selectedProvince != null,
                                    key = { it.name },
                                    value = { it?.name ?: pleaseSelectText },
                                    onConfirmRequest = {
                                        selectedCity = it
                                    }
                                )
                                
                                
                                SelectSetting(
                                    name = stringResource(R.string.settings_district),
                                    selected = playerState?.weatherCity,
                                    items = selectedCity?.districts ?: emptyList(),
                                    enabled = enabled && selectedCity != null,
                                    key = { it },
                                    value = { it ?: pleaseSelectText },
                                    onConfirmRequest = {
                                        if (it != null) {
                                            coroutineScope.launch {
                                                viewModel.saveWeatherCity(it)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        
        item {
            SimpleCard {
                
                val displayDuration = (notificationState?.sceneDisplayDuration ?: 5000) / 1000
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_scene_display_duration),
                        color = LabelColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IconBackground
                    ) {
                        Text(
                            text = "${displayDuration}s",
                            color = AccentColor,
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
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                
                NotificationSoundSection(
                    viewModel = viewModel,
                    notificationState = notificationState,
                    enabled = enabled,
                    coroutineScope = coroutineScope,
                    context = context
                )
                
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
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
    notificationState: com.example.ava.settings.NotificationSettings?,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val soundEnabled = notificationState?.soundEnabled ?: false
    val soundUri = notificationState?.soundUri ?: ""
    val noneLabel = stringResource(R.string.sound_none)
    val unknownLabel = stringResource(R.string.sound_unknown)
    val selectSoundLabel = stringResource(R.string.select_sound)
    
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
    
    SettingRow(
        label = stringResource(R.string.notification_sound),
        subLabel = stringResource(R.string.notification_sound_desc)
    ) {
        ModernSwitch(
            checked = soundEnabled,
            enabled = enabled,
            onCheckedChange = {
                coroutineScope.launch {
                    viewModel.saveSoundEnabled(it)
                }
            }
        )
    }
    
    if (soundEnabled) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showRingtonePicker = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectSoundLabel,
                color = SubLabelColor,
                fontSize = 13.sp
            )
            Text(
                text = soundName,
                color = AccentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
    }
    
    if (showRingtonePicker) {
        RingtonePickerDialog(
            ringtones = ringtones,
            currentUri = soundUri,
            context = context,
            onDismiss = { showRingtonePicker = false },
            onConfirm = { uri ->
                coroutineScope.launch {
                    viewModel.saveSoundUri(uri)
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
private fun RingtonePickerDialog(
    ringtones: List<Pair<String, String>>,
    currentUri: String,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onSelectExternal: () -> Unit
) {
    var tempSelectedUri by remember { mutableStateOf(currentUri) }
    var currentRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }
    val selectSoundLabel = stringResource(R.string.select_sound)
    
    AlertDialog(
        onDismissRequest = { 
            currentRingtone?.stop()
            onDismiss()
        },
        title = {
            Text(
                text = selectSoundLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TitleColor
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
                                        ringtone?.play()
                                        currentRingtone = ringtone
                                    } catch (e: Exception) {}
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
                                selectedColor = AccentColor,
                                unselectedColor = SubLabelColor
                            )
                        )
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = if (isSelected) AccentColor else LabelColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (title != ringtones.last().first) {
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentRingtone?.stop()
                            onSelectExternal()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.select_external_audio),
                        fontSize = 14.sp,
                        color = AccentColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                currentRingtone?.stop()
                onConfirm(tempSelectedUri)
            }) {
                Text(
                    text = stringResource(R.string.label_ok),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                currentRingtone?.stop()
                onDismiss()
            }) {
                Text(
                    text = stringResource(R.string.label_cancel),
                    fontSize = 14.sp,
                    color = SubLabelColor
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
private fun CustomSceneSection(
    viewModel: SettingsViewModel,
    notificationState: com.example.ava.settings.NotificationSettings?,
    enabled: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val currentUrl = notificationState?.customSceneUrl ?: ""
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
                    viewModel.saveCustomSceneUrl("https://ghfast.top/https://raw.githubusercontent.com/knoop7/Ava/refs/heads/master/c.json")
                }
            }
        )
    }
}
