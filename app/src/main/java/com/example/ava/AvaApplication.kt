package com.example.ava

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.ava.notifications.NotificationScenes
import com.example.ava.utils.LocaleUtils
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AvaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleUtils.applyLocale(this)
        setupGlobalExceptionHandler()
        
        NotificationScenes.loadFromAssets(this)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleUtils.applyLocale(base))
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun handleUncaughtException(throwable: Throwable) {
        val logFile = File(getExternalFilesDir(null), "ava_crash_log.txt")
        try {
            FileWriter(logFile, true).use { writer ->
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                writer.append("\n\n--- CRASH REPORT: ${sdf.format(Date())} ---\n")
                writer.append("Error: ${throwable.message}\n")
                writer.append("Stack Trace:\n")
                writer.append(Log.getStackTraceString(throwable))
                writer.append("\n--- END REPORT ---\n")
            }
        } catch (e: Exception) {
            
        }
    }
}
