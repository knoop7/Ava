package com.example.ava.permissions

import android.Manifest
import android.os.Build

val VOICE_SATELLITE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA)
} else {
    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
}