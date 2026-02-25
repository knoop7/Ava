package com.example.ava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ava.services.VoiceSatelliteService

/**
 * 通用广播接收器，允许外部应用控制 Ava 功能
 * 
 * 支持的 Action:
 * - com.example.ava.ACTION_TOGGLE_MIC: 切换麦克风静音状态
 * - com.example.ava.ACTION_MUTE_MIC: 静音麦克风
 * - com.example.ava.ACTION_UNMUTE_MIC: 取消静音麦克风
 * - com.example.ava.ACTION_WAKE: 手动唤醒语音助手
 * - com.example.ava.ACTION_STOP: 停止当前语音会话
 * 
 * 使用示例 (adb):
 * adb shell am broadcast -a com.example.ava.ACTION_TOGGLE_MIC
 * adb shell am broadcast -a com.example.ava.ACTION_WAKE
 * 
 * 使用示例 (Tasker/其他应用):
 * Intent action: com.example.ava.ACTION_TOGGLE_MIC
 * Package: com.example.ava
 */
class AvaControlReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AvaControlReceiver"
        
        const val ACTION_TOGGLE_MIC = "com.example.ava.ACTION_TOGGLE_MIC"
        const val ACTION_MUTE_MIC = "com.example.ava.ACTION_MUTE_MIC"
        const val ACTION_UNMUTE_MIC = "com.example.ava.ACTION_UNMUTE_MIC"
        const val ACTION_WAKE = "com.example.ava.ACTION_WAKE"
        const val ACTION_STOP = "com.example.ava.ACTION_STOP"
        const val ACTION_START_SERVICE = "com.example.ava.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.ava.ACTION_STOP_SERVICE"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received action: $action")
        
        when (action) {
            ACTION_TOGGLE_MIC -> {
                Log.d(TAG, "Toggling microphone mute state")
                VoiceSatelliteService.toggleMicMute()
            }
            ACTION_MUTE_MIC -> {
                Log.d(TAG, "Muting microphone")
                VoiceSatelliteService.setMicMute(true)
            }
            ACTION_UNMUTE_MIC -> {
                Log.d(TAG, "Unmuting microphone")
                VoiceSatelliteService.setMicMute(false)
            }
            ACTION_WAKE -> {
                Log.d(TAG, "Manual wake triggered")
                VoiceSatelliteService.manualWake()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping voice session")
                VoiceSatelliteService.stopVoiceSession()
            }
            ACTION_START_SERVICE -> {
                Log.d(TAG, "Starting VoiceSatelliteService")
                val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service", e)
                }
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stopping VoiceSatelliteService")
                val serviceIntent = Intent(context, VoiceSatelliteService::class.java)
                context.stopService(serviceIntent)
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }
    }
}
