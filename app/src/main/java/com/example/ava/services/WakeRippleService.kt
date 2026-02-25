package com.example.ava.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.example.ava.ui.views.WakeRippleView

class WakeRippleService : Service() {
    
    private var windowManager: WindowManager? = null
    private var rippleView: WakeRippleView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    companion object {
        private var instance: WakeRippleService? = null
        private const val AUTO_DISMISS_DELAY = 2500L
        
        fun show(context: Context, color: Int = Color.parseColor("#00FF88")) {
            val intent = Intent(context, WakeRippleService::class.java).apply {
                action = "ACTION_SHOW"
                putExtra("color", color)
            }
            context.startService(intent)
        }
        
        fun showAt(context: Context, x: Float, y: Float, color: Int = Color.parseColor("#00FF88")) {
            val intent = Intent(context, WakeRippleService::class.java).apply {
                action = "ACTION_SHOW_AT"
                putExtra("x", x)
                putExtra("y", y)
                putExtra("color", color)
            }
            context.startService(intent)
        }
        
        fun hide(context: Context) {
            val intent = Intent(context, WakeRippleService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_SHOW" -> {
                val color = intent.getIntExtra("color", Color.parseColor("#00FF88"))
                showRipple(color)
            }
            "ACTION_SHOW_AT" -> {
                val x = intent.getFloatExtra("x", -1f)
                val y = intent.getFloatExtra("y", -1f)
                val color = intent.getIntExtra("color", Color.parseColor("#00FF88"))
                showRippleAt(x, y, color)
            }
            "ACTION_HIDE" -> {
                hideRipple()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun showRipple(color: Int) {
        handler.removeCallbacksAndMessages(null)
        

        hideRippleView()
        createRippleView()
        
        rippleView?.apply {
            setColor(color)
            post { startRipple() }
        }
        
        handler.postDelayed({
            hideRipple()
        }, AUTO_DISMISS_DELAY)
    }
    
    private fun showRippleAt(x: Float, y: Float, color: Int) {
        handler.removeCallbacksAndMessages(null)
        
        if (rippleView == null) {
            createRippleView()
        }
        
        rippleView?.apply {
            setColor(color)
            post {
                if (x >= 0 && y >= 0) {
                    startRippleAt(x, y)
                } else {
                    startRipple()
                }
            }
        }
        
        handler.postDelayed({
            hideRipple()
        }, AUTO_DISMISS_DELAY)
    }
    
    private fun createRippleView() {
        rippleView = WakeRippleView(this)
        
        val params = WindowManager.LayoutParams().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                type = WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            @Suppress("DEPRECATION")
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        
        try {
            windowManager?.addView(rippleView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun hideRippleView() {
        rippleView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {

            }
        }
        rippleView = null
    }
    
    private fun hideRipple() {
        handler.removeCallbacksAndMessages(null)
        hideRippleView()
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideRipple()
        instance = null
    }
}
