package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.ava.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class VinylCoverService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var coverImageView: ImageView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var currentCoverUrl: String? = null
    private var vinylSize: Int = 530
    private var windowParams: WindowManager.LayoutParams? = null
    
    private var currentBitmap: Bitmap? = null
    private var defaultMusicBitmap: Bitmap? = null
    private var loadedCoverBitmap: Bitmap? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        vinylSize = minOf(screenWidth, screenHeight)
        
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        coverImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        
        defaultMusicBitmap = createDefaultMusicBitmapInternal()
        currentBitmap = defaultMusicBitmap
        coverImageView?.setImageBitmap(defaultMusicBitmap)
        
        container.addView(coverImageView)
        overlayView = container

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.CENTER
        }

        windowParams = params
        
        try {
            windowManager?.addView(overlayView, params)
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create vinyl cover window", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> {
                val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
                
                if (coverUrl != null && coverUrl != currentCoverUrl) {
                    handler.post {
                        setImageBitmapSafely(getDefaultMusicBitmap())
                    }
                    loadCoverImage(coverUrl)
                } else if (coverUrl != null && coverUrl == currentCoverUrl && loadedCoverBitmap != null) {
                    handler.post {
                        setImageBitmapSafely(loadedCoverBitmap)
                    }
                } else if (coverUrl == null && currentCoverUrl != null) {
                    loadCoverImage(currentCoverUrl)
                }
                show()
            }
            ACTION_HIDE -> {
                hide()
            }
            ACTION_UPDATE_COVER -> {
                val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
                loadCoverImage(coverUrl)
            }
        }
    }

    private fun createDefaultMusicBitmapInternal(): Bitmap {
        val bmp = Bitmap.createBitmap(vinylSize, vinylSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1A1A1A"))
        
        try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_music_note)
            if (drawable != null) {
                val iconSize = (vinylSize * 0.35f).toInt()
                val left = (vinylSize - iconSize) / 2
                val top = (vinylSize - iconSize) / 2
                drawable.setBounds(left, top, left + iconSize, top + iconSize)
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading music note icon", e)
        }
        
        return bmp
    }
    
    private fun getDefaultMusicBitmap(): Bitmap {
        return defaultMusicBitmap ?: createDefaultMusicBitmapInternal().also { defaultMusicBitmap = it }
    }
    
    private fun setImageBitmapSafely(newBitmap: Bitmap?) {
        val oldBitmap = currentBitmap
        currentBitmap = newBitmap
        coverImageView?.setImageBitmap(newBitmap)
        
        if (oldBitmap != null && oldBitmap != defaultMusicBitmap && oldBitmap != loadedCoverBitmap && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun loadCoverImage(url: String?) {
        if (url == null) return
        currentCoverUrl = url
        
        coroutineScope.launch {
            var actualUrl = url
            try {
                
                if (url.contains("cloud_music") && url.contains("data=")) {
                    try {
                        val uri = java.net.URI(url)
                        val query = uri.query
                        val dataParam = query.split("&").find { it.startsWith("data=") }?.substring(5)
                        if (dataParam != null) {
                            val decodedString = String(android.util.Base64.decode(dataParam, android.util.Base64.DEFAULT))
                            val idParam = decodedString.split("&").find { it.startsWith("id=") }?.substring(3)
                            
                            if (idParam != null) {
                                val neteaseApiUrl = "http://music.163.com/api/song/detail/?id=$idParam&ids=%5B$idParam%5D"
                                runCatching {
                                    val jsonStr = withContext(Dispatchers.IO) { URL(neteaseApiUrl).readText() }
                                    val picUrl = org.json.JSONObject(jsonStr).optJSONArray("songs")?.getJSONObject(0)?.optJSONObject("album")?.optString("picUrl")
                                    if (!picUrl.isNullOrEmpty()) actualUrl = picUrl
                                }.onFailure {
                                    
                                    val httpUrl = "http://music.163.com/api/song/detail/?id=$idParam&ids=%5B$idParam%5D"
                                    runCatching {
                                        val jsonStr = withContext(Dispatchers.IO) { URL(httpUrl).readText() }
                                        val picUrl = org.json.JSONObject(jsonStr).optJSONArray("songs")?.getJSONObject(0)?.optJSONObject("album")?.optString("picUrl")
                                        if (!picUrl.isNullOrEmpty()) actualUrl = picUrl
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error", e)
                    }
                }

                val finalBitmap = withContext(Dispatchers.IO) {
                    val connection = URL(actualUrl).openConnection()
                    connection.connectTimeout = 5000
                    connection.inputStream.use { input ->
                        val bytes = input.readBytes()
                        
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        
                        options.inSampleSize = calculateInSampleSize(options, vinylSize, vinylSize)
                        options.inJustDecodeBounds = false
                        
                        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null
                        
                        val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, vinylSize, vinylSize, true)
                        
                        if (rawBitmap != scaledBitmap && !rawBitmap.isRecycled) rawBitmap.recycle()
                        
                        return@withContext scaledBitmap
                    }
                }
                
                handler.post {
                    if (finalBitmap != null) {
                        setImageBitmapSafely(finalBitmap)
                        loadedCoverBitmap = finalBitmap
                        if (overlayView?.visibility == View.VISIBLE) {
                            
                        }
                    } else {
                        loadedCoverBitmap = null
                        setImageBitmapSafely(getDefaultMusicBitmap())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Load failed, URL: $actualUrl", e)
                System.gc()
            }
        }
    }
    
    private fun show() {
        handler.post {
            
            try {
                overlayView?.let { view ->
                    if (view.visibility == View.GONE) {
                        windowManager?.removeView(view)
                        windowManager?.addView(view, windowParams)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-add view", e)
            }
            overlayView?.visibility = View.VISIBLE
        }
    }

    private fun hide() {
        handler.post {
            overlayView?.visibility = View.GONE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        coroutineScope.cancel()
        
        currentBitmap?.let { if (!it.isRecycled) it.recycle() }
        currentBitmap = null
        loadedCoverBitmap?.let { if (!it.isRecycled) it.recycle() }
        loadedCoverBitmap = null
        defaultMusicBitmap?.let { if (!it.isRecycled) it.recycle() }
        defaultMusicBitmap = null
        
        try { overlayView?.let { windowManager?.removeView(it) } } catch (e: Exception) {
            Log.e(TAG, "Failed to remove vinyl cover window", e)
        }
    }

    companion object {
        private const val TAG = "VinylCoverService"
        
        const val ACTION_SHOW = "com.example.ava.SHOW_VINYL"
        const val ACTION_HIDE = "com.example.ava.HIDE_VINYL"
        const val ACTION_UPDATE_COVER = "com.example.ava.UPDATE_VINYL_COVER"
        const val EXTRA_COVER_URL = "cover_url"

        fun show(context: Context, coverUrl: String?) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_SHOW
                coverUrl?.let { putExtra(EXTRA_COVER_URL, it) }
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun updateCover(context: Context, coverUrl: String) {
            val intent = Intent(context, VinylCoverService::class.java).apply {
                action = ACTION_UPDATE_COVER
                putExtra(EXTRA_COVER_URL, coverUrl)
            }
            context.startService(intent)
        }
    }
}
