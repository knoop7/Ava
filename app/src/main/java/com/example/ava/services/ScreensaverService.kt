package com.example.ava.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.ava.R
import com.example.ava.notifications.createScreensaverServiceNotification
import com.example.ava.notifications.createScreensaverServiceNotificationChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ScreensaverService : LifecycleService() {
    
    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var imageView: ImageView? = null
    private var mjpegJob: Job? = null
    private var isForeground = false
    
    companion object {
        private const val TAG = "ScreensaverService"
        private const val NOTIFICATION_ID = 3
        private var instance: ScreensaverService? = null
        
        fun show(context: Context, imageUrl: String) {
            val intent = Intent(context, ScreensaverService::class.java).apply {
                action = "ACTION_SHOW"
                putExtra("image_url", imageUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun hide(context: Context) {
            val intent = Intent(context, ScreensaverService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }
        
        fun updateImage(context: Context, imageUrl: String) {
            val intent = Intent(context, ScreensaverService::class.java).apply {
                action = "ACTION_UPDATE"
                putExtra("image_url", imageUrl)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            "ACTION_SHOW" -> {
                ensureForeground()
                val imageUrl = intent.getStringExtra("image_url") ?: ""
                showOverlay(imageUrl)
            }
            "ACTION_HIDE" -> {
                hideOverlay()
                stopForegroundIfNeeded()
                stopSelf()
            }
            "ACTION_UPDATE" -> {
                ensureForeground()
                val imageUrl = intent.getStringExtra("image_url") ?: ""
                updateImage(imageUrl)
            }
        }
        
        return START_STICKY
    }

    private fun ensureForeground() {
        if (isForeground) return
        createScreensaverServiceNotificationChannel(this)
        startForeground(NOTIFICATION_ID, createScreensaverServiceNotification(this))
        isForeground = true
    }

    private fun stopForegroundIfNeeded() {
        if (!isForeground) return
        stopForeground(true)
        isForeground = false
    }
    
    private fun showOverlay(imageUrl: String) {
        if (overlayView != null) return
        
        try {
            
            overlayView = LayoutInflater.from(this).inflate(R.layout.screensaver_overlay, null)
            imageView = overlayView?.findViewById(R.id.screensaver_image)
            
            
            val params = WindowManager.LayoutParams().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    type = WindowManager.LayoutParams.TYPE_PHONE
                }
                @Suppress("DEPRECATION")
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.TOP or Gravity.LEFT
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            
            windowManager?.addView(overlayView, params)
            
            
            loadImage(imageUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }
    
    private fun hideOverlay() {
        mjpegJob?.cancel()
        mjpegJob = null
        
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
        }
        overlayView = null
        imageView = null
    }
    
    private fun updateImage(imageUrl: String) {
        if (overlayView == null) {
            showOverlay(imageUrl)
        } else {
            loadImage(imageUrl)
        }
    }
    
    private fun loadImage(imageUrl: String) {
        if (imageUrl.isEmpty()) {
            hideOverlay()
            stopForegroundIfNeeded()
            stopSelf()
            return
        }
        
        
        mjpegJob?.cancel()
        mjpegJob = null
        
        lifecycleScope.launch {
            try {
                
                val isMjpeg = isMjpegUrl(imageUrl)
                val isGo2rtc = imageUrl.contains("/api/frame.jpeg")
                
                when {
                    isMjpeg -> {
                        loadMjpegStream(imageUrl)
                    }
                    isGo2rtc -> {
                        loadGo2rtcStream(imageUrl)
                    }
                    else -> {
                        loadStaticImage(imageUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image", e)
            }
        }
    }
    
    private suspend fun isMjpegUrl(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()
                
                val contentType = connection.getHeaderField("Content-Type") ?: ""
                connection.disconnect()
                
                contentType.contains("multipart/x-mixed-replace") || 
                url.contains("mjpeg") || 
                url.contains("stream")
            } catch (e: Exception) {
                
                url.contains("mjpeg") || url.contains("stream")
            }
        }
    }
    
    private suspend fun loadStaticImage(url: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    withContext(Dispatchers.Main) {
                        imageView?.setImageBitmap(bitmap)
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load static image", e)
            }
        }
    }
    
    private fun loadMjpegStream(url: String) {
        mjpegJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    
                    val contentType = connection.getHeaderField("Content-Type") ?: ""
                    val boundary = extractBoundary(contentType)
                    
                    val inputStream = connection.inputStream
                    parseMjpegStream(inputStream, boundary)
                    inputStream.close()
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load MJPEG stream", e)
            }
        }
    }
    
    private fun loadGo2rtcStream(url: String) {
        mjpegJob = lifecycleScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                try {
                    loadStaticImage(url)
                    
                    delay(3000)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load go2rtc frame", e)
                    delay(3000) 
                }
            }
        }
    }
    
    private fun extractBoundary(contentType: String): String {
        
        
        val boundaryPattern = "boundary=([^;]+)".toRegex()
        val match = boundaryPattern.find(contentType)
        return match?.groupValues?.get(1)?.trim() ?: "--boundary"
    }
    
    private suspend fun parseMjpegStream(inputStream: java.io.InputStream, boundary: String) {
        val buffer = ByteArray(8192)
        val frameBuffer = ByteArrayOutputStream()
        var searchOffset = 0
        
        while (currentCoroutineContext().isActive) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break
            
            frameBuffer.write(buffer, 0, bytesRead)
            
            
            val frameData = frameBuffer.toByteArray()
            val startIndex = findJpegStart(frameData)
            val endIndex = findJpegEnd(frameData, startIndex)
            
            if (startIndex != -1 && endIndex != -1) {
                val jpegData = frameData.copyOfRange(startIndex, endIndex)
                
                
                val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        imageView?.setImageBitmap(bitmap)
                    }
                }
                
                
                frameBuffer.reset()
                if (endIndex < frameData.size) {
                    frameBuffer.write(frameData, endIndex, frameData.size - endIndex)
                }
                
                
                delay(100) 
            }
        }
    }
    
    private fun findJpegStart(data: ByteArray): Int {
        for (i in 0 until data.size - 3) {
            if (data[i].toInt() == 0xFF && 
                data[i + 1].toInt() == 0xD8 && 
                data[i + 2].toInt() == 0xFF) {
                return i
            }
        }
        return -1
    }
    
    private fun findJpegEnd(data: ByteArray, startIndex: Int): Int {
        for (i in startIndex + 1 until data.size - 1) {
            if (data[i].toInt() == 0xFF && 
                data[i + 1].toInt() == 0xD9) {
                return i + 2
            }
        }
        return -1
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        stopForegroundIfNeeded()
        instance = null
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
