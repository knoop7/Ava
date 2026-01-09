package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class ScreensaverWebViewService : Service() {

    private var windowManager: WindowManager? = null
    private var containerView: ViewGroup? = null
    private var webView: WebView? = null
    private var currentUrl: String = ""
    private var hasTriedHttpFallback = false

    companion object {
        private const val TAG = "ScreensaverWebView"

        fun show(context: Context, url: String) {
            val intent = Intent(context, ScreensaverWebViewService::class.java).apply {
                action = "ACTION_SHOW"
                putExtra("url", url)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, ScreensaverWebViewService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }

        fun updateUrl(context: Context, url: String) {
            val intent = Intent(context, ScreensaverWebViewService::class.java).apply {
                action = "ACTION_UPDATE"
                putExtra("url", url)
            }
            context.startService(intent)
        }
        
        fun pause(context: Context) {
            val intent = Intent(context, ScreensaverWebViewService::class.java).apply {
                action = "ACTION_PAUSE"
            }
            context.startService(intent)
        }
        
        fun resume(context: Context) {
            val intent = Intent(context, ScreensaverWebViewService::class.java).apply {
                action = "ACTION_RESUME"
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_SHOW" -> {
                val url = intent.getStringExtra("url") ?: ""
                showWebView(url)
            }
            "ACTION_HIDE" -> {
                hideWebView()
                stopSelf()
            }
            "ACTION_UPDATE" -> {
                val url = intent.getStringExtra("url") ?: ""
                updateUrlInternal(url)
            }
            "ACTION_PAUSE" -> {
                webView?.onPause()
                webView?.pauseTimers()
                Log.d(TAG, "WebView paused")
            }
            "ACTION_RESUME" -> {
                webView?.resumeTimers()
                webView?.onResume()
                Log.d(TAG, "WebView resumed")
            }
        }
        return START_STICKY
    }

    private fun showWebView(url: String) {
        currentUrl = url
        hasTriedHttpFallback = false
        if (containerView != null) {
            updateUrlInternal(url)
            return
        }
        if (url.isBlank()) return

        try {
            webView = WebView(this)
            setupWebView()

            val params = WindowManager.LayoutParams().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    type = WindowManager.LayoutParams.TYPE_PHONE
                }
                @Suppress("DEPRECATION")
                flags = WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            val container = FrameLayout(this).apply {
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        ScreensaverController.onUserInteraction()
                    }
                    true
                }
                isFocusableInTouchMode = true
                requestFocus()
            }

            containerView = container
            windowManager?.addView(containerView, params)
            loadUrl(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show screensaver WebView", e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView?.let { wv ->
            val settings = wv.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            wv.setBackgroundColor(Color.BLACK)
            wv.isVerticalScrollBarEnabled = false
            wv.isHorizontalScrollBarEnabled = false
            wv.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    ScreensaverController.onUserInteraction()
                }
                true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wv.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
                ) {
                    val url = view?.url ?: currentUrl
                    if (!hasTriedHttpFallback && url.startsWith("https://")) {
                        hasTriedHttpFallback = true
                        handler?.cancel()
                        val httpUrl = url.replaceFirst("https://", "http://")
                        view?.loadUrl(httpUrl)
                    } else {
                        handler?.proceed()
                    }
                }
                
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    Log.e(TAG, "Renderer crashed, didCrash=${detail?.didCrash()}, priority=${detail?.rendererPriorityAtExit()}")
                    webView?.let { wv ->
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                    webView = null
                    if (currentUrl.isNotBlank()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            recreateWebView()
                        }, 500)
                    }
                    return true
                }
            }
        }
    }

    private fun recreateWebView() {
        try {
            webView = WebView(this)
            setupWebView()
            containerView?.let { container ->
                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            loadUrl(currentUrl)
            Log.d(TAG, "WebView recreated after crash")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate WebView", e)
        }
    }

    private fun loadUrl(url: String) {
        if (url.isBlank()) return
        val finalUrl = when {
            url.startsWith("file://") -> url
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }
        webView?.loadUrl(finalUrl)
    }

    private fun updateUrlInternal(url: String) {
        currentUrl = url
        hasTriedHttpFallback = false
        if (containerView == null) {
            showWebView(url)
        } else {
            loadUrl(url)
        }
    }

    private fun hideWebView() {
        try {
            webView?.let { wv ->
                wv.stopLoading()
                wv.onPause()
                wv.pauseTimers()
                wv.loadUrl("about:blank")
                (wv.parent as? ViewGroup)?.removeView(wv)
            }
            containerView?.let { container ->
                windowManager?.removeView(container)
            }
            webView?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide screensaver WebView", e)
        }
        containerView = null
        webView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hideWebView()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
