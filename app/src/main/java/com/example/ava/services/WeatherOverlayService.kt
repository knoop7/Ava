package com.example.ava.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.ava.R
import com.example.ava.settings.playerSettingsStore
import com.example.ava.weather.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random


class WeatherOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var weatherView: WeatherOverlayView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isWeatherEnabled = false
    private var isWeatherVisible = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
    }
    
    private fun ensureWeatherViewCreated() {
        if (weatherView == null) {
            createWeatherView()
            fetchWeather()
        }
    }

    private var lastCity: String = ""
    
    private fun fetchWeather() {
        serviceScope.launch {
            try {
                val settings = playerSettingsStore.data.first()
                val city = settings.weatherCity
                if (city.isBlank()) {
                    Log.w(TAG, "No city configured, skipping weather fetch")
                    return@launch
                }
                lastCity = city
                val weather = WeatherService.getWeather(city)
                weatherView?.updateWeather(weather)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch weather: ${e.message}")
            }
        }
        
        handler.postDelayed({ fetchWeather() }, 60 * 1000L)
    }
    
    
    private fun checkAndRefreshIfSettingsChanged() {
        serviceScope.launch {
            try {
                val settings = playerSettingsStore.data.first()
                val city = settings.weatherCity
                
                
                if (city != lastCity && city.isNotBlank()) {
                    lastCity = city
                    val weather = WeatherService.getWeather(city)
                    weatherView?.updateWeather(weather)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check settings change: ${e.message}")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createWeatherView() {
        val displayMetrics = resources.displayMetrics

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        
        val realMetrics = android.util.DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager?.defaultDisplay?.getRealMetrics(realMetrics)
        } else {
            windowManager?.defaultDisplay?.getMetrics(realMetrics)
        }
        val realWidth = realMetrics.widthPixels
        val realHeight = realMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        weatherView = WeatherOverlayView(this, realWidth, realHeight).apply {
            
            setOnTouchListener(DoubleTapPassThroughListener(
                onDoubleTap = { toggleVisibility() },
                onSwipeLeft = { switchToClock() },  
                windowManager = windowManager
            ))
        }

        try {
            windowManager?.addView(weatherView, params)
            weatherView?.startAnimation()
            isWeatherEnabled = true
            isWeatherVisible = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add weather window", e)
        }
    }

    private fun toggleVisibility() {
        if (!isWeatherEnabled) return
        
        if (isWeatherVisible) {
            
            weatherView?.stopAnimation()
            weatherView?.visibility = View.GONE
            isWeatherVisible = false
        } else {
            
            weatherView?.visibility = View.VISIBLE
            weatherView?.startAnimation()
            isWeatherVisible = true
        }
    }

    private fun switchToClock() {
        
        serviceScope.launch {
            try {
                val settings = playerSettingsStore.data.first()
                
                if (settings.enableDreamClock) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        hide(this@WeatherOverlayService)
                        DreamClockService.show(this@WeatherOverlayService)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check clock settings: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                ensureWeatherViewCreated()
                isWeatherEnabled = true
                weatherView?.visibility = View.VISIBLE
                weatherView?.startAnimation()
                isWeatherVisible = true
                checkAndRefreshIfSettingsChanged()
            }
            ACTION_HIDE -> {
                isWeatherEnabled = false
                weatherView?.stopAnimation()
                weatherView?.visibility = View.GONE
                isWeatherVisible = false
            }
            ACTION_TOGGLE -> {
                if (isWeatherEnabled) {
                    toggleVisibility()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        weatherView?.stopAnimation()
        try {
            weatherView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove weather window", e)
        }
    }

    companion object {
        private const val TAG = "WeatherOverlayService"
        const val ACTION_SHOW = "com.example.ava.SHOW_WEATHER"
        const val ACTION_HIDE = "com.example.ava.HIDE_WEATHER"
        const val ACTION_TOGGLE = "com.example.ava.TOGGLE_WEATHER"

        fun show(context: Context) {
            val intent = Intent(context, WeatherOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, WeatherOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun toggle(context: Context) {
            val intent = Intent(context, WeatherOverlayService::class.java).apply {
                action = ACTION_TOGGLE
            }
            context.startService(intent)
        }
    }
}


class WeatherOverlayView(
    context: Context,
    private var screenWidth: Int,
    private var screenHeight: Int
) : View(context) {

    private fun isChineseLocale(): Boolean {
        val locale = java.util.Locale.getDefault()
        val language = locale.language.lowercase()
        val country = locale.country.uppercase()
        val timezone = java.util.TimeZone.getDefault().id
        return language.startsWith("zh") || 
            country in listOf("CN", "TW", "HK", "MO") ||
            timezone.startsWith("Asia/Shanghai") || 
            timezone.startsWith("Asia/Chongqing") ||
            timezone.startsWith("Asia/Hong_Kong") ||
            timezone.startsWith("Asia/Taipei")
    }

    init {
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            screenWidth = w
            screenHeight = h
            initParticles()
        }
    }

    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(
            if (parentWidth > 0) parentWidth else screenWidth,
            if (parentHeight > 0) parentHeight else screenHeight
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    
    private var temperature = 0
    private var condition = ""
    private var humidity = 0
    private var windLevel = "--"
    private var windDirection = ""    
    private var windDirectionEn = ""  
    private var aqi = 0
    private var pm25 = 0
    private var visibility = 0f
    private var cityName = ""
    private var isDay = true

    
    private val particles = mutableListOf<Particle>()
    private val maxParticles: Int
        get() = when (condition) {
            "heavy_rain", "sandstorm" -> 100
            "light_rain", "heavy_snow" -> 60
            "light_snow" -> 35
            "wind" -> 50
            "fog" -> 30
            "haze" -> 50
            else -> 40
        }

    
    private var lastTapTime = 0L
    private var tapCount = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var onDoubleTap: (() -> Unit)? = null
    private var onSwipeRight: (() -> Unit)? = null

    
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dashboardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    
    private val oswaldLight: Typeface = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.ava.R.font.oswald_light)
            ?: Typeface.create("sans-serif-light", Typeface.NORMAL)
    } catch (e: Exception) {
        Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val rajdhaniSemibold: Typeface = try {
        androidx.core.content.res.ResourcesCompat.getFont(context, com.example.ava.R.font.rajdhani_semibold)
            ?: Typeface.create("sans-serif", Typeface.BOLD)
    } catch (e: Exception) {
        Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val labelTypeface = Typeface.create("sans-serif", Typeface.NORMAL)

    fun setOnDoubleTap(callback: () -> Unit) { onDoubleTap = callback }
    fun setOnSwipeRight(callback: () -> Unit) { onSwipeRight = callback }

    fun updateWeather(data: com.example.ava.weather.WeatherData) {
        temperature = data.temperature
        condition = data.condition
        humidity = data.humidity
        windLevel = com.example.ava.weather.WeatherService.getWindLevel(data.windSpeed)
        windDirection = data.windDirection
        windDirectionEn = data.windDirectionEn
        aqi = data.aqi
        pm25 = data.pm25
        visibility = data.visibility
        cityName = data.city
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        isDay = hour in 6..18
        
        initParticles()
        postInvalidate()
    }

    private fun initParticles() {
        particles.clear()
        repeat(maxParticles) {
            particles.add(createParticle(true))
        }
    }

    private fun createParticle(initial: Boolean): Particle {
        val p = Particle()
        p.x = Random.nextFloat() * screenWidth
        p.y = if (initial) Random.nextFloat() * screenHeight else -20f
        p.opacity = Random.nextFloat() * 0.5f + 0.1f

        when (condition) {
            "sunny", "cloudy" -> {
                p.vx = (Random.nextFloat() - 0.5f) * 0.3f
                p.vy = (Random.nextFloat() - 0.5f) * 0.3f
                p.radius = Random.nextFloat() * 1.5f
                p.isLine = false
            }
            "rainy", "light_rain" -> {
                p.vx = 0.3f
                p.vy = 4f + Random.nextFloat() * 4f
                p.length = 8f + Random.nextFloat() * 8f
                p.isLine = true
            }
            "heavy_rain" -> {
                p.vx = 0.8f
                p.vy = 10f + Random.nextFloat() * 10f
                p.length = 18f + Random.nextFloat() * 15f
                p.isLine = true
            }
            "snowy", "light_snow" -> {
                p.vx = (Random.nextFloat() - 0.5f) * 1f
                p.vy = 0.8f + Random.nextFloat() * 1.2f
                p.radius = 1f + Random.nextFloat() * 2f
                p.isLine = false
            }
            "heavy_snow" -> {
                p.vx = (Random.nextFloat() - 0.5f) * 1.5f
                p.vy = 1.5f + Random.nextFloat() * 2.5f
                p.radius = 2f + Random.nextFloat() * 3f
                p.isLine = false
            }
            "sandstorm" -> {
                p.x = if (initial) Random.nextFloat() * screenWidth else -50f
                p.y = Random.nextFloat() * screenHeight
                p.vx = 4f + Random.nextFloat() * 4f
                p.vy = (Random.nextFloat() - 0.5f) * 1.5f
                p.radius = 1f + Random.nextFloat() * 1.5f
                p.isLine = false
            }
            "wind" -> {
                p.x = if (initial) Random.nextFloat() * screenWidth else -80f
                p.y = Random.nextFloat() * screenHeight
                p.vx = 8f + Random.nextFloat() * 6f
                p.vy = (Random.nextFloat() - 0.5f) * 0.3f
                p.length = 60f + Random.nextFloat() * 50f
                p.isLine = true
            }
            "fog" -> {
                p.vx = (Random.nextFloat() - 0.5f) * 0.5f
                p.vy = (Random.nextFloat() - 0.5f) * 0.3f
                p.radius = 3f + Random.nextFloat() * 5f
                p.isLine = false
            }
            "haze" -> {
                p.vx = (Random.nextFloat() - 0.5f) * 0.6f
                p.vy = (Random.nextFloat() - 0.5f) * 0.4f
                p.radius = 1.5f + Random.nextFloat() * 2.5f
                p.isLine = false
            }
        }
        return p
    }

    fun startAnimation() {
        if (isRunning) return
        isRunning = true
        initParticles()
        scheduleUpdate()
    }

    fun stopAnimation() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleUpdate() {
        if (!isRunning) return
        handler.postDelayed({
            updateParticles()
            invalidate()
            scheduleUpdate()
        }, 16) 
    }

    private fun updateParticles() {
        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            if (p.x > screenWidth + 100 || p.x < -100 || p.y > screenHeight + 20) {
                val newP = createParticle(false)
                p.x = newP.x
                p.y = newP.y
                p.vx = newP.vx
                p.vy = newP.vy
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        
        drawSkyBackground(canvas)
        
        
        drawExposureLayer(canvas)
        
        
        if (condition == "sunny" && isDay) {
            drawSunAssembly(canvas)
        }
        
        
        if (condition in listOf("cloudy", "light_rain", "heavy_rain", "light_snow", "heavy_snow", "fog")) {
            drawClouds(canvas)
        }
        
        
        drawCoreGlow(canvas)
        
        
        drawParticles(canvas)
        
        
        drawUILayer(canvas)
    }

    private fun drawSkyBackground(canvas: Canvas) {
        val colors = when (condition) {
            
            "sunny" -> if (isDay) intArrayOf(Color.parseColor("#0284c7"), Color.parseColor("#0369a1"))
                       else intArrayOf(Color.parseColor("#1e3a5f"), Color.parseColor("#0f172a"))
            "cloudy" -> if (isDay) intArrayOf(Color.parseColor("#0ea5e9"), Color.parseColor("#0284c7"))
                        else intArrayOf(Color.parseColor("#334155"), Color.parseColor("#1e293b"))
            "rainy", "light_rain" -> intArrayOf(Color.parseColor("#64748b"), Color.parseColor("#475569"))
            "heavy_rain" -> intArrayOf(Color.parseColor("#334155"), Color.parseColor("#1e293b"))
            "snowy", "light_snow", "heavy_snow" -> intArrayOf(Color.parseColor("#94a3b8"), Color.parseColor("#64748b"))
            "fog" -> intArrayOf(Color.parseColor("#d1d5db"), Color.parseColor("#9ca3af"))
            "haze" -> intArrayOf(Color.parseColor("#a8a29e"), Color.parseColor("#78716c"))
            "wind" -> intArrayOf(Color.parseColor("#2c3e50"), Color.parseColor("#bdc3c7"))
            "sandstorm" -> intArrayOf(Color.parseColor("#5d4037"), Color.parseColor("#3e2723"))
            else -> intArrayOf(Color.parseColor("#0284c7"), Color.parseColor("#0369a1"))
        }
        
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, screenHeight.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), bgPaint)
        
        
        if (condition == "sunny" && isDay) {
            drawSunnyLightBlobs(canvas)
        }
    }
    
    
    private fun drawSunnyLightBlobs(canvas: Canvas) {
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        cloudPaint.style = Paint.Style.FILL
        
        cloudPaint.maskFilter = android.graphics.BlurMaskFilter(
            screenWidth * 0.08f, android.graphics.BlurMaskFilter.Blur.NORMAL
        )
        cloudPaint.color = Color.argb(100, 255, 255, 255)  
        
        
        
        cloudPaint.alpha = 100
        canvas.drawCircle(screenWidth * 0.25f, screenHeight * 0.12f, screenWidth * 0.2f, cloudPaint)
        
        
        cloudPaint.alpha = 130
        canvas.drawCircle(screenWidth * 0.5f, screenHeight * 0.08f, screenWidth * 0.28f, cloudPaint)
        
        
        cloudPaint.alpha = 80
        canvas.drawCircle(screenWidth * 0.68f, screenHeight * 0.15f, screenWidth * 0.15f, cloudPaint)
        
        
        
        cloudPaint.alpha = 80
        canvas.drawCircle(screenWidth * 0.85f, screenHeight * 0.45f, screenWidth * 0.22f, cloudPaint)
        
        
        cloudPaint.alpha = 100
        canvas.drawCircle(screenWidth * 0.55f, screenHeight * 0.5f, screenWidth * 0.3f, cloudPaint)
        
        
        cloudPaint.maskFilter = null
    }

    private fun drawExposureLayer(canvas: Canvas) {
        if (isDay) {
            
            glowPaint.shader = RadialGradient(
                screenWidth * 0.8f, screenHeight * 0.2f, screenWidth * 0.7f,
                intArrayOf(Color.argb(89, 255, 255, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        } else {
            
            glowPaint.shader = RadialGradient(
                screenWidth * 0.5f, screenHeight * 0.5f, screenWidth * 0.5f,
                intArrayOf(Color.TRANSPARENT, Color.argb(77, 0, 0, 0)),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), glowPaint)
    }

    private fun drawSunAssembly(canvas: Canvas) {
        val sunX = screenWidth - 100f
        val sunY = 100f
        val sunRadius = 250f
        
        glowPaint.shader = RadialGradient(
            sunX, sunY, sunRadius,
            intArrayOf(
                Color.argb(153, 255, 250, 240),
                Color.argb(51, 255, 230, 200),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sunX, sunY, sunRadius, glowPaint)
    }

    
    private var cloudAnimTime = 0L
    
    private fun drawClouds(canvas: Canvas) {
        val cloudAlpha = when (condition) {
            "cloudy" -> 45
            "light_rain", "light_snow" -> 40
            "heavy_rain", "heavy_snow" -> 35
            "fog" -> 55
            else -> 40
        }
        
        
        cloudAnimTime = System.currentTimeMillis()
        
        
        val t1 = cloudAnimTime / 8000.0  
        val t2 = cloudAnimTime / 12000.0 
        val t3 = cloudAnimTime / 15000.0 
        
        val offset1X = (Math.sin(t1) * 30 + Math.sin(t1 * 1.7) * 15).toFloat()
        val offset1Y = (Math.sin(t1 * 0.8) * 10 + Math.cos(t1 * 1.3) * 8).toFloat()
        
        val offset2X = (Math.sin(t2 + 1.5) * 35 + Math.cos(t2 * 1.4) * 18).toFloat()
        val offset2Y = (Math.cos(t2 * 0.9) * 12 + Math.sin(t2 * 1.1) * 6).toFloat()
        
        val offset3X = (Math.sin(t3 + 3.0) * 25 + Math.sin(t3 * 1.2) * 20).toFloat()
        val offset3Y = (Math.cos(t3 * 0.7) * 8 + Math.sin(t3 * 1.5) * 10).toFloat()
        
        
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        cloudPaint.style = Paint.Style.FILL
        cloudPaint.maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
        
        
        cloudPaint.shader = LinearGradient(
            0f, 0f, 0f, screenHeight * 0.25f,
            intArrayOf(Color.argb(cloudAlpha, 255, 255, 255), Color.argb(cloudAlpha / 3, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(offset1X, offset1Y)
        canvas.drawRect(-30f, -20f, screenWidth * 0.45f + 30f, screenHeight * 0.25f, cloudPaint)
        canvas.restore()
        
        
        cloudPaint.shader = LinearGradient(
            0f, 0f, 0f, screenHeight * 0.3f,
            intArrayOf(Color.argb(cloudAlpha, 255, 255, 255), Color.argb(cloudAlpha / 3, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(offset2X, offset2Y)
        canvas.drawRect(screenWidth * 0.5f - 30f, -20f, screenWidth + 30f, screenHeight * 0.3f, cloudPaint)
        canvas.restore()
        
        
        cloudPaint.shader = LinearGradient(
            0f, screenHeight * 0.1f, 0f, screenHeight * 0.35f,
            intArrayOf(Color.argb(cloudAlpha / 2, 255, 255, 255), Color.argb(cloudAlpha / 4, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.save()
        canvas.translate(offset3X, offset3Y)
        canvas.drawRect(screenWidth * 0.2f - 30f, screenHeight * 0.1f - 20f, screenWidth * 0.8f + 30f, screenHeight * 0.35f, cloudPaint)
        canvas.restore()
        
        cloudPaint.maskFilter = null
    }

    private fun drawCoreGlow(canvas: Canvas) {
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        
        
        val glowColor = when (condition) {
            "sunny" -> if (isDay) intArrayOf(Color.argb(80, 255, 230, 200), Color.argb(40, 255, 240, 220), Color.TRANSPARENT)
                       else intArrayOf(Color.argb(50, 220, 230, 255), Color.argb(25, 200, 210, 240), Color.TRANSPARENT)
            "rainy", "light_rain", "heavy_rain" -> intArrayOf(Color.argb(60, 100, 150, 255), Color.argb(30, 120, 170, 255), Color.TRANSPARENT)
            "snowy", "light_snow", "heavy_snow" -> intArrayOf(Color.argb(70, 255, 255, 255), Color.argb(35, 240, 245, 255), Color.TRANSPARENT)
            "cloudy" -> intArrayOf(Color.argb(60, 200, 230, 255), Color.argb(30, 210, 235, 255), Color.TRANSPARENT)
            "wind" -> intArrayOf(Color.argb(50, 150, 255, 220), Color.argb(25, 170, 255, 230), Color.TRANSPARENT)
            "sandstorm" -> intArrayOf(Color.argb(60, 255, 180, 100), Color.argb(30, 255, 200, 130), Color.TRANSPARENT)
            "haze" -> intArrayOf(Color.argb(50, 180, 160, 130), Color.argb(25, 190, 170, 140), Color.TRANSPARENT)
            "fog" -> intArrayOf(Color.argb(60, 220, 220, 220), Color.argb(30, 230, 230, 230), Color.TRANSPARENT)
            else -> if (isDay) intArrayOf(Color.argb(60, 255, 255, 255), Color.argb(30, 255, 255, 255), Color.TRANSPARENT)
                    else intArrayOf(Color.argb(50, 220, 230, 255), Color.argb(25, 200, 210, 240), Color.TRANSPARENT)
        }
        
        
        val radius = if (!isDay && (condition == "sunny" || condition == "cloudy" || condition.isEmpty())) 450f else 420f
        val blurRadius = 180f  
        
        val stops = floatArrayOf(0f, 0.5f, 1f)
        glowPaint.shader = RadialGradient(cx, cy, radius, glowColor, stops, Shader.TileMode.CLAMP)
        glowPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(cx, cy, radius, glowPaint)
        glowPaint.maskFilter = null
    }

    private fun drawParticles(canvas: Canvas) {
        particles.forEach { p ->
            val color = when (condition) {
                "sunny", "cloudy" -> Color.argb((p.opacity * 255).toInt(), 255, 255, 255)
                "rainy", "light_rain" -> Color.argb((p.opacity * 0.7f * 255).toInt(), 180, 210, 255)
                "heavy_rain" -> Color.argb((p.opacity * 255).toInt(), 160, 200, 255)
                "snowy", "light_snow" -> Color.argb((p.opacity * 0.8f * 255).toInt(), 255, 255, 255)
                "heavy_snow" -> Color.argb((p.opacity * 255).toInt(), 255, 255, 255)
                "sandstorm" -> Color.argb((p.opacity * 255).toInt(), 210, 160, 100)
                "wind" -> Color.argb((p.opacity * 0.35f * 255).toInt(), 255, 255, 255)
                "fog" -> Color.argb((p.opacity * 0.3f * 255).toInt(), 255, 255, 255)
                "haze" -> Color.argb((p.opacity * 0.5f * 255).toInt(), 180, 160, 130)
                else -> Color.argb((p.opacity * 255).toInt(), 255, 255, 255)
            }
            
            particlePaint.color = color
            if (p.isLine) {
                particlePaint.strokeWidth = 1f
                particlePaint.style = Paint.Style.STROKE
                val endY = if (condition == "wind") p.y + 1 else p.y + p.length
                canvas.drawLine(p.x, p.y, p.x + p.vx, endY, particlePaint)
            } else {
                particlePaint.style = Paint.Style.FILL
                canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
            }
        }
    }

    private fun drawUILayer(canvas: Canvas) {
        
        val isSmallSquareScreen = kotlin.math.abs(screenWidth - screenHeight) < 50 && screenWidth <= 500
        val vmin = minOf(screenWidth, screenHeight).toFloat()
        val padding = if (isSmallSquareScreen) screenWidth * 0.1f else screenWidth * 0.06f
        
        
        val maskPaint = Paint()
        maskPaint.shader = LinearGradient(
            0f, screenHeight * 0.75f, 0f, screenHeight.toFloat(),
            Color.TRANSPARENT, Color.argb(153, 0, 0, 0),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, screenHeight * 0.75f, screenWidth.toFloat(), screenHeight.toFloat(), maskPaint)
        
        
        if (isDay) {
            textPaint.setShadowLayer(8f, 0f, 2f, Color.argb(77, 0, 0, 0))
        } else {
            textPaint.setShadowLayer(4f, 0f, 1f, Color.argb(50, 0, 0, 0))
        }
        
        
        if (!isSmallSquareScreen) {
            val topBarY = padding + vmin * 0.05f
            val iconSize = (vmin * 0.045f).toInt()  
            
            
            val locationIcon = ContextCompat.getDrawable(context, R.drawable.mdi_map_marker)
            locationIcon?.let {
                val iconLeft = (padding + 40f).toInt()
                val iconTop = (topBarY - iconSize * 0.75f).toInt()  
                it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                it.draw(canvas)
            }
            
            
            textPaint.color = Color.WHITE
            textPaint.textSize = vmin * 0.04f
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textPaint.letterSpacing = 0.02f
            textPaint.textAlign = Paint.Align.LEFT
            val cityX = padding + 40f + iconSize + vmin * 0.015f  
            canvas.drawText(cityName, cityX, topBarY, textPaint)
            
            
            val cityWidth = textPaint.measureText(cityName)
            val conditionText = " · ${getConditionText(condition)}"
            textPaint.alpha = 180
            canvas.drawText(conditionText, cityX + cityWidth, topBarY, textPaint)
            textPaint.alpha = 255
            
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeStr = timeFormat.format(Date())
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = vmin * 0.05f
            textPaint.typeface = rajdhaniSemibold
            textPaint.alpha = 230
            canvas.drawText(timeStr, screenWidth - padding - 40f, topBarY, textPaint)
            textPaint.alpha = 255
        }
        
        
        val tempY = if (isSmallSquareScreen) screenHeight * 0.45f - 40f else screenHeight * 0.35f - 20f
        val tempSize = if (isSmallSquareScreen) vmin * 0.55f else vmin * 0.4f
        
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = oswaldLight
        textPaint.textSize = tempSize
        textPaint.letterSpacing = -0.03f
        textPaint.color = Color.WHITE
        
        val tempText = "$temperature"
        
        val digitCount = tempText.replace("-", "").length + (if (temperature < 0) 1 else 0)
        val offsetX = if (isSmallSquareScreen && digitCount > 1) vmin * 0.04f * (digitCount - 1) else 0f
        val tempX = screenWidth / 2f - offsetX
        canvas.drawText(tempText, tempX, tempY + textPaint.textSize * 0.35f, textPaint)
        
        
        val tempBounds = android.graphics.Rect()
        textPaint.getTextBounds(tempText, 0, tempText.length, tempBounds)
        val tempHalfWidth = tempBounds.width() / 2f
        
        val unitSize = if (isSmallSquareScreen) vmin * 0.12f else vmin * 0.08f
        textPaint.textSize = unitSize
        textPaint.typeface = oswaldLight
        textPaint.alpha = 128
        val unitX = tempX + tempHalfWidth + vmin * 0.03f
        val unitY = tempY - vmin * 0.05f
        canvas.drawText("°", unitX, unitY, textPaint)
        textPaint.alpha = 255
        
        
        drawDashboard(canvas, padding, isSmallSquareScreen)
    }

    private fun drawDashboard(canvas: Canvas, padding: Float, isSmallSquareScreen: Boolean) {
        val vmin = minOf(screenWidth, screenHeight).toFloat()
        
        
        val gridLeft = padding
        val gridRight = screenWidth - padding
        val gridWidth = gridRight - gridLeft
        val cellWidth = gridWidth / 3f
        val cellHeight = if (isSmallSquareScreen) vmin * 0.14f else vmin * 0.16f
        val rowGap = if (isSmallSquareScreen) vmin * 0.02f else vmin * 0.03f
        
        
        val gridHeight = cellHeight * 2 + rowGap
        val gridBottom = screenHeight - padding - (if (isSmallSquareScreen) 10f else 0f)
        val gridTop = gridBottom - gridHeight
        
        
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 1f
        
        
        val lineOffset = 5f
        
        
        val midY = gridTop + cellHeight + rowGap / 2 + lineOffset
        val hLineMargin = gridWidth * 0.08f
        linePaint.shader = LinearGradient(
            gridLeft + hLineMargin, midY, gridRight - hLineMargin, midY,
            intArrayOf(Color.TRANSPARENT, Color.argb(20, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawLine(gridLeft + hLineMargin, midY, gridRight - hLineMargin, midY, linePaint)
        
        
        val vLineMargin = cellHeight * 0.35f
        for (i in 1..2) {
            val lineX = gridLeft + cellWidth * i
            val lineTop = gridTop + vLineMargin + lineOffset
            val lineBottom = gridBottom - vLineMargin + lineOffset
            linePaint.shader = LinearGradient(
                lineX, lineTop, lineX, lineBottom,
                intArrayOf(Color.TRANSPARENT, Color.argb(25, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawLine(lineX, lineTop, lineX, lineBottom, linePaint)
        }
        
        
        val isCN = isChineseLocale()
        val cards = listOf(
            Triple(if (isCN) "湿度" else "Humidity", "$humidity", "%"),
            Triple(if (isCN) "空气" else "Air", "$aqi", "AQI"),
            Triple(if (isCN) "气压" else "Pressure", "0", "HPA"),
            Triple(if (isCN) "风速" else "Wind", "${String.format("%.1f", windLevel.replace("级", "").replace("-", ".").toFloatOrNull() ?: 3f)}", "KM/H"),
            Triple("PM2.5", "$pm25", "μg"),
            Triple(if (isCN) "风向" else "Dir", windDirectionEn.ifEmpty { "N" }, "")
        )
        
        cards.forEachIndexed { index, (label, value, unit) ->
            val col = index % 3
            val row = index / 3
            val cellX = gridLeft + col * cellWidth + cellWidth / 2
            val cellY = gridTop + row * (cellHeight + rowGap) + cellHeight / 2
            
            drawGridCell(canvas, cellX, cellY, label, value, unit, vmin, isSmallSquareScreen)
        }
    }
    
    private fun drawGridCell(canvas: Canvas, x: Float, y: Float, label: String, value: String, unit: String, vmin: Float, isSmall: Boolean) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.setShadowLayer(4f, 0f, 2f, Color.argb(100, 0, 0, 0))
        
        
        val isChinese = value.any { it.code > 0x4E00 && it.code < 0x9FFF }
        
        
        if (isChinese) {
            
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textPaint.textSize = if (isSmall) vmin * 0.045f else vmin * 0.05f
        } else {
            textPaint.typeface = rajdhaniSemibold
            textPaint.textSize = if (isSmall) vmin * 0.055f else vmin * 0.06f
        }
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        
        val valueY = if (isChinese) y + vmin * 0.005f else y - vmin * 0.005f
        canvas.drawText(value, x, valueY, textPaint)
        
        
        textPaint.textSize = if (isSmall) vmin * 0.022f else vmin * 0.025f
        textPaint.alpha = 120
        canvas.drawText(unit, x, y + vmin * 0.035f, textPaint)
        
        
        textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textPaint.textSize = if (isSmall) vmin * 0.025f else vmin * 0.03f
        textPaint.alpha = 100
        canvas.drawText(label, x, y + vmin * 0.07f, textPaint)
        textPaint.alpha = 255
        textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 300) {
                    tapCount++
                    if (tapCount >= 2) {
                        onDoubleTap?.invoke()
                        tapCount = 0
                        return true
                    }
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                
                
                if (dx > 100 && abs(dy) < 100) {
                    onSwipeRight?.invoke()
                    return true
                }
            }
        }
        return false
    }

    private data class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var radius: Float = 1f,
        var length: Float = 10f,
        var opacity: Float = 0.5f,
        var isLine: Boolean = false
    )
    
    
    private fun getConditionText(condition: String): String {
        val isCN = isChineseLocale()
        return when (condition) {
            "sunny" -> if (isCN) "晴" else "Sunny"
            "cloudy" -> if (isCN) "多云" else "Cloudy"
            "light_rain" -> if (isCN) "小雨" else "Light Rain"
            "rainy" -> if (isCN) "雨" else "Rain"
            "heavy_rain" -> if (isCN) "大雨" else "Heavy Rain"
            "light_snow" -> if (isCN) "小雪" else "Light Snow"
            "snowy" -> if (isCN) "雪" else "Snow"
            "heavy_snow" -> if (isCN) "大雪" else "Heavy Snow"
            "fog" -> if (isCN) "雾" else "Fog"
            "haze" -> if (isCN) "霾" else "Haze"
            "wind" -> if (isCN) "大风" else "Windy"
            "sandstorm" -> if (isCN) "沙尘暴" else "Sandstorm"
            else -> if (isCN) "晴" else "Sunny"
        }
    }
}
