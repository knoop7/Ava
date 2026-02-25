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
import com.example.ava.ui.theme.SlateText
import com.example.ava.ui.theme.SlateSecondary
import com.example.ava.ui.theme.SlateTertiary
import com.example.ava.ui.theme.SlateBorder
import com.example.ava.ui.theme.AccentBlue
import com.example.ava.ui.theme.AccentViolet
import com.example.ava.ui.theme.AccentGreen
import com.example.ava.ui.theme.AccentRed

@Composable
fun StartStopVoiceSatellite(
    onNavigateToSettings: () -> Unit = {},
    isDarkMode: Boolean = false
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
                color = if (isDarkMode) Color(0xFFA78B73) else AccentBlue,
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
                onStop = { currentService.stopVoiceSatellite() },
                isDarkMode = isDarkMode
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
private fun MainControlButton(
    isStarted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    isDarkMode: Boolean = false
) {
    val registerPermissionsResult = rememberLaunchWithMultiplePermissions(
        onPermissionGranted = onStart,
        onPermissionDenied = { }
    )
    
    
    val buttonColor by animateColorAsState(
        targetValue = when {
            isDarkMode && isStarted -> Color(0xFF1A2332)
            isDarkMode -> Color(0xFF1F1F1F)
            isStarted -> AccentBlue
            else -> Color.White
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )
    
    
    val borderColor = when {
        isDarkMode && isStarted -> Color(0xFF2A3A4A)
        isStarted -> Color.Transparent
        isDarkMode -> Color(0xFF333333)
        else -> SlateBorder
    }
    
    val iconTint = when {
        isStarted -> Color.White
        isDarkMode -> Color(0xFF6B7280)
        else -> SlateTertiary
    }
    
    val textColor = when {
        isStarted -> Color.White.copy(alpha = 0.8f)
        isDarkMode -> Color(0xFF6B7280)
        else -> SlateTertiary
    }
    
    Box(
        modifier = Modifier
            .size(160.dp)
            .shadow(
                elevation = if (isStarted) 20.dp else if (isDarkMode) 0.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (isStarted && !isDarkMode) AccentBlue.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f),
                spotColor = if (isStarted && !isDarkMode) AccentBlue.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
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
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
            
            
            Text(
                text = if (isStarted) "STOP SERVICE" else "START SERVICE",
                color = textColor,
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
    DisposableEffect(Unit) {
        var bound = false
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
        try {
            bound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound)
                Log.e("BindToService", "Cannot bind to VoiceSatelliteService")
        } catch (e: Exception) {
            Log.e("BindToService", "Error binding to service", e)
        }

        onDispose {
            if (bound) {
                try {
                    context.unbindService(serviceConnection)
                } catch (e: Exception) {
                    Log.e("BindToService", "Error unbinding service", e)
                }
            }
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