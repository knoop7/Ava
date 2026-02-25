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
import com.example.ava.utils.UserScriptManager
import com.example.ava.webmonkey.GmApi
import com.example.ava.webmonkey.GmApiInjector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WebViewService : LifecycleService() {
    
    private var windowManager: WindowManager? = null
    private var containerView: ViewGroup? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var webView: WebView? = null
    private var gmApi: GmApi? = null
    private lateinit var browserSettingsStore: BrowserSettingsStore
    private lateinit var userScriptManager: UserScriptManager
    private var currentSettings: BrowserSettings = BrowserSettings.DEFAULT
    private var originalUrl: String = ""
    private var tampermonkeyDialog: android.app.AlertDialog? = null
    private var scriptListDialog: android.app.AlertDialog? = null 
    
    companion object {
        private const val TAG = "WebViewService"
        private var instance: WebViewService? = null
        private var pendingRestoreUrl: String? = null 
        private var isInSettings = false
        private var isCreating = false 
        
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
        
        fun destroy(context: Context) {
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_DESTROY"
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
        
        fun showOrRefresh(context: Context, url: String) {
            val intent = Intent(context, WebViewService::class.java).apply {
                action = "ACTION_SHOW_OR_REFRESH"
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
        browserSettingsStore = BrowserSettingsStore(this)
        userScriptManager = UserScriptManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
            "ACTION_DESTROY" -> {
                destroyWebView()
            }
            "ACTION_UPDATE" -> {
                val url = intent.getStringExtra("url") ?: ""
                updateUrl(url)
            }
            "ACTION_COMMAND" -> {
                val command = intent.getStringExtra("command") ?: ""
                handleCommand(command)
            }
            "ACTION_SHOW_OR_REFRESH" -> {
                val url = intent.getStringExtra("url") ?: ""
                showOrRefreshWebView(url)
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
        
        if (isCreating) {
            Log.d(TAG, "Already creating WebView, ignoring show request")
            return
        }
        isCreating = true
        
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
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_FULLSCREEN or
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
                    
                    if (currentSettings.tampermonkeyEnabled) {
                        val density = resources.displayMetrics.density
                        val iconSize = (48 * density).toInt()
                        val topMargin = (20 * density).toInt()
                        val rightMargin = (12 * density).toInt()
                        
                        val tampermonkeyIcon = ImageButton(this@WebViewService).apply {
                            setImageResource(com.example.ava.R.drawable.ic_tampermonkey)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            alpha = 0.5f
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                            isClickable = true
                            isFocusable = true
                            elevation = 10f
                            setOnClickListener {
                                showTampermonkeyDialog()
                            }
                        }
                        val iconParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                            gravity = Gravity.TOP or Gravity.END
                            setMargins(0, topMargin, rightMargin, 0)
                        }
                        addView(tampermonkeyIcon, iconParams)
                    }
                }
                containerView = container
                windowParams = params
                windowManager?.addView(containerView, params)
                loadUrl(url)
                
                WeatherOverlayService.bringToFrontStatic()
                DreamClockService.bringToFrontStatic()
                QuickEntityOverlayService.bringToFrontStatic()
                
                ScreensaverController.onUserInteraction()
                
                isCreating = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show WebView", e)
                isCreating = false
            }
        }
    }
    
    private var renderCrashCount = 0
    private var lastRenderCrashTime = 0L
    private val MAX_CRASH_COUNT = 3
    private val CRASH_RESET_INTERVAL = 60_000L
    
    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun setupWebView() {
        webView?.let { wv ->
            val settings = wv.settings
            
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.mediaPlaybackRequiresUserGesture = false
            
            val baseUA = settings.userAgentString
            settings.userAgentString = when (currentSettings.userAgentMode) {
                1 -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                2 -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
                3 -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
                else -> "$baseUA AvaWebView"
            }
            
            if (currentSettings.tampermonkeyEnabled) {
                gmApi = GmApi(this@WebViewService, wv, "ava_scripts")
                wv.addJavascriptInterface(gmApi!!, GmApi.JS_BRIDGE_NAME)
            }
            
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
            
            
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true 
            
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (currentSettings.tampermonkeyEnabled && url.endsWith(".user.js")) {
                        downloadAndInstallScript(url)
                        return true
                    }
                    return false
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    hideLoadingDialog()
                    if (currentSettings.tampermonkeyEnabled && url != null) {
                        injectMatchingScripts(url)
                    }
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
                    val now = System.currentTimeMillis()
                    val didCrash = detail?.didCrash() ?: false
                    Log.e(TAG, "Renderer crashed, didCrash=$didCrash, crashCount=$renderCrashCount")
                    

                    if (now - lastRenderCrashTime > CRASH_RESET_INTERVAL) {
                        renderCrashCount = 0
                    }
                    lastRenderCrashTime = now
                    renderCrashCount++
                    
                    val lastUrl = webView?.url ?: originalUrl
                    webView?.let { wv ->
                        try {
                            (wv.parent as? ViewGroup)?.removeView(wv)
                            wv.destroy()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error destroying crashed WebView", e)
                        }
                    }
                    webView = null
                    
                    if (renderCrashCount >= MAX_CRASH_COUNT) {
                        Log.e(TAG, "Too many crashes ($renderCrashCount), stopping WebView rebuild to protect system")
                        lifecycleScope.launch {
                            browserSettingsStore.enableBrowserVisible.set(false)
                        }
                        return true
                    }
                    
                    if (lastUrl.isNotEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            showWebView(lastUrl)
                        }, 2000)
                    }
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
            
            wv.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    windowParams?.let { params ->
                        if (params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0) {
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            windowManager?.updateViewLayout(containerView, params)
                        }
                    }
                    v.requestFocus()
                }
                false
            }
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
    
    private fun showOrRefreshWebView(url: String) {
        originalUrl = url
        if (containerView == null) {
            showWebView(url)
        } else {
            webView?.reload()
            if (webView?.url != url) {
                loadUrl(url)
            }
        }
    }
    
    private fun cleanupWebView(caller: String) {
        Log.d(TAG, "$caller called, containerView=$containerView, webView=$webView")
        
        try {
            webView?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.onPause()
                    wv.pauseTimers()
                    wv.loadUrl("about:blank")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping WebView", e)
                }
                try {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing WebView from parent", e)
                }
                try {
                    wv.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error destroying WebView", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in WebView cleanup", e)
        }
        
        try {
            containerView?.let { container ->
                try {
                    windowManager?.removeView(container)
                    Log.d(TAG, "Container removed from WindowManager")
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Container not attached to WindowManager")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing container", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in container cleanup", e)
        }
        
        lifecycleScope.launch {
            try {
                val settings = browserSettingsStore.get()
                if (settings.enableBrowserDisplay && settings.enableBrowserVisible) {
                    browserSettingsStore.enableBrowserVisible.set(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating settings", e)
            }
        }
        
        containerView = null
        swipeRefreshLayout = null
        webView = null
        isCreating = false
    }
    
    private fun hideWebView() {
        cleanupWebView("hideWebView")
    }
    
    private fun destroyWebView() {
        cleanupWebView("destroyWebView")
        originalUrl = ""
        pendingRestoreUrl = null
        stopSelf()
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
    
    private fun showTampermonkeyDialog() {
        tampermonkeyDialog?.dismiss()
        
        val density = resources.displayMetrics.density
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            setBackgroundColor(android.graphics.Color.parseColor("#222222"))
        }
        
        val scripts = userScriptManager.getAllScripts()
        val titleText = android.widget.TextView(this).apply {
            text = if (scripts.isNotEmpty()) "Tampermonkey (${scripts.size})" else "Tampermonkey"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
        }
        dialogView.addView(titleText)
        
        val divider = View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#333333"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()
            ).apply { setMargins(0, (10 * density).toInt(), 0, (10 * density).toInt()) }
        }
        dialogView.addView(divider)
        
        val addScriptBtn = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_add_script)
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            setOnClickListener {
                tampermonkeyDialog?.dismiss()
                showAddScriptDialog()
            }
        }
        dialogView.addView(addScriptBtn)
        
        val installBtn = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_install_script)
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            setOnClickListener {
                tampermonkeyDialog?.dismiss()
                showLoadingDialog()
                webView?.loadUrl("https://greasyfork.org/")
            }
        }
        dialogView.addView(installBtn)
        
        val manageBtn = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_manage_scripts)
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            setOnClickListener {
                tampermonkeyDialog?.dismiss()
                showScriptListDialog()
            }
        }
        dialogView.addView(manageBtn)
        
        tampermonkeyDialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView)
            .create()
        
        tampermonkeyDialog?.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        tampermonkeyDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        tampermonkeyDialog?.show()
    }
    
    private var loadingDialog: android.app.AlertDialog? = null
    
    private fun showLoadingDialog() {
        loadingDialog?.dismiss()
        val density = resources.displayMetrics.density
        
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (16 * density).toInt())
            setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val progressBar = android.widget.ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = android.widget.LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt())
        }
        dialogView.addView(progressBar)
        
        val loadingText = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_installing)
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setPadding((12 * density).toInt(), 0, 0, 0)
        }
        dialogView.addView(loadingText)
        
        loadingDialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        loadingDialog?.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }
    
    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    
    private fun downloadAndInstallScript(url: String) {
        showLoadingDialog()
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var connection: java.net.HttpURLConnection? = null
            try {
                connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val scriptContent = connection.inputStream.bufferedReader().use { it.readText() }
                
                val script = UserScriptManager.parseUserScript(scriptContent)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    hideLoadingDialog()
                    if (script != null) {
                        userScriptManager.saveScript(script)
                        showScriptInstalledDialog(script.name)
                    } else {
                        showInstallFailedDialog()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download script: $url", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    hideLoadingDialog()
                    showInstallFailedDialog()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }
    
    private fun showInstallFailedDialog() {
        val density = resources.displayMetrics.density
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt(), (16 * density).toInt())
            setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            gravity = Gravity.CENTER
        }
        
        val msgText = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_install_failed)
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
        }
        dialogView.addView(msgText)
        
        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .create()
        
        dialog.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun showScriptInstalledDialog(scriptName: String) {
        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(com.example.ava.R.string.tampermonkey_script_installed))
            .setMessage(scriptName)
            .setPositiveButton("OK", null)
            .create()
        
        dialog.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        dialog.show()
    }
    
    private fun injectMatchingScripts(url: String) {
        webView?.evaluateJavascript(GmApiInjector.getGmApiScript()) { 
            Log.d(TAG, "GM API injected")
        }
        
        val scripts = userScriptManager.getMatchingScripts(url)
        scripts.forEach { script ->
            val metadataEndMarker = "==/UserScript=="
            val metadataEnd = script.code.indexOf(metadataEndMarker)
            if (metadataEnd < 0) return@forEach
            
            var codeStart = metadataEnd + metadataEndMarker.length
            while (codeStart < script.code.length && script.code[codeStart] in listOf('\n', '\r', ' ', '\t')) {
                codeStart++
            }
            val pureCode = script.code.substring(codeStart)
            
            if (pureCode.isBlank()) return@forEach
            
            val wrappedCode = """
                (function() {
                    'use strict';
                    try {
                        $pureCode
                    } catch(e) {
                        console.error('[Tampermonkey] Script error:', e);
                    }
                })();
            """.trimIndent()
            
            webView?.evaluateJavascript(wrappedCode) { result ->
                Log.d(TAG, "Injected script: ${script.name}")
            }
        }
    }
    
    private fun showAddScriptDialog() {
        val density = resources.displayMetrics.density
        
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            setBackgroundColor(android.graphics.Color.parseColor("#222222"))
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_add_script)
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        dialogView.addView(titleText)
        
        val descText = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_add_script_desc)
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#888888"))
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        dialogView.addView(descText)
        
        val editText = android.widget.EditText(this).apply {
            hint = "// ==UserScript==\n// @name  My Script\n// @match *://*/*\n// ==/UserScript==\n\nalert('Hello!');"
            setHintTextColor(android.graphics.Color.parseColor("#555555"))
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
            textSize = 11f
            minLines = 8
            maxLines = 12
            gravity = Gravity.TOP or Gravity.START
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        dialogView.addView(editText)
        
        val buttonRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (12 * density).toInt(), 0, 0)
        }
        
        val cancelBtn = android.widget.TextView(this).apply {
            text = getString(android.R.string.cancel)
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#888888"))
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }
        buttonRow.addView(cancelBtn)
        
        val saveBtn = android.widget.TextView(this).apply {
            text = getString(android.R.string.ok)
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }
        buttonRow.addView(saveBtn)
        dialogView.addView(buttonRow)
        
        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView)
            .create()
        
        cancelBtn.setOnClickListener { dialog.dismiss() }
        saveBtn.setOnClickListener {
            val code = editText.text.toString()
            if (code.isNotBlank()) {
                val script = UserScriptManager.parseUserScript(code)
                if (script != null) {
                    userScriptManager.saveScript(script)
                    dialog.dismiss()
                    showScriptInstalledDialog(script.name)
                } else {
                    val simpleScript = com.example.ava.utils.UserScript(
                        id = System.currentTimeMillis().toString(),
                        name = "Custom Script",
                        namespace = "local",
                        version = "1.0",
                        description = "",
                        matchPatterns = listOf("*"),
                        code = "// ==UserScript==\n// @name Custom Script\n// @match *://*/*\n// ==/UserScript==\n\n$code",
                        enabled = true
                    )
                    userScriptManager.saveScript(simpleScript)
                    dialog.dismiss()
                    showScriptInstalledDialog("Custom Script")
                }
            }
        }
        
        dialog.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun showScriptListDialog() {
        scriptListDialog?.dismiss()
        
        val density = resources.displayMetrics.density
        val scripts = userScriptManager.getAllScripts()
        
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            setBackgroundColor(android.graphics.Color.parseColor("#222222"))
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = getString(com.example.ava.R.string.tampermonkey_manage_scripts)
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        dialogView.addView(titleText)
        
        val divider = View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#333333"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()
            ).apply { setMargins(0, 0, 0, (8 * density).toInt()) }
        }
        dialogView.addView(divider)
        
        if (scripts.isEmpty()) {
            val emptyText = android.widget.TextView(this).apply {
                text = getString(com.example.ava.R.string.tampermonkey_no_scripts)
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(0, (12 * density).toInt(), 0, (12 * density).toInt())
            }
            dialogView.addView(emptyText)
        } else {
            val scrollView = android.widget.ScrollView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    height = minOf((200 * density).toInt(), (scripts.size * 48 * density).toInt())
                }
            }
            val listContainer = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            
            scripts.forEach { script ->
                val itemView = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
                    gravity = Gravity.CENTER_VERTICAL
                }
                
                val statusDot = android.widget.TextView(this).apply {
                    text = if (script.enabled) "●" else "○"
                    textSize = 10f
                    setTextColor(if (script.enabled) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#555555"))
                    setPadding(0, 0, (6 * density).toInt(), 0)
                    setOnClickListener {
                        userScriptManager.toggleScript(script.id, !script.enabled)
                        showScriptListDialog()
                    }
                }
                itemView.addView(statusDot)
                
                val nameText = android.widget.TextView(this).apply {
                    text = script.name
                    textSize = 12f
                    setTextColor(if (script.enabled) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#666666"))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, (16 * density).toInt(), 0)
                    }
                    setOnClickListener {
                        userScriptManager.toggleScript(script.id, !script.enabled)
                        showScriptListDialog()
                    }
                }
                itemView.addView(nameText)
                
                val deleteBtn = android.widget.TextView(this).apply {
                    text = "✕"
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#555555"))
                    setPadding((10 * density).toInt(), (4 * density).toInt(), 0, (4 * density).toInt())
                    setOnClickListener {
                        userScriptManager.deleteScript(script.id)
                        showScriptListDialog()
                    }
                }
                itemView.addView(deleteBtn)
                
                listContainer.addView(itemView)
            }
            scrollView.addView(listContainer)
            dialogView.addView(scrollView)
        }
        
        val closeBtn = android.widget.TextView(this).apply {
            text = "OK"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, (10 * density).toInt(), 0, (4 * density).toInt())
        }
        dialogView.addView(closeBtn)
        
        scriptListDialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setView(dialogView)
            .create()
        
        closeBtn.setOnClickListener { scriptListDialog?.dismiss() }
        
        scriptListDialog?.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        )
        scriptListDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        scriptListDialog?.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        

        try {
            tampermonkeyDialog?.dismiss()
            tampermonkeyDialog = null
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing tampermonkeyDialog", e)
        }
        
        try {
            scriptListDialog?.dismiss()
            scriptListDialog = null
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing scriptListDialog", e)
        }
        
        try {
            loadingDialog?.dismiss()
            loadingDialog = null
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing loadingDialog", e)
        }
        

        try {
            hideWebView()
        } catch (e: Exception) {
            Log.e(TAG, "Error in hideWebView during onDestroy", e)
        }
        

        try {
            gmApi?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying gmApi", e)
        }
        gmApi = null
        instance = null
    }
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
