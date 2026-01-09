package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.ava.R


class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var scrollView: ScrollView? = null
    private var assistantTextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(48, 32, 48, 48)  
        }

        
        val assistantLabel = TextView(this).apply {
            text = getString(R.string.floating_ai_reply)
            setTextColor(Color.parseColor("#333333"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 0, 0, 24)  
        }
        container.addView(assistantLabel)

        
        scrollView = ScrollView(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            )
            lp.weight = 1f
            layoutParams = lp
            isScrollbarFadingEnabled = false
            setBackgroundColor(Color.WHITE)  
        }

        
        assistantTextView = TextView(this).apply {
            setTextColor(Color.parseColor("#1565C0"))  
            textSize = 26f  
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 16, 16, 16)
            text = ""
            maxLines = Int.MAX_VALUE
            isSingleLine = false
            setLineSpacing(0f, 1.4f)  
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        scrollView?.addView(assistantTextView)
        container.addView(scrollView)

        overlayView = container

        
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
        }

        try {
            windowManager?.addView(overlayView, params)
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating window", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_USER_TEXT -> {
                
                handler.post {
                    overlayView?.visibility = View.VISIBLE
                    assistantTextView?.text = getString(R.string.thinking)
                    currentText = ""
                }
            }
            ACTION_SHOW_ASSISTANT_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                showAssistantTextWithTypewriter(text)
            }
            ACTION_APPEND_TEXT -> {
                
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                appendText(text)
            }
            ACTION_HIDE -> {
                
                hideOverlay()
            }
            ACTION_CLEAR -> {
                clearText()
            }
        }
    }

    private var currentText = ""
    private var typewriterRunnable: Runnable? = null
    private var autoHideRunnable: Runnable? = null

    private fun showAssistantTextWithTypewriter(text: String) {
        handler.post {
            
            typewriterRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            
            
            currentText = ""
            var index = 0
            
            typewriterRunnable = object : Runnable {
                override fun run() {
                    if (index < text.length) {
                        
                        if (index == 0) {
                            overlayView?.visibility = View.VISIBLE
                        }
                        
                        currentText += text[index]
                        assistantTextView?.text = currentText
                        
                        
                        scrollView?.post {
                            scrollView?.fullScroll(View.FOCUS_DOWN)
                        }
                        
                        index++
                        
                        val delay = when (text.getOrNull(index - 1)) {
                            '。', '！', '？', '.', '!', '?' -> 650L
                            '，', ',', '、', '；', ';', ':' -> 400L
                            '\n' -> 500L
                            else -> 200L
                        }
                        handler.postDelayed(this, delay)
                    } else {
                        
                        scheduleAutoHide()
                    }
                }
            }
            
            handler.postDelayed(typewriterRunnable!!, 1500)
        }
    }
    
    private fun scheduleAutoHide() {
        autoHideRunnable?.let { handler.removeCallbacks(it) }
        autoHideRunnable = Runnable {
            overlayView?.visibility = View.GONE
            currentText = ""
            assistantTextView?.text = ""
        }
        handler.postDelayed(autoHideRunnable!!, 3000)
    }

    private fun appendText(text: String) {
        handler.post {
            overlayView?.visibility = View.VISIBLE
            currentText += text
            assistantTextView?.text = currentText
            
            
            scrollView?.post {
                scrollView?.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun hideOverlay() {
        handler.post {
            typewriterRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            overlayView?.visibility = View.GONE
            currentText = ""
            assistantTextView?.text = ""
        }
    }

    private fun clearText() {
        handler.post {
            typewriterRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            currentText = ""
            assistantTextView?.text = ""
            overlayView?.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating window", e)
        }
    }

    companion object {
        private const val TAG = "FloatingWindowService"
        
        const val ACTION_SHOW_USER_TEXT = "com.example.ava.SHOW_USER_TEXT"
        const val ACTION_SHOW_ASSISTANT_TEXT = "com.example.ava.SHOW_ASSISTANT_TEXT"
        const val ACTION_APPEND_TEXT = "com.example.ava.APPEND_TEXT"
        const val ACTION_HIDE = "com.example.ava.HIDE_FLOATING"
        const val ACTION_CLEAR = "com.example.ava.CLEAR_FLOATING"
        const val EXTRA_TEXT = "text"

        fun showUserText(context: Context, text: String) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_USER_TEXT
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }

        fun showAssistantText(context: Context, text: String) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_ASSISTANT_TEXT
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }

        fun appendText(context: Context, text: String) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_APPEND_TEXT
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun clear(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_CLEAR
            }
            context.startService(intent)
        }
    }
}
