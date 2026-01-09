package com.example.ava.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ava.settings.BrowserSettings
import com.example.ava.settings.BrowserSettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WebViewService : LifecycleService() {
    
    private var windowManager: WindowManager? = null
    private var containerView: ViewGroup? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var webView: WebView? = null
    private lateinit var browserSettingsStore: BrowserSettingsStore
    private var currentSettings: BrowserSettings = BrowserSettings.DEFAULT
    private var originalUrl: String = "" 
    
    companion object {
        private const val TAG = "WebViewService"
        private var instance: WebViewService? = null
        private var pendingRestoreUrl: String? = null 
        private var isInSettings = false 
        
        fun show(context: Context, url: String) {
            
            if (isInSettings) {
                pendingRestoreUrl = url
                return
            }
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_SHOW"
                putExtra("url", url)
            }
            context.startService(intent)
        }
        
        fun hideForSettings(context: Context, currentUrl: String) {
            pendingRestoreUrl = currentUrl
            hide(context)
        }
        
        fun restoreIfPending(context: Context) {
            isInSettings = false 
            pendingRestoreUrl?.let { url ->
                pendingRestoreUrl = null
                show(context, url)
            }
        }
        
        fun enterSettings() {
            isInSettings = true
        }
        
        fun exitSettings(context: Context) {
            isInSettings = false
            pendingRestoreUrl?.let { url ->
                pendingRestoreUrl = null
                show(context, url)
            }
        }
        
        fun resetSettingsState() {
            isInSettings = false
            pendingRestoreUrl = null
        }
        
        fun hide(context: Context) {
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }
        
        fun updateUrl(context: Context, url: String) {
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_UPDATE"
                putExtra("url", url)
            }
            context.startService(intent)
        }
        
        
        fun executeCommand(context: Context, command: String) {
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_COMMAND"
                putExtra("command", command)
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        browserSettingsStore = BrowserSettingsStore(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            "ACTION_SHOW" -> {
                val url = intent.getStringExtra("url") ?: ""
                showWebView(url)
            }
            "ACTION_HIDE" -> {
                hideWebView()
            }
            "ACTION_UPDATE" -> {
                val url = intent.getStringExtra("url") ?: ""
                updateUrl(url)
            }
            "ACTION_COMMAND" -> {
                val command = intent.getStringExtra("command") ?: ""
                handleCommand(command)
            }
        }
        
        return START_STICKY
    }
    
    private fun showWebView(url: String) {
        originalUrl = url 
        if (containerView != null) {
            updateUrl(url)
            return
        }
        
        lifecycleScope.launch {
            try {
                
                currentSettings = browserSettingsStore.get()
                
                
                webView = WebView(this@WebViewService)
                setupWebView()
                
                
                val rootView: View = if (currentSettings.pullRefreshEnabled) {
                    swipeRefreshLayout = SwipeRefreshLayout(this@WebViewService).apply {
                        addView(webView, ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        ))
                        setOnRefreshListener {
                            webView?.reload()
                            postDelayed({ isRefreshing = false }, 1500)
                        }
                        
                        setColorSchemeColors(0xFF4F46E5.toInt())
                    }
                    swipeRefreshLayout!!
                } else {
                    webView!!
                }
                
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
                            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    format = PixelFormat.TRANSLUCENT
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    gravity = Gravity.TOP or Gravity.START
                    x = 0
                    y = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                
                
                val container = object : FrameLayout(this@WebViewService) {
                    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                            
                            if (currentSettings.backKeyHideEnabled) {
                                if (webView?.canGoBack() == true) {
                                    webView?.goBack()
                                } else {
                                    hideWebView()
                                }
                                return true
                            }
                        }
                        return super.dispatchKeyEvent(event)
                    }
                }.apply {
                    isFocusableInTouchMode = true
                    requestFocus()
                    addView(rootView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                    
                    
                    if (currentSettings.settingsButtonEnabled) {
                        val density = resources.displayMetrics.density
                        val buttonSize = (48 * density).toInt()
                        val margin = (16 * density).toInt()
                        val bottomMargin = (16 * density).toInt() 
                        
                        val settingsButton = ImageButton(this@WebViewService).apply {
                            setImageResource(com.example.ava.R.drawable.settings_24px)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setColorFilter(android.graphics.Color.GRAY) 
                            alpha = 0.6f 
                            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                            isClickable = true
                            isFocusable = true
                            elevation = 10f 
                            setOnClickListener {
                                
                                if (originalUrl.isNotEmpty()) {
                                    pendingRestoreUrl = originalUrl
                                }
                                enterSettings()
                                hideWebView()
                                val intent = android.content.Intent(this@WebViewService, com.example.ava.MainActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra("navigate_to", "settings")
                                }
                                startActivity(intent)
                            }
                        }
                        val buttonParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                            gravity = Gravity.BOTTOM or Gravity.END
                            setMargins(0, 0, margin, bottomMargin)
                        }
                        addView(settingsButton, buttonParams)
                    }
                }
                containerView = container
                windowManager?.addView(containerView, params)
                loadUrl(url)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show WebView", e)
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun setupWebView() {
        webView?.let { wv ->
            val settings = wv.settings
            
            
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            settings.userAgentString = settings.userAgentString + " AvaWebView"
            
            
            
            if (currentSettings.initialScale > 0) {
                wv.setInitialScale(currentSettings.initialScale)
            }
            
            
            settings.textZoom = currentSettings.fontSize
            
            
            if (currentSettings.hardwareAcceleration) {
                wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            
            
            if (!currentSettings.touchEnabled) {
                wv.setOnTouchListener { _, _ -> true }
            }
            
            
            settings.setSupportZoom(currentSettings.dragEnabled)
            settings.builtInZoomControls = currentSettings.dragEnabled
            settings.displayZoomControls = false 
            
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $errorCode - $description")
                }
                
                override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError?) {
                    handler.proceed()
                }
                
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    Log.e(TAG, "Renderer crashed, didCrash=${detail?.didCrash()}")
                    webView?.let { wv ->
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                    webView = null
                    return true
                }
            }
            
            wv.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                }
            }
            
            
            wv.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    if (wv.canGoBack()) {
                        wv.goBack()
                    } else {
                        hideWebView()
                    }
                    true
                } else {
                    false
                }
            }
            wv.isFocusableInTouchMode = true
            wv.requestFocus()
        }
    }
    
    private fun loadUrl(url: String) {
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        webView?.loadUrl(finalUrl)
    }
    
    private fun updateUrl(url: String) {
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
            Log.e(TAG, "Failed to hide WebView", e)
        }
        containerView = null
        swipeRefreshLayout = null
        webView = null
    }
    
    
    private fun handleCommand(command: String) {
        
        if (command.isEmpty()) {
            val clearJs = "document.querySelectorAll('[data-ava-injected]').forEach(e=>e.remove());"
            webView?.evaluateJavascript(clearJs, null)
            Log.d(TAG, "Cleared injected effects")
            return
        }
        
        try {
            val json = org.json.JSONObject(command)
            
            
            if (json.has("eval")) {
                val js = json.getString("eval")
                if (js.isEmpty()) {
                    
                    val clearJs = """
                        (function(){
                            var id = window.setTimeout(function(){}, 0);
                            while (id--) { window.clearTimeout(id); window.clearInterval(id); }
                            document.querySelectorAll('*').forEach(function(e){
                                var s = window.getComputedStyle(e);
                                if(s.position==='fixed' && parseInt(s.zIndex)>=9999) e.remove();
                            });
                            document.querySelectorAll('style').forEach(function(s){
                                if(s.innerHTML.indexOf('@keyframes')!==-1) s.remove();
                            });
                        })();
                    """.trimIndent()
                    webView?.evaluateJavascript(clearJs, null)
                    Log.d(TAG, "Cleared injected effects via empty eval")
                } else if (webView != null) {
                    webView?.evaluateJavascript(js) { result ->
                        Log.d(TAG, "JS result: $result")
                    }
                    Log.d(TAG, "Executed JS: ${js.take(100)}...")
                } else {
                    Log.e(TAG, "WebView is null, cannot execute JS")
                }
            }
            
            
            if (json.has("clearCache") && json.getBoolean("clearCache")) {
                webView?.clearCache(true)
                webView?.clearHistory()
                Log.d(TAG, "Cache cleared")
            }
            
            
            if (json.has("reload") && json.getBoolean("reload")) {
                webView?.reload()
                Log.d(TAG, "Page reloaded")
            }
            
            
            if (json.has("settings") && json.getBoolean("settings")) {
                hideWebView()
                val intent = android.content.Intent(this, com.example.ava.MainActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("navigate_to", "settings")
                }
                startActivity(intent)
                Log.d(TAG, "Opening settings")
            }
            
            
            if (json.has("brightness")) {
                val brightness = json.getInt("brightness").coerceIn(0, 255)
                try {
                    android.provider.Settings.System.putInt(
                        contentResolver,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        brightness
                    )
                    Log.d(TAG, "Brightness set to: $brightness")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set brightness", e)
                }
            }
            
            
            if (json.has("volume")) {
                val volume = json.getInt("volume").coerceIn(0, 100)
                try {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    val targetVolume = (volume * maxVolume / 100)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetVolume, 0)
                    Log.d(TAG, "Volume set to: $volume%")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set volume", e)
                }
            }
            
            
            if (json.has("camera") && json.getBoolean("camera")) {
                try {
                    val cameraIntent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(cameraIntent)
                    Log.d(TAG, "Opening camera")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open camera", e)
                }
            }
            
            
            if (json.has("injectCSS")) {
                val css = json.getString("injectCSS")
                val escapedCss = css.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                val js = """
                    (function() {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = '$escapedCss';
                        document.head.appendChild(style);
                    })();
                """.trimIndent()
                webView?.evaluateJavascript(js, null)
                Log.d(TAG, "Injected CSS")
            }
            
            
            if (json.has("clickElement")) {
                val selector = json.getString("clickElement")
                val escapedSelector = selector.replace("\\", "\\\\").replace("'", "\\'")
                val js = """
                    (function() {
                        var el = document.querySelector('$escapedSelector');
                        if (el) { el.click(); return 'clicked'; }
                        return 'not found';
                    })();
                """.trimIndent()
                webView?.evaluateJavascript(js) { result ->
                    Log.d(TAG, "clickElement result: $result")
                }
            }
            
            
            if (json.has("fillInput")) {
                val fillObj = json.getJSONObject("fillInput")
                val selector = fillObj.getString("selector")
                val value = fillObj.getString("value")
                val escapedSelector = selector.replace("\\", "\\\\").replace("'", "\\'")
                val escapedValue = value.replace("\\", "\\\\").replace("'", "\\'")
                val js = """
                    (function() {
                        var el = document.querySelector('$escapedSelector');
                        if (el) {
                            el.value = '$escapedValue';
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            return 'filled';
                        }
                        return 'not found';
                    })();
                """.trimIndent()
                webView?.evaluateJavascript(js) { result ->
                    Log.d(TAG, "fillInput result: $result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse command: $command", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideWebView()
        instance = null
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
