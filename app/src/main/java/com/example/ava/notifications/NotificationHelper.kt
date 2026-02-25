package com.example.ava.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ava.MainActivity
import com.example.ava.R

private const val VOICE_SATELLITE_SERVICE_CHANNEL_ID = "VoiceSatelliteService"

fun createVoiceSatelliteServiceNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = "Voice Satellite Background Service"
        val chan = NotificationChannel(
            VOICE_SATELLITE_SERVICE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
    }
}

fun createVoiceSatelliteServiceNotification(context: Context, content: String): Notification {
    val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationCompat.Builder(context, VOICE_SATELLITE_SERVICE_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        NotificationCompat.Builder(context)
    }

    
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }

    val mainIntent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        mainIntent,
        flags
    )
    val notification = notificationBuilder.setOngoing(true)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(content)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setContentIntent(pendingIntent)
        .build()
    return notification
}

