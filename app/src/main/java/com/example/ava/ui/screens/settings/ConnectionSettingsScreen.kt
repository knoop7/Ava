package com.example.ava.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.ui.screens.settings.components.*
import com.example.ava.settings.PlayerSettings
import kotlinx.coroutines.launch


@Composable
fun ConnectionSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.satelliteSettingsState.collectAsStateWithLifecycle(null)
    val microphoneState by viewModel.microphoneSettingsState.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerSettingsState.collectAsStateWithLifecycle(null)
    val context = LocalContext.current
    val enabled = uiState != null
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_connection)
    ) {
        item {
            SimpleCard {
                
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
            }
        }
        
        item(key = "continuous_conversation") {
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
            }
        }
        
        item(key = "wake_sound") {
            SimpleCard {
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
                
                val wakeSoundEnabled = playerState?.enableWakeSound ?: true
                if (wakeSoundEnabled) {
                    SettingsDivider()
                    WakeSoundItem(
                        label = stringResource(R.string.wake_sound_1),
                        soundUri = playerState?.wakeSound ?: "asset:///sounds/wake_word_triggered.wav",
                        enabled = enabled,
                        context = context,
                        onSoundSelected = { uri ->
                            coroutineScope.launch {
                                viewModel.saveWakeSoundUri(uri)
                            }
                        }
                    )
                    SettingsDivider()
                    WakeSoundItem(
                        label = stringResource(R.string.wake_sound_2),
                        soundUri = playerState?.wakeSound2 ?: "asset:///sounds/wake_word_triggered.wav",
                        enabled = enabled,
                        context = context,
                        onSoundSelected = { uri ->
                            coroutineScope.launch {
                                viewModel.saveWakeSound2Uri(uri)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeSoundItem(
    label: String,
    soundUri: String,
    enabled: Boolean,
    context: android.content.Context,
    onSoundSelected: (String) -> Unit
) {
    val noneLabel = stringResource(R.string.sound_none)
    val defaultLabel = stringResource(R.string.wake_sound_default)
    val unknownLabel = stringResource(R.string.sound_unknown)
    
    var showRingtonePicker by remember { mutableStateOf(false) }
    
    val audioFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            onSoundSelected(uri.toString())
            showRingtonePicker = false
        }
    }
    
    val ringtones = remember {
        val list = mutableListOf<Pair<String, String>>()
        list.add(Pair(defaultLabel, "asset:///sounds/wake_word_triggered.wav"))
        list.add(Pair(noneLabel, ""))
        val manager = android.media.RingtoneManager(context)
        manager.setType(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(android.media.RingtoneManager.TITLE_COLUMN_INDEX)
            val uriStr = manager.getRingtoneUri(cursor.position).toString()
            list.add(Pair(title, uriStr))
        }
        list
    }
    
    val soundName = remember(soundUri, noneLabel, defaultLabel, unknownLabel) {
        when {
            soundUri.isEmpty() -> noneLabel
            soundUri.startsWith("asset://") -> defaultLabel
            else -> {
                try {
                    val uri = android.net.Uri.parse(soundUri)
                    val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: unknownLabel
                } catch (e: Exception) {
                    unknownLabel
                }
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showRingtonePicker = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = getLabelColor(),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.wake_sound_select_prefix) + soundName,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8)
        )
    }
    
    if (showRingtonePicker) {
        SharedRingtonePickerDialog(
            ringtones = ringtones,
            currentUri = soundUri,
            context = context,
            title = stringResource(R.string.wake_sound_select),
            onDismiss = { showRingtonePicker = false },
            onConfirm = { uri ->
                onSoundSelected(uri)
                showRingtonePicker = false
            },
            onSelectExternal = {
                audioFileLauncher.launch("audio/*")
            }
        )
    }
}
