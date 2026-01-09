package com.example.ava.ui.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.example.ava.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ava.esphome.*
import com.example.ava.permissions.VOICE_SATELLITE_PERMISSIONS
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.utils.translate
import kotlinx.coroutines.launch


private val SlateText = Color(0xFF1E293B)
private val SlateSecondary = Color(0xFF64748B)
private val SlateTertiary = Color(0xFF94A3B8)
private val SlateBorder = Color(0xFFE2E8F0)
private val AccentPrimary = Color(0xFF4F46E5) 
private val AccentBlue = Color(0xFF3B82F6)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF22C55E)
private val AccentRed = Color(0xFFEF4444)

@Composable
fun StartStopVoiceSatellite(
    onNavigateToSettings: () -> Unit = {}
) {
    var service by remember { mutableStateOf<VoiceSatelliteService?>(null) }
    BindToService(
        onConnected = { service = it },
        onDisconnected = { service = null }
    )

    val currentService = service
    if (currentService == null) {
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = AccentBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.connecting_service),
                color = SlateTertiary,
                fontSize = 14.sp
            )
        }
    } else {
        val serviceState by currentService.voiceSatelliteState.collectAsStateWithLifecycle(Stopped)
        val isStarted = serviceState !is Stopped
        val resources = LocalContext.current.resources
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            MainControlButton(
                isStarted = isStarted,
                onStart = { currentService.startVoiceSatellite() },
                onStop = { currentService.stopVoiceSatellite() }
            )
            
            
            Text(
                text = remember(serviceState) { serviceState.translate(resources) },
                color = if (isStarted) SlateSecondary else SlateTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
private fun StatusCard(state: EspHomeState) {
    val (title, subtitle, bgColor) = when (state) {
        is Connected -> Triple(stringResource(R.string.status_connected), "Connected", AccentGreen)
        is Stopped -> Triple(stringResource(R.string.status_stopped), "Stopped", SlateTertiary)
        is Disconnected -> Triple(stringResource(R.string.status_disconnected), "Disconnected", AccentRed)
        is ServerError -> Triple(stringResource(R.string.status_error), "Error", AccentRed)
        else -> Triple(stringResource(R.string.status_working), "Working...", AccentBlue)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor.copy(alpha = 0.1f),
        modifier = Modifier.padding(horizontal = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(bgColor, CircleShape)
            )
            
            Column {
                Text(
                    text = title,
                    color = SlateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = SlateTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}


@Composable
private fun MainControlButton(
    isStarted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val registerPermissionsResult = rememberLaunchWithMultiplePermissions(
        onPermissionGranted = onStart,
        onPermissionDenied = { }
    )
    
    
    val buttonColor by animateColorAsState(
        targetValue = if (isStarted) AccentPrimary else Color.White,
        animationSpec = tween(300),
        label = "buttonColor"
    )
    
    
    val borderColor = if (isStarted) Color.Transparent else SlateBorder
    
    Box(
        modifier = Modifier
            .size(160.dp)
            .shadow(
                elevation = if (isStarted) 20.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isStarted) AccentBlue.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f),
                spotColor = if (isStarted) AccentBlue.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
            )
            .clip(CircleShape)
            .background(buttonColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable {
                if (isStarted) {
                    onStop()
                } else {
                    registerPermissionsResult.launch(VOICE_SATELLITE_PERMISSIONS)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            
            Icon(
                painter = painterResource(R.drawable.power_24px),
                contentDescription = if (isStarted) "Stop" else "Start",
                tint = if (isStarted) Color.White else SlateTertiary,
                modifier = Modifier.size(48.dp)
            )
            
            
            Text(
                text = if (isStarted) "STOP SERVICE" else "START SERVICE",
                color = if (isStarted) Color.White.copy(alpha = 0.8f) else SlateTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BindToService(onConnected: (VoiceSatelliteService) -> Unit, onDisconnected: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                (binder as? VoiceSatelliteService.VoiceSatelliteBinder)?.let {
                    onConnected(it.service)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                onDisconnected()
            }
        }
        val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
        coroutineScope.launch {
            try {
                val bound =
                    context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                if (!bound)
                    Log.e("BindToService", "Cannot bind to VoiceAssistantService")
            } catch (e: Exception) {
                Log.e("BindToService", "Error binding to service", e)
            }
        }


        onDispose {
            context.unbindService(serviceConnection)
        }
    }
}

@Composable
fun rememberLaunchWithMultiplePermissions(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (deniedPermissions: Array<String>) -> Unit = { }
): ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> {
    val registerPermissionsResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val deniedPermissions = granted.filter { !it.value }.keys.toTypedArray()
        if (deniedPermissions.isEmpty()) {
            onPermissionGranted()
        } else {
            onPermissionDenied(deniedPermissions)
        }
    }
    return registerPermissionsResult
}