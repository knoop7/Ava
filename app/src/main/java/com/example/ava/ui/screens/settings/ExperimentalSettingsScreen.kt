package com.example.ava.ui.screens.settings

import android.widget.Toast
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
import com.example.ava.settings.CameraMode
import com.example.ava.ui.screens.settings.components.*
import kotlinx.coroutines.launch


@Composable
fun ExperimentalSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val experimentalState by viewModel.experimentalSettingsState.collectAsStateWithLifecycle(null)
    
    val hasCamera = viewModel.hasCamera()
    val hasBackCamera = viewModel.hasBackCamera()
    val hasFrontCamera = viewModel.hasFrontCamera()
    val noCameraText = stringResource(R.string.settings_no_camera)
    
    SettingsDetailScreen(
        navController = navController,
        title = stringResource(R.string.settings_group_experimental)
    ) {
        item {
            SimpleCard {
                
                SettingRow(
                    label = stringResource(R.string.settings_camera_enabled),
                    subLabel = if (hasCamera) 
                        stringResource(R.string.settings_camera_enabled_desc)
                    else 
                        noCameraText
                ) {
                    ModernSwitch(
                        checked = if (hasCamera) experimentalState?.cameraEnabled ?: false else false,
                        enabled = hasCamera,
                        onCheckedChange = {
                            if (hasCamera) {
                                coroutineScope.launch {
                                    viewModel.saveCameraEnabled(it)
                                }
                            } else {
                                Toast.makeText(context, noCameraText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                
                if (experimentalState?.cameraEnabled == true && hasCamera) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    
                    
                    val currentMode = try {
                        CameraMode.valueOf(experimentalState?.cameraMode ?: "SNAPSHOT")
                    } catch (e: Exception) {
                        CameraMode.SNAPSHOT
                    }
                    
                    val modeOptions = listOf(CameraMode.SNAPSHOT, CameraMode.VIDEO)
                    val snapshotModeLabel = stringResource(R.string.settings_camera_mode_snapshot)
                    val videoModeLabel = stringResource(R.string.settings_camera_mode_video)
                    val snapshotModeDesc = stringResource(R.string.settings_camera_mode_snapshot_desc)
                    val videoModeDesc = stringResource(R.string.settings_camera_mode_video_desc)
                    
                    SelectSetting(
                        name = stringResource(R.string.settings_camera_mode),
                        selected = currentMode,
                        items = modeOptions,
                        enabled = true,
                        key = { it.name },
                        value = {
                            when (it) {
                                CameraMode.SNAPSHOT -> snapshotModeLabel
                                CameraMode.VIDEO -> videoModeLabel
                                else -> ""
                            }
                        },
                        onConfirmRequest = {
                            if (it != null) {
                                coroutineScope.launch {
                                    viewModel.saveCameraMode(it)
                                }
                            }
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    
                    
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
                        enabled = cameraOptions.size > 1,
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
                    
                    
                    if (currentMode == CameraMode.SNAPSHOT) {
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        
                        val currentSize = experimentalState?.imageSize ?: 500
                        val sizeOptions = listOf(0, 500, 720, 1080)
                        
                        val sizeOriginalLabel = stringResource(R.string.settings_image_size_original)
                        val size500Label = stringResource(R.string.settings_image_size_500)
                        val size720Label = stringResource(R.string.settings_image_size_720)
                        val size1080Label = stringResource(R.string.settings_image_size_1080)
                        
                        SelectSetting(
                            name = stringResource(R.string.settings_image_size),
                            selected = currentSize,
                            items = sizeOptions,
                            enabled = true,
                            key = { it.toString() },
                            value = {
                                when (it) {
                                    0 -> sizeOriginalLabel
                                    500 -> size500Label
                                    720 -> size720Label
                                    1080 -> size1080Label
                                    else -> "${it}×${it}"
                                }
                            },
                            onConfirmRequest = {
                                if (it != null) {
                                    coroutineScope.launch {
                                        viewModel.saveImageSize(it)
                                    }
                                }
                            }
                        )
                    }
                    
                    
                    if (currentMode == CameraMode.VIDEO) {
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        
                        
                        val currentFps = experimentalState?.videoFps ?: 5
                        val fpsOptions = listOf(1, 2, 3, 5, 8, 10, 15)
                        val fpsFormat = stringResource(R.string.settings_video_fps_format)
                        
                        SelectSetting(
                            name = stringResource(R.string.settings_video_fps),
                            selected = currentFps,
                            items = fpsOptions,
                            enabled = true,
                            key = { it.toString() },
                            value = { String.format(fpsFormat, it) },
                            onConfirmRequest = {
                                if (it != null) {
                                    coroutineScope.launch {
                                        viewModel.saveVideoFps(it)
                                    }
                                }
                            }
                        )
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        
                        
                        val currentResolution = experimentalState?.videoResolution ?: 480
                        val resolutionOptions = listOf(240, 360, 480, 720)
                        
                        val res240Label = stringResource(R.string.settings_video_resolution_240)
                        val res360Label = stringResource(R.string.settings_video_resolution_360)
                        val res480Label = stringResource(R.string.settings_video_resolution_480)
                        val res720Label = stringResource(R.string.settings_video_resolution_720)
                        
                        SelectSetting(
                            name = stringResource(R.string.settings_video_resolution),
                            selected = currentResolution,
                            items = resolutionOptions,
                            enabled = true,
                            key = { it.toString() },
                            value = {
                                when (it) {
                                    240 -> res240Label
                                    360 -> res360Label
                                    480 -> res480Label
                                    720 -> res720Label
                                    else -> "${it}p"
                                }
                            },
                            onConfirmRequest = {
                                if (it != null) {
                                    coroutineScope.launch {
                                        viewModel.saveVideoResolution(it)
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
