package com.example.ava.ui.screens.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ava.R
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch


@Composable
fun ConnectionSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.satelliteSettingsState.collectAsStateWithLifecycle(null)
    val microphoneState by viewModel.microphoneSettingsState.collectAsStateWithLifecycle(null)
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
                
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                
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
    }
}
