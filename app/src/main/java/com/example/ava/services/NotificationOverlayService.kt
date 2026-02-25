package com.example.ava.services

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.DisplayMetrics
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ava.notifications.FontAwesomeHelper
import com.example.ava.notifications.NotificationScenes
import com.example.ava.notifications.NotificationScene
import com.example.ava.settings.NotificationSettings
import com.example.ava.settings.NotificationSettingsStore
import com.example.ava.settings.notificationSettingsStore
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class NotificationOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    
    private var backgroundView: View? = null
    private var beamView: View? = null
    private var scanLine: View? = null  
    private var coreGlowView: View? = null
    private var techRingView: View? = null
    private var iconView: TextView? = null
    private var titleView: TextView? = null
    private var descView: TextView? = null
    private var subDescView: TextView? = null
    private var dividerView: View? = null
    private var dotView: View? = null
    
    
    private var auroraBlob1: View? = null
    private var auroraBlob2: View? = null
    private var auroraBlob3: View? = null
    private var auroraBlob4: View? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var autoHideRunnable: Runnable? = null
    private var currentScene: NotificationScene? = null
    private var techRingAnimator: ObjectAnimator? = null
    private var isShowingAnimation = false
    
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    
    private val notificationSettingsStore by lazy { NotificationSettingsStore(applicationContext.notificationSettingsStore) }

    
    private var iconSunRiseAnimator: ObjectAnimator? = null
    private var iconShadowAnimator: ValueAnimator? = null

    
    private var auroraAnimator1: Animator? = null
    private var auroraAnimator2: Animator? = null
    private var auroraAnimator3: Animator? = null
    private var auroraAnimator4: Animator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()
        
        initializeDefaultColors()
        
        
        notificationSettingsStore.sceneDisplayDuration.onEach { duration ->
            
            if (overlayView?.visibility == View.VISIBLE) {
                scheduleAutoHide()
            }
        }.launchIn(serviceScope)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        createOverlayView()
    }

    
    private fun initializeDefaultColors() {
        
        NotificationScenes.loadFromAssets(this)

        val defaultScene = NotificationScenes.getSceneById("morning")
        if (defaultScene != null) {
            
            updateContent(defaultScene)
            updateColors(defaultScene)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val display = windowManager?.defaultDisplay
        display?.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        
        val isPortraitOrSquare = screenHeight >= screenWidth  
        val minScreenSize = minOf(screenWidth, screenHeight)  
        
        
        
        val iconSize = (screenWidth * 0.05f / density).coerceIn(40f, 64f)
        
        val titleSize = (screenWidth * 0.05f / density).coerceIn(35f, 72f)
        
        val descSize = (screenWidth * 0.018f / density).coerceIn(16f, 26f)
        
        
        val rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#02040a"))
            clipChildren = false  
            clipToPadding = false
        }
        
        
        
        backgroundView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            
            val gradient = GradientDrawable()
            gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
            gradient.setGradientCenter(0.5f, 1.0f)  
            gradient.gradientRadius = screenHeight.toFloat()
            
            gradient.colors = intArrayOf(
                Color.parseColor("#66f59e0b"),  
                Color.parseColor("#33d97706"),  
                Color.TRANSPARENT               
            )
            background = gradient
        }
        
        
        auroraBlob1 = null
        auroraBlob2 = null
        auroraBlob3 = null
        auroraBlob4 = null

        rootContainer.addView(backgroundView)
        
        
        
        beamView = null
        
        
        scanLine = null
        
        
        
        val maxCardWidth = (1600 * density).toInt()
        val cardWidth: Int
        val cardHeight: Int
        val cardCornerRadius: Float
        
        if (isPortraitOrSquare) {
            
            cardWidth = screenWidth
            cardHeight = screenHeight
            cardCornerRadius = 0f  
        } else {
            
            cardWidth = ((screenWidth * 0.95f).toInt()).coerceAtMost(maxCardWidth)
            cardHeight = (screenHeight * 0.92f).toInt()
            cardCornerRadius = 16 * density  
        }

        val cardContainer = FrameLayout(this).apply {
            val lp = FrameLayout.LayoutParams(cardWidth, cardHeight)
            lp.gravity = Gravity.CENTER
            lp.topMargin = (5 * density).toInt()  
            layoutParams = lp

            
            clipChildren = false
            clipToPadding = false

            
            val cardBg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(180, 5, 10, 25),  
                    Color.argb(140, 5, 10, 25),  
                    Color.argb(80, 5, 10, 25)    
                )
            )
            cardBg.cornerRadius = cardCornerRadius
            if (!isPortraitOrSquare) {
                cardBg.setStroke((1 * density).toInt(), Color.parseColor("#19FFFFFF")) 
            }
            background = cardBg
        }
        
        
        if (!isPortraitOrSquare) {
            val cornerSize = (20 * density).toInt()
            val cornerThickness = (2 * density).toInt()
            val cornerMargin = (16 * density).toInt()
            
            
            addCornerMarker(cardContainer, Gravity.TOP or Gravity.START, cornerSize, cornerThickness, cornerMargin, density)
            
            addCornerMarker(cardContainer, Gravity.TOP or Gravity.END, cornerSize, cornerThickness, cornerMargin, density)
            
            addCornerMarker(cardContainer, Gravity.BOTTOM or Gravity.START, cornerSize, cornerThickness, cornerMargin, density)
            
            addCornerMarker(cardContainer, Gravity.BOTTOM or Gravity.END, cornerSize, cornerThickness, cornerMargin, density)
        }
        
        
        
        val maxContentWidth = (896 * density).toInt()
        val basePadding = if (isPortraitOrSquare) (16 * density).toInt() else (32 * density).toInt()
        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER  
            clipChildren = false  
            clipToPadding = false
            setPadding(basePadding, basePadding, basePadding, basePadding)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }

        
        
        
        val iconBoxSize = if (isPortraitOrSquare) {
            
            (screenWidth * 0.25f).toInt().coerceIn((90 * density).toInt(), (120 * density).toInt())
        } else {
            
            (screenWidth * 0.10f).toInt().coerceIn((80 * density).toInt(), (120 * density).toInt())
        }
        
        val iconBottomMargin = if (isPortraitOrSquare) (6 * density).toInt() else (24 * density).toInt()
        val iconContainer = FrameLayout(this).apply {
            val lp = LinearLayout.LayoutParams(iconBoxSize, iconBoxSize)
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = iconBottomMargin
            layoutParams = lp
            clipChildren = false  
            clipToPadding = false
        }
        
        
        
        
        coreGlowView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val gradient = GradientDrawable()
            gradient.shape = GradientDrawable.OVAL
            gradient.setGradientCenter(0.5f, 0.5f)
            gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
            gradient.gradientRadius = iconBoxSize / 2f
            
            gradient.colors = intArrayOf(
                Color.parseColor("#66fbbf24"),  
                Color.TRANSPARENT
            )
            background = gradient
            alpha = 0.5f  
            
            
            val coreView = this
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 3000  
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    
                    
                    val (newAlpha, newScale) = if (value <= 0.5f) {
                        val t = value / 0.5f
                        Pair(0.5f + 0.5f * t, 1f + 0.2f * t)
                    } else {
                        val t = (value - 0.5f) / 0.5f
                        Pair(1f - 0.5f * t, 1.2f - 0.2f * t)
                    }
                    coreView.alpha = newAlpha
                    coreView.scaleX = newScale
                    coreView.scaleY = newScale
                }
                start()
            }
        }
        iconContainer.addView(coreGlowView)
        
        

        
        
        val faTypeface = FontAwesomeHelper.loadFont(this)
        iconView = TextView(this).apply {
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            textSize = iconSize
            gravity = Gravity.CENTER
            typeface = faTypeface  
            setTextColor(Color.parseColor("#fde68a"))  
            text = FontAwesomeHelper.getIconChar("fa-sun")  
            setShadowLayer(25 * density, 0f, 0f, Color.parseColor("#E6fde68a"))
        }
        iconContainer.addView(iconView)

        
        startIconSunRiseAnimation()

        
        
        dotView = View(this).apply {
            val dotSize = (10 * density).toInt()  
            val lp = FrameLayout.LayoutParams(dotSize, dotSize)
            lp.gravity = Gravity.TOP or Gravity.END
            lp.topMargin = (8 * density).toInt()  
            lp.rightMargin = (8 * density).toInt()  
            layoutParams = lp
            
            val dotBg = GradientDrawable()
            dotBg.shape = GradientDrawable.OVAL
            dotBg.setColor(Color.parseColor("#fcd34d"))
            background = dotBg
            
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                outlineAmbientShadowColor = Color.parseColor("#fcd34d")
            }
            elevation = 15 * density
            
            
            ObjectAnimator.ofFloat(this, "alpha", 1f, 0.5f, 1f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
        iconContainer.addView(dotView)
        
        contentContainer.addView(iconContainer)
        
        
        
        
        dividerView = View(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            )
            
            val marginVh = if (isPortraitOrSquare) (screenHeight * 0.01f).toInt() else (screenHeight * 0.02f).toInt()
            lp.topMargin = marginVh
            lp.bottomMargin = marginVh
            layoutParams = lp
            alpha = 0.8f

            
            val dividerGradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#CCfbbf24"), Color.TRANSPARENT)
            )
            background = dividerGradient

            
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                outlineAmbientShadowColor = Color.parseColor("#99fbbf24") 
            }
        }
        contentContainer.addView(dividerView)
        
        
        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            
            val titleMargin = if (isPortraitOrSquare) (4 * density).toInt() else (8 * density).toInt()
            lp.topMargin = titleMargin
            lp.bottomMargin = titleMargin
            layoutParams = lp
        }

        
        
        titleView = TextView(this).apply {
            textSize = titleSize
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = ""
            
            
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            
            lp.bottomMargin = if (isPortraitOrSquare) (4 * density).toInt() else (8 * density).toInt()
            layoutParams = lp
        }
        titleContainer.addView(titleView)

        
        
        val descOuterContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipChildren = false  
            clipToPadding = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            
            lp.topMargin = if (isPortraitOrSquare) (1.3f * density).toInt() else (3.3f * density).toInt()
            layoutParams = lp
        }
        
        
        val descContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            
            setBaselineAligned(true)
            clipChildren = false  
            clipToPadding = false
            setBackgroundColor(Color.TRANSPARENT)  
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }
        
        
        
        descView = TextView(this).apply {
            textSize = descSize
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#fcd34d"))  
            text = ""
            setBackgroundColor(Color.TRANSPARENT)
            letterSpacing = 0.05f  
        }
        descContainer.addView(descView)

        
        subDescView = TextView(this).apply {
            textSize = descSize
            setTextColor(Color.parseColor("#e5e7eb"))  
            text = ""
            setBackgroundColor(Color.TRANSPARENT)
            letterSpacing = 0.05f  
        }
        descContainer.addView(subDescView)
        
        descOuterContainer.addView(descContainer)
        
        
        
        
        val haContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL  
            clipChildren = false  
            clipToPadding = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            lp.topMargin = (16 * density).toInt()  
            layoutParams = lp
        }

        
        
        val leftLine = View(this).apply {
            val lp = LinearLayout.LayoutParams((32 * density).toInt(), (1 * density).toInt())
            lp.rightMargin = (16 * density).toInt()  
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#33FFFFFF"))  
        }
        haContainer.addView(leftLine)

        
        
        val logoSize = ((screenHeight * 0.03f).toInt().coerceIn((28 * density).toInt(), (40 * density).toInt()))
        val haLogoContainer = FrameLayout(this).apply {
            val lp = LinearLayout.LayoutParams(logoSize, logoSize)
            lp.rightMargin = (8 * density).toInt()  
            layoutParams = lp

            val logoBg = GradientDrawable()
            logoBg.shape = GradientDrawable.OVAL
            logoBg.setColor(Color.parseColor("#0DFFFFFF"))  
            logoBg.setStroke((1 * density).toInt(), Color.parseColor("#1AFFFFFF"))  
            background = logoBg
            
            
            elevation = 100 * density
        }

        
        
        val haIcon = ImageView(this).apply {
            val iconSize = (logoSize * 0.62f).toInt()  
            val lp = FrameLayout.LayoutParams(iconSize, iconSize)
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            try {
                assets.open("ha_logo.png").use { inputStream ->
                    setImageBitmap(android.graphics.BitmapFactory.decodeStream(inputStream))
                }
            } catch (e: Exception) { }
            scaleType = ImageView.ScaleType.FIT_CENTER
            
            translationY = (-logoSize * 0.02f)
        }
        haLogoContainer.addView(haIcon)
        haContainer.addView(haLogoContainer)

        
        val haTextContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            layoutParams = lp
        }

        
        
        
        val haTitle = TextView(this).apply {
            text = "Home Assistant"
            
            textSize = (screenWidth * 0.012f / density).coerceIn(11.2f, 16f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.025f  
        }
        haTextContainer.addView(haTitle)

        
        
        
        val haSubtitle = TextView(this).apply {
            text = "SYSTEM NOTIFICATION"
            
            textSize = (screenWidth * 0.008f / density).coerceIn(6.4f, 10.4f)
            setTextColor(Color.parseColor("#99bfdbfe")) 
            letterSpacing = 0.05f  
        }
        haTextContainer.addView(haSubtitle)

        haContainer.addView(haTextContainer)

        
        
        val rightLine = View(this).apply {
            val lp = LinearLayout.LayoutParams((32 * density).toInt(), (1 * density).toInt())
            lp.leftMargin = (16 * density).toInt()  
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#33FFFFFF"))  
        }
        haContainer.addView(rightLine)
        
        
        descOuterContainer.addView(haContainer)

        
        
        
        
        
        titleContainer.addView(descOuterContainer)

        
        contentContainer.addView(titleContainer)
        
        cardContainer.addView(contentContainer)
        rootContainer.addView(cardContainer)
        
        overlayView = rootContainer

        
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
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            overlayView?.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification overlay", e)
        }
    }
    
    
    private fun startIconSunRiseAnimation() {
        
        iconSunRiseAnimator = ObjectAnimator.ofFloat(iconView, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 3000  
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            
            addUpdateListener {
                iconView?.scaleY = iconView?.scaleX ?: 1f
            }
            start()
        }

        
        iconShadowAnimator = ValueAnimator.ofFloat(20f, 35f, 20f).apply {
            duration = 3000  
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val shadowRadius = animation.animatedValue as Float
                val currentColor = iconView?.currentTextColor ?: Color.parseColor("#fde68a")
                iconView?.setShadowLayer(shadowRadius, 0f, 0f, adjustAlpha(currentColor, 0.8f))
            }
            start()
        }
    }

    
    private fun addCornerMarker(container: FrameLayout, gravity: Int, size: Int, thickness: Int, margin: Int, density: Float) {
        val corner = FrameLayout(this).apply {
            val lp = FrameLayout.LayoutParams(size, size)
            lp.gravity = gravity
            when (gravity) {
                Gravity.TOP or Gravity.START -> {
                    lp.leftMargin = margin
                    lp.topMargin = margin
                }
                Gravity.TOP or Gravity.END -> {
                    lp.rightMargin = margin
                    lp.topMargin = margin
                }
                Gravity.BOTTOM or Gravity.START -> {
                    lp.leftMargin = margin
                    lp.bottomMargin = margin
                }
                Gravity.BOTTOM or Gravity.END -> {
                    lp.rightMargin = margin
                    lp.bottomMargin = margin
                }
            }
            layoutParams = lp
            alpha = 0.5f  
        }
        
        
        val hLine = View(this).apply {
            val lp = FrameLayout.LayoutParams(size, thickness)
            lp.gravity = when (gravity) {
                Gravity.TOP or Gravity.START, Gravity.TOP or Gravity.END -> Gravity.TOP or Gravity.START
                else -> Gravity.BOTTOM or Gravity.START
            }
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#4DFFFFFF"))  
        }
        corner.addView(hLine)
        
        
        val vLine = View(this).apply {
            val lp = FrameLayout.LayoutParams(thickness, size)
            lp.gravity = when (gravity) {
                Gravity.TOP or Gravity.START, Gravity.BOTTOM or Gravity.START -> Gravity.TOP or Gravity.START
                else -> Gravity.TOP or Gravity.END
            }
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#4DFFFFFF"))  
        }
        corner.addView(vLine)
        
        container.addView(corner)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_SCENE -> {
                val sceneId = intent.getStringExtra(EXTRA_SCENE_ID)
                if (sceneId != null) {
                    showScene(sceneId)
                }
            }
            ACTION_SHOW_SCENE_BY_INDEX -> {
                val index = intent.getIntExtra(EXTRA_SCENE_INDEX, 0)
                showSceneByIndex(index)
            }
            ACTION_SHOW_SCENE_BY_TITLE -> {
                val sceneTitle = intent.getStringExtra(EXTRA_SCENE_TITLE)
                if (sceneTitle != null) {
                    showSceneByTitle(sceneTitle)
                }
            }
            ACTION_HIDE -> {
                hideOverlay()
            }
        }
    }

    private fun showScene(sceneId: String) {
        val scene = NotificationScenes.getSceneById(sceneId) ?: return
        displayScene(scene)
    }
    
    private fun showSceneByTitle(sceneTitle: String) {
        val scene = NotificationScenes.getSceneByTitle(sceneTitle) ?: return
        displayScene(scene)
    }
    
    private fun showSceneByIndex(index: Int) {
        val scene = NotificationScenes.getSceneByIndex(index) ?: return
        displayScene(scene)
    }
    
    private fun displayScene(scene: NotificationScene) {
        handler.post {
            currentScene = scene
            
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            
            
            if (isShowingAnimation) {
                overlayView?.animate()?.cancel()
            }
            
            updateContent(scene)
            updateColors(scene)
            
            isShowingAnimation = true
            overlayView?.alpha = 0f
            overlayView?.visibility = View.VISIBLE
            overlayView?.animate()
                ?.alpha(1f)
                ?.setDuration(300)
                ?.setInterpolator(AccelerateDecelerateInterpolator())
                ?.withEndAction {
                    isShowingAnimation = false
                }
                ?.start()
            
            
            playNotificationSound()
            
            scheduleAutoHide()
        }
    }
    
    
    private fun playNotificationSound() {
        serviceScope.launch {
            val settings = notificationSettingsStore.get()
            if (settings.soundEnabled && settings.soundUri.isNotEmpty()) {
                try {
                    val uri = Uri.parse(settings.soundUri)
                    val ringtone = RingtoneManager.getRingtone(this@NotificationOverlayService, uri)
                    ringtone?.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play notification sound", e)
                }
            }
        }
    }
    
    private fun updateContent(scene: NotificationScene) {
        
        iconView?.text = FontAwesomeHelper.getIconChar(scene.icon)
        titleView?.text = scene.title
        descView?.text = scene.desc
        subDescView?.text = " ${scene.subDesc}"
    }
    
    private fun updateColors(scene: NotificationScene) {
        val themeColors = scene.getThemeColorInts()
        val primaryColor = scene.getPrimaryColor()
        val beamColor = scene.getBeamColorInt()
        val dividerColor = scene.getDividerColorInt()
        val dotColor = scene.getDotColorInt()
        val iconColor = scene.getIconColorInt()

        
        val bgGradient = backgroundView?.background as? GradientDrawable
        if (bgGradient != null && themeColors.isNotEmpty()) {
            val color1 = adjustAlpha(themeColors.getOrElse(0) { primaryColor }, 0.4f)
            val color2 = adjustAlpha(themeColors.getOrElse(1) { primaryColor }, 0.2f)
            bgGradient.colors = intArrayOf(color1, color2, Color.TRANSPARENT)
        }

        
        val dividerGradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.TRANSPARENT, dividerColor, Color.TRANSPARENT)
        )
        dividerView?.background = dividerGradient

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dividerView?.outlineAmbientShadowColor = adjustAlpha(dividerColor, 0.6f)
        }

        
        descView?.setTextColor(primaryColor)

        
        iconView?.setTextColor(iconColor)
        iconView?.setShadowLayer(25f, 0f, 0f, adjustAlpha(iconColor, 0.9f))

        
        val beamGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.TRANSPARENT,
                beamColor,
                primaryColor,
                beamColor,
                Color.TRANSPARENT
            )
        )
        beamView?.background = beamGradient

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            beamView?.outlineAmbientShadowColor = adjustAlpha(primaryColor, 0.3f)
        }

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanLine?.outlineAmbientShadowColor = adjustAlpha(primaryColor, 0.2f)
        }

        
        val iconBoxSize = coreGlowView?.width ?: 100
        val coreGradient = GradientDrawable()
        coreGradient.shape = GradientDrawable.OVAL
        coreGradient.gradientType = GradientDrawable.RADIAL_GRADIENT
        coreGradient.gradientRadius = iconBoxSize / 2f
        coreGradient.colors = intArrayOf(
            adjustAlpha(primaryColor, 0.4f),
            Color.TRANSPARENT
        )
        coreGlowView?.background = coreGradient

        
        (dotView?.background as? GradientDrawable)?.setColor(dotColor)
    }
    
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (255 * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun scheduleAutoHide() {
        
        autoHideRunnable?.let { handler.removeCallbacks(it) }
        
        val currentRunnable = Runnable {
            overlayView?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.withEndAction {
                    overlayView?.visibility = View.GONE
                }
                ?.start()
        }
        autoHideRunnable = currentRunnable
        
        
        serviceScope.launch {
            val settings = notificationSettingsStore.get()
            
            if (autoHideRunnable === currentRunnable) {
                handler.postDelayed(currentRunnable, settings.sceneDisplayDuration.toLong())
            }
        }
    }

    private fun hideOverlay() {
        handler.post {
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            overlayView?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction {
                    overlayView?.visibility = View.GONE
                }
                ?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        
        
        serviceScope.cancel()

        
        techRingAnimator?.cancel()
        iconSunRiseAnimator?.cancel()
        iconShadowAnimator?.cancel()
        auroraAnimator1?.cancel()
        auroraAnimator2?.cancel()
        auroraAnimator3?.cancel()
        auroraAnimator4?.cancel()

        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove notification overlay", e)
        }
    }

    companion object {
        private const val TAG = "NotificationOverlay"
        
        const val ACTION_SHOW_SCENE = "com.example.ava.SHOW_NOTIFICATION_SCENE"
        const val ACTION_SHOW_SCENE_BY_INDEX = "com.example.ava.SHOW_NOTIFICATION_SCENE_INDEX"
        const val ACTION_SHOW_SCENE_BY_TITLE = "com.example.ava.SHOW_NOTIFICATION_SCENE_TITLE"
        const val ACTION_HIDE = "com.example.ava.HIDE_NOTIFICATION"
        const val EXTRA_SCENE_ID = "scene_id"
        const val EXTRA_SCENE_INDEX = "scene_index"
        const val EXTRA_SCENE_TITLE = "scene_title"

        fun showScene(context: Context, sceneId: String) {
            val intent = Intent(context, NotificationOverlayService::class.java).apply {
                action = ACTION_SHOW_SCENE
                putExtra(EXTRA_SCENE_ID, sceneId)
            }
            context.startService(intent)
        }
        
        fun showSceneByTitle(context: Context, sceneTitle: String) {
            val intent = Intent(context, NotificationOverlayService::class.java).apply {
                action = ACTION_SHOW_SCENE_BY_TITLE
                putExtra(EXTRA_SCENE_TITLE, sceneTitle)
            }
            context.startService(intent)
        }
        
        fun showSceneByIndex(context: Context, index: Int) {
            val intent = Intent(context, NotificationOverlayService::class.java).apply {
                action = ACTION_SHOW_SCENE_BY_INDEX
                putExtra(EXTRA_SCENE_INDEX, index)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, NotificationOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }
}
