package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.util.Calendar
import com.example.ava.R
import com.example.ava.settings.playerSettingsStore
import com.example.ava.weather.WeatherData
import com.example.ava.weather.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class DreamClockService : Service() {

    private var windowManager: WindowManager? = null
    private var clockView: DreamClockView? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isClockEnabled = false
    private var isClockVisible = false
    @Volatile private var isServiceRunning = true

    override fun onBind(intent: Intent?): IBinder? = null

    private val weatherListener: (WeatherData) -> Unit = { weather ->
        handler.post {
            clockView?.updateWeather(weather.temperature, weather.condition)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        WeatherService.addWeatherListener(weatherListener)
    }
    
    private fun bringToFront() {
        if (!isClockEnabled || !isClockVisible) return
        clockView?.let { view ->
            windowParams?.let { params ->
                try {

                    if (view.isAttachedToWindow) {
                        windowManager?.removeView(view)
                        windowManager?.addView(view, params)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bring clock to front", e)
                }
            }
        }
    }
    
    private fun fetchWeather() {
        serviceScope.launch {
            try {
                val settings = playerSettingsStore.data.first()
                val weatherEntity = settings.haWeatherEntity
                if (weatherEntity.isBlank()) {
                    return@launch
                }
                val weather = WeatherService.getCachedWeather()
                if (weather != null) {
                    clockView?.updateWeather(weather.temperature, weather.condition)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather: ${e.message}")
            }
        }
        
        if (isServiceRunning) {
            handler.postDelayed({ fetchWeather() }, 60 * 1000L)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createClockView() {
        val displayMetrics = resources.displayMetrics
        val clockSize = minOf(displayMetrics.widthPixels, displayMetrics.heightPixels) + 25

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        
        clockView = DreamClockView(this, clockSize).apply {
            setOnTouchListener(DoubleTapPassThroughListener(
                onDoubleTap = { toggleClock() },
                onSwipeLeft = { switchToWeather() },
                windowManager = windowManager
            ))
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager?.addView(clockView, windowParams)
            clockView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create clock window", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> {
                Log.d(TAG, "ACTION_SHOW received, canDrawOverlays=${android.provider.Settings.canDrawOverlays(this)}")
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Log.w(TAG, "Cannot draw overlays, returning")
                    return
                }
                if (clockView == null) {
                    Log.d(TAG, "Creating clock view")
                    createClockView()
                }
                isClockEnabled = true
                isClockVisible = true
                bringToFront()
                showClock()
                Log.d(TAG, "Clock shown, clockView=${clockView != null}, visibility=${clockView?.visibility}")
            }
            ACTION_HIDE -> {
                isClockEnabled = false
                hideClock()
            }
            ACTION_TOGGLE -> {
                if (isClockEnabled) {
                    toggleClock()
                }
            }
            ACTION_SET_VISIBLE -> {
                if (isClockEnabled) {
                    val visible = intent.getBooleanExtra(EXTRA_VISIBLE, true)
                    if (visible && !isClockVisible) {
                        showClock()
                    } else if (!visible && isClockVisible) {
                        hideClock()
                    }
                }
            }
            "com.example.ava.ACTION_BRING_TO_FRONT" -> {
                bringToFront()
            }
        }
    }
    
    private fun showClock() {
        clockView?.visibility = View.VISIBLE
        clockView?.setShowClock(true)
        clockView?.startClock()
        isClockVisible = true
        handler.postDelayed({ fetchWeather() }, 1000)
    }
    
    private fun hideClock() {
        clockView?.stopClock()
        clockView?.visibility = View.GONE
        isClockVisible = false
    }
    
    
    private fun toggleClock() {
        toggleClockDisplay()
    }
    
    
    private fun switchToWeather() {
        
        serviceScope.launch {
            try {
                val settings = playerSettingsStore.data.first()
                
                if (settings.enableWeatherOverlay) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        hide(this@DreamClockService)
                        WeatherOverlayService.show(this@DreamClockService)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check weather settings: ${e.message}")
            }
        }
    }
    
    private fun toggleClockDisplay() {
        if (!isClockEnabled) return
        
        if (isClockVisible) {
            clockView?.stopClock()
            clockView?.visibility = View.GONE
            isClockVisible = false
            serviceScope.launch {
                playerSettingsStore.updateData { it.copy(enableDreamClockVisible = false) }
            }
        } else {
            clockView?.visibility = View.VISIBLE
            clockView?.setShowClock(true)
            clockView?.startClock()
            isClockVisible = true
            serviceScope.launch {
                playerSettingsStore.updateData { it.copy(enableDreamClockVisible = true) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        WeatherService.removeWeatherListener(weatherListener)
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        clockView?.stopClock()
        try {
            clockView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove clock window", e)
        }
    }

    companion object {
        private const val TAG = "DreamClockService"
        private var instance: DreamClockService? = null
        
        const val ACTION_SHOW = "com.example.ava.SHOW_CLOCK"
        const val ACTION_HIDE = "com.example.ava.HIDE_CLOCK"
        const val ACTION_TOGGLE = "com.example.ava.TOGGLE_CLOCK"

        fun bringToFrontStatic() {
            instance?.bringToFront()
        }

        fun show(context: Context) {
            val intent = Intent(context, DreamClockService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, DreamClockService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
        
        fun toggle(context: Context) {
            val intent = Intent(context, DreamClockService::class.java).apply {
                action = ACTION_TOGGLE
            }
            context.startService(intent)
        }
        
        const val ACTION_SET_VISIBLE = "com.example.ava.SET_CLOCK_VISIBLE"
        const val EXTRA_VISIBLE = "visible"
        
        fun setVisible(context: Context, visible: Boolean) {
            val intent = Intent(context, DreamClockService::class.java).apply {
                action = ACTION_SET_VISIBLE
                putExtra(EXTRA_VISIBLE, visible)
            }
            context.startService(intent)
        }
    }
}


class DoubleTapPassThroughListener(
    private val onDoubleTap: () -> Unit,
    private val onSwipeLeft: (() -> Unit)? = null,
    private val windowManager: WindowManager?
) : View.OnTouchListener {
    private var lastTapTime: Long = 0
    private var tapCount = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: android.view.MotionEvent?): Boolean {
        when (event?.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 300) {
                    tapCount++
                    if (tapCount >= 2) {
                        onDoubleTap()
                        tapCount = 0
                        return true
                    }
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime
            }
            android.view.MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                
                
                if (dx < -100 && kotlin.math.abs(dy) < 100) {
                    onSwipeLeft?.invoke()
                    return true
                }
            }
        }
        return false
    }
}


class DreamClockView(context: Context, private val clockSize: Int) : View(context) {
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var showClockFace = true
    
    
    private var weatherTemp = 21
    private var weatherCondition = "sunny"
    
    init {
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }
    
    fun updateWeather(temp: Int, condition: String) {
        weatherTemp = temp
        weatherCondition = condition
        invalidate()
    }
    
    
    private val bgDeep = Color.parseColor("#020205")
    private val bgNebula = Color.parseColor("#151530")
    private val accentGlow = Color.parseColor("#a29bfe")  
    private val accentGold = Color.parseColor("#ffeaa7")  
    private val metalLight = Color.WHITE
    private val metalDark = Color.parseColor("#888899")
    private val lumeColor = Color.parseColor("#ccffcc")   
    
    fun setShowClock(show: Boolean) {
        showClockFace = show
        invalidate()
    }
    
    
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numeralPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brandMainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lumePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dateWindowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    init {
        
        tickPaint.apply {
            color = Color.argb(64, 255, 255, 255)
            strokeWidth = 1f
            strokeCap = Paint.Cap.ROUND
        }
        majorTickPaint.apply {
            color = metalLight
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(8f, 0f, 0f, Color.argb(77, 255, 255, 255))
        }
        
        numeralPaint.apply {
            color = metalLight
            textSize = clockSize * 0.08f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            setShadowLayer(15f, 0f, 2f, Color.argb(102, 255, 255, 255))
        }
        
        brandMainPaint.apply {
            color = metalLight
            textSize = clockSize * 0.035f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.15f
            setShadowLayer(4f, 0f, 2f, Color.argb(204, 0, 0, 0))
        }
        
        brandSubPaint.apply {
            color = accentGlow
            textSize = clockSize * 0.02f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        
        datePaint.apply {
            color = metalLight
            textSize = clockSize * 0.04f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        
        tempPaint.apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = clockSize * 0.04f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        
        hourHandPaint.apply {
            color = metalLight
            style = Paint.Style.FILL
        }
        
        minHandPaint.apply {
            color = metalLight
            style = Paint.Style.FILL
        }
        
        secHandPaint.apply {
            color = accentGlow
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(4f, 0f, 0f, accentGlow)
        }
        
        lumePaint.apply {
            color = lumeColor
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 0f, Color.argb(102, 204, 255, 204))
        }
        
        centerPaint.apply {
            style = Paint.Style.FILL
        }
        
        dateWindowPaint.apply {
            color = Color.parseColor("#151515")
            style = Paint.Style.FILL
        }
        
        moonPaint.apply {
            style = Paint.Style.FILL
        }
    }
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            if (isRunning) {
                handler.postDelayed(this, 67)  
            }
        }
    }
    
    fun startClock() {
        isRunning = true
        handler.post(updateRunnable)
    }
    
    fun stopClock() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.drawColor(Color.BLACK)
        if (!showClockFace) return
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = clockSize / 2f * 0.95f
        
        
        drawDialBackground(canvas, centerX, centerY, radius)
        
        
        drawMinuteTrack(canvas, centerX, centerY, radius)
        
        
        drawTicks(canvas, centerX, centerY, radius)
        
        
        drawNumerals(canvas, centerX, centerY, radius)
        
        
        drawBrand(canvas, centerX, centerY, radius)
        
        
        drawWeatherWithIcon(canvas, centerX, centerY, radius)
        
        
        drawDateWindow(canvas, centerX, centerY, radius)
        
        
        drawHands(canvas, centerX, centerY, radius)
        
        
        drawPinion(canvas, centerX, centerY, radius)
        
        
        drawCrystal(canvas, centerX, centerY, radius)
    }
    
    private fun drawDialBackground(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        
        val bgGradient = android.graphics.RadialGradient(
            cx, cy - radius * 0.15f, radius,
            intArrayOf(bgNebula, bgDeep, Color.BLACK),
            floatArrayOf(0f, 0.7f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgGradient
        canvas.drawCircle(cx, cy, radius, bgPaint)
        bgPaint.shader = null
        
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..18
        
        
        val glowColor = if (isDay) 
            intArrayOf(Color.argb(30, 255, 80, 80), Color.argb(15, 255, 100, 100), Color.TRANSPARENT)
        else 
            intArrayOf(Color.argb(25, 162, 155, 254), Color.argb(12, 180, 170, 255), Color.TRANSPARENT)
        val glowRadius = radius * 0.85f  
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                cx, cy, glowRadius,
                glowColor,
                floatArrayOf(0f, 0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            maskFilter = android.graphics.BlurMaskFilter(120f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)
    }
    
    private fun drawMinuteTrack(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(10, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawCircle(cx, cy, radius * 0.94f, trackPaint)
    }
    
    private fun drawTicks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        for (i in 0 until 60) {
            
            if (i % 15 == 0) continue  
            if (i % 15 == 1 || i % 15 == 14) continue
            if (i % 15 == 2 || i % 15 == 13) continue
            if (i % 15 == 3 || i % 15 == 12) continue  
            
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val isMajor = i % 5 == 0
            val outerR = radius * 0.94f
            val innerR = if (isMajor) radius * 0.91f else radius * 0.925f
            val paint = if (isMajor) majorTickPaint else tickPaint
            
            val startX = cx + (outerR * Math.cos(angle)).toFloat()
            val startY = cy + (outerR * Math.sin(angle)).toFloat()
            val endX = cx + (innerR * Math.cos(angle)).toFloat()
            val endY = cy + (innerR * Math.sin(angle)).toFloat()
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
    
    private fun drawNumerals(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val offset = numeralPaint.textSize * 0.35f
        
        canvas.drawText("12", cx, cy - radius * 0.90f + offset, numeralPaint)
        
        canvas.drawText("6", cx, cy + radius * 0.88f + offset, numeralPaint)
        
        canvas.drawText("3", cx + radius * 0.88f, cy + offset - numeralPaint.textSize * 0.08f, numeralPaint)
        
        canvas.drawText("9", cx - radius * 0.88f, cy + offset - numeralPaint.textSize * 0.1f, numeralPaint)
    }
    
    private fun drawBrand(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..18
        
        canvas.drawText("BONJOUR", cx, cy - radius * 0.52f, brandMainPaint)
        
        
        val subColor = if (isDay) Color.parseColor("#ff4444") else accentGlow
        brandSubPaint.color = subColor
        canvas.drawText("Chronometer", cx, cy - radius * 0.52f + brandMainPaint.textSize + 4, brandSubPaint)
        
        
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = radius * 0.035f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            letterSpacing = 0.15f
        }
        canvas.drawText("CENTER", cx, cy + radius * 0.15f, centerPaint)
    }
    
    
    private fun drawWeatherWithIcon(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        try {
            val weatherY = cy + radius * 0.45f
            
            
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..18
            
            
            val isSmallScreen = clockSize <= 350
            
            
            val iconSize = if (isSmallScreen) (radius * 0.12f).toInt() else (radius * 0.10f).toInt()
            
            
            val iconResId = when (weatherCondition) {
                "sunny" -> if (isDay) R.drawable.mdi_weather_sunny else R.drawable.mdi_weather_night
                "cloudy" -> if (isDay) R.drawable.mdi_weather_partly_cloudy else R.drawable.mdi_weather_cloudy
                "light_rain", "rainy" -> R.drawable.mdi_weather_rainy
                "heavy_rain" -> R.drawable.mdi_weather_pouring
                "light_snow", "snowy" -> R.drawable.mdi_weather_snowy
                "heavy_snow" -> R.drawable.mdi_weather_snowy_heavy
                "fog" -> R.drawable.mdi_weather_fog
                "haze" -> R.drawable.mdi_weather_hazy
                "wind" -> R.drawable.mdi_weather_windy
                "sandstorm" -> R.drawable.mdi_weather_dust
                else -> if (isDay) R.drawable.mdi_weather_sunny else R.drawable.mdi_weather_night
            }
            
            
            val drawable = ContextCompat.getDrawable(context, iconResId)
            drawable?.let {
                val iconLeft = (cx - iconSize / 2).toInt()
                val iconTop = (weatherY - iconSize / 2).toInt()
                it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                it.draw(canvas)
            }
            
        } catch (e: Exception) {
            
            drawWeather(canvas, cx, cy, radius)
        }
    }
    
    
    private enum class WeatherType { SUNNY, CLOUDY, RAINY, SNOWY, NIGHT_CLEAR, NIGHT_CLOUDY }
    
    private fun drawWeather(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val weatherY = cy + radius * 0.45f
        
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..18
        
        
        val iconX = cx - radius * 0.08f
        val iconY = weatherY
        val iconSize = radius * 0.028f
        
        
        val weather = when (weatherCondition) {
            "sunny" -> if (isDay) WeatherType.SUNNY else WeatherType.NIGHT_CLEAR
            "cloudy" -> if (isDay) WeatherType.CLOUDY else WeatherType.NIGHT_CLOUDY
            "rainy" -> WeatherType.RAINY
            "snowy" -> WeatherType.SNOWY
            else -> if (isDay) WeatherType.SUNNY else WeatherType.NIGHT_CLEAR
        }
        
        drawWeatherIcon(canvas, iconX, iconY, iconSize, weather)
        
        
        val tempTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = radius * 0.055f
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.ITALIC)
            letterSpacing = 0.02f
            setShadowLayer(2f, 0f, 1f, Color.argb(180, 0, 0, 0))
        }
        canvas.drawText("${weatherTemp}°C", iconX + radius * 0.08f, weatherY + tempTextPaint.textSize * 0.35f, tempTextPaint)
    }
    
    private fun drawWeatherIcon(canvas: Canvas, x: Float, y: Float, size: Float, type: WeatherType) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        when (type) {
            WeatherType.SUNNY -> {
                
                paint.color = Color.parseColor("#ffd700")
                paint.style = Paint.Style.FILL
                paint.setShadowLayer(size, 0f, 0f, Color.parseColor("#ffd700"))
                canvas.drawCircle(x, y, size, paint)
                
                paint.strokeWidth = 1.5f
                paint.style = Paint.Style.STROKE
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val x1 = x + (size * 1.4f * Math.cos(angle)).toFloat()
                    val y1 = y + (size * 1.4f * Math.sin(angle)).toFloat()
                    val x2 = x + (size * 2f * Math.cos(angle)).toFloat()
                    val y2 = y + (size * 2f * Math.sin(angle)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
            }
            WeatherType.CLOUDY -> {
                
                paint.color = Color.parseColor("#cccccc")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x - size * 0.5f, y, size * 0.7f, paint)
                canvas.drawCircle(x + size * 0.3f, y - size * 0.2f, size * 0.8f, paint)
                canvas.drawCircle(x + size * 0.8f, y + size * 0.1f, size * 0.6f, paint)
            }
            WeatherType.RAINY -> {
                
                paint.color = Color.parseColor("#888888")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x - size * 0.3f, y - size * 0.3f, size * 0.5f, paint)
                canvas.drawCircle(x + size * 0.3f, y - size * 0.4f, size * 0.6f, paint)
                
                
                paint.color = Color.parseColor("#6699ff")
                paint.strokeWidth = 1.5f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(x - size * 0.4f, y + size * 0.3f, x - size * 0.5f, y + size * 0.8f, paint)
                canvas.drawLine(x, y + size * 0.4f, x - size * 0.1f, y + size * 0.9f, paint)
                canvas.drawLine(x + size * 0.4f, y + size * 0.3f, x + size * 0.3f, y + size * 0.8f, paint)
            }
            WeatherType.SNOWY -> {
                
                paint.color = Color.parseColor("#aaaaaa")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x - size * 0.3f, y - size * 0.3f, size * 0.5f, paint)
                canvas.drawCircle(x + size * 0.3f, y - size * 0.4f, size * 0.6f, paint)
                
                
                paint.color = Color.WHITE
                canvas.drawCircle(x - size * 0.4f, y + size * 0.5f, size * 0.15f, paint)
                canvas.drawCircle(x, y + size * 0.7f, size * 0.15f, paint)
                canvas.drawCircle(x + size * 0.4f, y + size * 0.5f, size * 0.15f, paint)
            }
            WeatherType.NIGHT_CLEAR -> {
                
                paint.color = Color.parseColor("#e0e0e0")
                paint.style = Paint.Style.FILL
                paint.setShadowLayer(size * 0.5f, 0f, 0f, Color.argb(128, 255, 255, 255))
                canvas.drawCircle(x, y, size, paint)
                paint.color = Color.parseColor("#101030")
                canvas.drawCircle(x + size * 0.4f, y - size * 0.2f, size * 0.7f, paint)
            }
            WeatherType.NIGHT_CLOUDY -> {
                
                paint.color = Color.parseColor("#888888")
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x + size * 0.5f, y + size * 0.2f, size * 0.6f, paint)
                
                paint.color = Color.parseColor("#cccccc")
                canvas.drawCircle(x - size * 0.3f, y - size * 0.2f, size * 0.7f, paint)
            }
        }
        paint.clearShadowLayer()
    }
    
    private fun drawDateWindow(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        
        val dateX = cx + radius * 0.45f
        val dateY = cy + radius * 0.25f
        val windowRadius = radius * 0.075f
        
        
        canvas.drawCircle(dateX, dateY, windowRadius, dateWindowPaint)
        
        
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444444")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawCircle(dateX, dateY, windowRadius, borderPaint)
        
        
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        canvas.drawText(day.toString(), dateX, dateY + datePaint.textSize * 0.35f, datePaint)
    }
    
    private fun drawHands(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        
        
        val sDeg = second * 6f + millis * 0.006f
        val mDeg = minute * 6f + second * 0.1f
        val hDeg = hour * 30f + minute * 0.5f
        
        
        drawTauffeeHand(canvas, cx, cy, hDeg, radius * 0.4f, radius * 0.03f, true)
        
        
        drawTauffeeHand(canvas, cx, cy, mDeg, radius * 0.65f, radius * 0.02f, true)
        
        
        drawSecondHand(canvas, cx, cy, sDeg, radius)
    }
    
    private fun drawTauffeeHand(canvas: Canvas, cx: Float, cy: Float, degrees: Float, length: Float, width: Float, hasLume: Boolean) {
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        
        
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - length)  
            lineTo(cx + width / 2, cy - length * 0.12f)  
            lineTo(cx + width / 2, cy)  
            lineTo(cx - width / 2, cy)  
            lineTo(cx - width / 2, cy - length * 0.12f)  
            close()
        }
        
        
        hourHandPaint.shader = android.graphics.LinearGradient(
            cx - width / 2, cy, cx + width / 2, cy,
            intArrayOf(Color.parseColor("#d8d8d8"), Color.parseColor("#fbfbfb")),
            floatArrayOf(0.5f, 0.5f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, hourHandPaint)
        hourHandPaint.shader = null
        
        
        if (hasLume) {
            val lumeRect = android.graphics.RectF(
                cx - 1f, cy - length * 0.9f,
                cx + 1f, cy - length * 0.15f
            )
            canvas.drawRect(lumeRect, lumePaint)
        }
        
        canvas.restore()
    }
    
    private fun drawSecondHand(canvas: Canvas, cx: Float, cy: Float, degrees: Float, radius: Float) {
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        
        val length = radius * 0.7f
        val tailLength = radius * 0.18f
        
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..18
        val secColor = if (isDay) Color.parseColor("#ff4444") else accentGlow
        
        
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secColor
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            setShadowLayer(4f, 0f, 0f, secColor)
        }
        canvas.drawLine(cx, cy + tailLength, cx, cy - length, secPaint)
        
        
        val counterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secColor
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 0f, secColor)
        }
        val counterRect = android.graphics.RectF(
            cx - 3f, cy + tailLength * 0.3f,
            cx + 3f, cy + tailLength * 0.9f
        )
        canvas.drawRoundRect(counterRect, 2f, 2f, counterPaint)
        
        canvas.restore()
    }
    
    private fun drawPinion(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val pinionRadius = radius * 0.02f
        
        
        centerPaint.shader = android.graphics.RadialGradient(
            cx - pinionRadius * 0.3f, cy - pinionRadius * 0.3f, pinionRadius,
            intArrayOf(metalLight, Color.parseColor("#444444")),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, pinionRadius, centerPaint)
        centerPaint.shader = null
        
        
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawCircle(cx, cy, pinionRadius, borderPaint)
    }
    
    private fun drawCrystal(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        
        val crystalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(5, 255, 255, 255)),
                floatArrayOf(0.7f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, crystalPaint)
        
    }
}
