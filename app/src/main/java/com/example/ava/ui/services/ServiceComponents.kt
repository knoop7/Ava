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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ava.R
import com.example.ava.esphome.VoiceSatelliteState
import com.example.ava.services.VoiceSatelliteService
import com.example.ava.permissions.VOICE_SATELLITE_PERMISSIONS
import kotlin.collections.filter
import kotlin.collections.isEmpty
import kotlin.collections.toTypedArray
import kotlin.jvm.java
import kotlin.let

@Composable
fun StartStopVoiceSatellite() {
    var _service by remember { mutableStateOf<VoiceSatelliteService?>(null) }
    BindToService(
        onConnected = { _service = it },
        onDisconnected = { _service = null }
    )

    val service = _service
    if (service == null) {
        Text(
            text = "Service disconnected",
            color = MaterialTheme.colorScheme.error
        )
    } else {
        val serviceState by service.voiceSatelliteState.collectAsStateWithLifecycle(
            VoiceSatelliteState.Stopped()
        )

        Text(
            text = stateText(serviceState),
            color = stateColor(serviceState),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        StartStopWithPermissionsButton(
            permissions = VOICE_SATELLITE_PERMISSIONS,
            isStarted = serviceState !is VoiceSatelliteState.Stopped,
            onStart = { service.startVoiceSatellite() },
            onStop = { service.stopVoiceSatellite() },
            onPermissionDenied = { /*TODO*/ }
        )
    }
}

@Composable
fun stateText(state: VoiceSatelliteState) = stringResource(
    when (state) {
        is VoiceSatelliteState.Stopped -> R.string.satellite_state_stopped
        is VoiceSatelliteState.Disconnected -> R.string.satellite_state_disconnected
        is VoiceSatelliteState.Idle -> R.string.satellite_state_idle
        is VoiceSatelliteState.Listening -> R.string.satellite_state_listening
        is VoiceSatelliteState.Processing -> R.string.satellite_state_processing
        is VoiceSatelliteState.Responding -> R.string.satellite_state_responding
    }
)

@Composable
fun stateColor(state: VoiceSatelliteState) = when (state) {
    is VoiceSatelliteState.Stopped, is VoiceSatelliteState.Disconnected -> MaterialTheme.colorScheme.error
    is VoiceSatelliteState.Idle -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun BindToService(onConnected: (VoiceSatelliteService) -> Unit, onDisconnected: () -> Unit) {
    val context = LocalContext.current
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
        val bound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!bound)
            Log.e("BindToService", "Cannot bind to VoiceAssistantService")

        onDispose {
            context.unbindService(serviceConnection)
        }
    }
}

@Composable
fun StartStopWithPermissionsButton(
    permissions: Array<String>,
    isStarted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPermissionDenied: (deniedPermissions: Array<String>) -> Unit
) {
    val registerPermissionsResult = rememberLaunchWithMultiplePermissions(
        onPermissionGranted = onStart,
        onPermissionDenied = onPermissionDenied
    )
    val content = if (isStarted) "Stop" else "Start"
    ExtendedFloatingActionButton(
        onClick = {
            if (isStarted)
                onStop()
            else
                registerPermissionsResult.launch(permissions)
        }
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.titleMedium
        )
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