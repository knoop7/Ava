package com.example.ava.services

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.ava.utils.EmotionKeywordDetector
import com.example.ava.utils.EmotionKeywordDetector.Expression

class FloatingWindowService : Service() {

    private enum class State { IDLE, LISTENING, PROCESSING, SPEAKING }
    private enum class DisplayMode { NONE, STREAMING, STATIC_TEXT, KARAOKE }

    private data class PerformanceProfile(
        val enableGlowBlur: Boolean,
        val useSmoothScroll: Boolean,
        val streamingUiThrottleMs: Long,
        val pulseAmplitude: Float,
        val glowStrength: Float
    ) {
        companion object {
            fun balanced() = PerformanceProfile(
                enableGlowBlur = true,
                useSmoothScroll = true,
                streamingUiThrottleMs = 0L,
                pulseAmplitude = 0.02f,
                glowStrength = 1.0f
            )

            fun lowPower() = PerformanceProfile(
                enableGlowBlur = false,
                useSmoothScroll = false,
                streamingUiThrottleMs = 90L,
                pulseAmplitude = 0.012f,
                glowStrength = 0.65f
            )
        }
    }
    

    private inner class EsperSphereView(
        context: Context,
        private var fixedScreenWidth: Int,
        private var fixedScreenHeight: Int
    ) : View(context) {
        private val density = context.resources.displayMetrics.density
        private fun dp(v: Float) = v * density
        

        private var calculatedSphereRatio = 0.32f
        
        private var currentExpression = Expression.NEUTRAL
        
        private var sphereDiameter = dp(120f)
        private var sphereRadius = sphereDiameter / 2f
        private var sphereCenterX = 0f
        private var sphereCenterY = 0f
        private var targetCenterY = 0f
        private var baseCenterY = 0f
        private var topCenterY = 0f
        
        
        private var sphereColor = 0xFF6366F1.toInt()
        private var targetColor = sphereColor
        private var colorTransition = 1f
        
        private var statusText = "LISTENING"
        private var statusDotColor = 0xFF00FFAA.toInt()
        private var showStatusBadge = false
        private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(204, 255, 255, 255)
            textSize = dp(14f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        
        private val expressionColors = mapOf(
            Expression.NEUTRAL   to 0xFF6366F1.toInt(),
            Expression.HAPPY     to 0xFF22C55E.toInt(),
            Expression.SAD       to 0xFF64748B.toInt(),
            Expression.SURPRISED to 0xFFF59E0B.toInt(),
            Expression.THINKING  to 0xFF8B5CF6.toInt(),
            Expression.SLEEPY    to 0xFF475569.toInt(),
            Expression.EXCITED   to 0xFFEC4899.toInt(),
            Expression.CONFUSED  to 0xFFF97316.toInt(),
            Expression.LISTENING to 0xFF3B82F6.toInt(),
            Expression.SPEAKING  to 0xFF06B6D4.toInt(),
            Expression.ANGRY     to 0xFFEF4444.toInt(),
            Expression.SHY       to 0xFFFB7185.toInt(),
            Expression.PROUD     to 0xFFFBBF24.toInt(),
            Expression.CURIOUS   to 0xFF14B8A6.toInt()
        )
        
        private var eyeWidth = dp(10f)
        private var eyeHeight = dp(30f)
        private var eyeCornerRadius = dp(3f)
        private var eyeGap = dp(30f)
        
        private var eyeHeightFactor = 1f
        private var eyeLeftPercent = 0.47f
        private var floatOffsetY = 0f
        private var pulseScale = 1f
        
        private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        
        private var glowAlpha = 0.06f
        private var glowScale = 1.15f
        
        
        private var isAnimating = false
        private var animationStartTime = 0L
        


        private val blinkAnimator = ValueAnimator.ofFloat(1f, 0.1f, 1f).apply {
            duration = 160L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { eyeHeightFactor = it.animatedValue as Float; invalidate() }
        }
        
        private var blinkInterval = 4000L
        
        private val blinkRunnable = object : Runnable {
            override fun run() {
                if (isAnimating) {
                    blinkAnimator.start()
                    handler?.postDelayed(this, blinkInterval)
                }
            }
        }
        
        private val mainAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 30000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                val now = System.currentTimeMillis()
                if (animationStartTime == 0L) animationStartTime = now
                val elapsed = now - animationStartTime
                


                val lookP = (elapsed % 10000L) / 10000f
                eyeLeftPercent = when {
                    lookP < 0.40f -> 0.47f
                    lookP < 0.45f -> 0.47f - (lookP - 0.40f) / 0.05f * 0.07f
                    lookP < 0.55f -> 0.40f
                    lookP < 0.60f -> 0.40f + (lookP - 0.55f) / 0.05f * 0.14f
                    lookP < 0.70f -> 0.54f
                    lookP < 0.75f -> 0.54f - (lookP - 0.70f) / 0.05f * 0.07f
                    else -> 0.47f
                }
                


                val floatP = (elapsed % 6000L) / 6000f
                floatOffsetY = if (floatP < 0.5f) {
                    -dp(10f) * (floatP / 0.5f)
                } else {
                    -dp(10f) * (1f - (floatP - 0.5f) / 0.5f)
                }
                
                val pulseP = (elapsed % 4000L) / 4000f
                pulseScale = 1f + performanceProfile.pulseAmplitude * kotlin.math.sin(pulseP * 2 * Math.PI.toFloat())
                

                val glowP = (elapsed % 4000L) / 4000f
                glowAlpha = (0.04f + 0.03f * kotlin.math.sin(glowP * 2 * Math.PI.toFloat()).toFloat()) * performanceProfile.glowStrength
                glowScale = 1.12f + (0.05f * performanceProfile.glowStrength) * kotlin.math.sin(glowP * 2 * Math.PI.toFloat()).toFloat()
                
                if (colorTransition < 1f) {
                    colorTransition = (colorTransition + 0.05f).coerceAtMost(1f)
                    sphereColor = blendColors(sphereColor, targetColor, colorTransition)
                }
                
                if (kotlin.math.abs(sphereCenterY - targetCenterY) > 1f) {
                    sphereCenterY += (targetCenterY - sphereCenterY) * 0.08f
                }
                
                postInvalidateOnAnimation()
            }
        }
        
        private fun blendColors(from: Int, to: Int, ratio: Float): Int {
            val r = ((Color.red(from) * (1 - ratio) + Color.red(to) * ratio)).toInt()
            val g = ((Color.green(from) * (1 - ratio) + Color.green(to) * ratio)).toInt()
            val b = ((Color.blue(from) * (1 - ratio) + Color.blue(to) * ratio)).toInt()
            return Color.rgb(r, g, b)
        }
        
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0) {
                fixedScreenWidth = w
                fixedScreenHeight = h
            }
            sphereCenterX = w / 2f
            baseCenterY = h * 0.50f
            topCenterY = h * 0.42f
            sphereCenterY = baseCenterY
            targetCenterY = baseCenterY
            

            val localAspectRatio = w.toFloat() / h.toFloat()
            val localIsSquare = localAspectRatio in 0.9f..1.1f
            val localSmallestWidthDp = minOf(w, h) / density
            val localIsLandscape = w > h
            calculatedSphereRatio = when {
                localIsSquare -> 0.25f
                localSmallestWidthDp >= 600f -> 0.35f
                localIsLandscape -> 0.50f
                else -> 0.42f
            }
            android.util.Log.d("EsperSphereView", "onSizeChanged: ${w}x${h}, ratio=$calculatedSphereRatio, landscape=$localIsLandscape, square=$localIsSquare")
            
            sphereDiameter = minOf(w, h) * calculatedSphereRatio
            sphereRadius = sphereDiameter / 2f
            
            val scale = sphereDiameter / dp(200f)
            eyeWidth = dp(10f) * scale
            eyeHeight = dp(30f) * scale
            eyeGap = dp(30f) * scale
            eyeCornerRadius = dp(3f) * scale
        }
        
        fun moveToTop() {
            targetCenterY = topCenterY
        }
        
        fun moveToCenter() {
            targetCenterY = baseCenterY
        }
        
        fun startAnimations() {
            isAnimating = true
            animationStartTime = System.currentTimeMillis()
            mainAnimator.start()
            handler?.postDelayed(blinkRunnable, 1500L)
            invalidate()
        }
        
        fun stopAnimations() {
            isAnimating = false
            blinkAnimator.cancel()
            mainAnimator.cancel()
            handler?.removeCallbacks(blinkRunnable)
        }
        
        fun isAnimating() = isAnimating
        
        override fun onDraw(canvas: Canvas) {
            val cx = sphereCenterX
            val cy = sphereCenterY + floatOffsetY
            val r = sphereRadius * pulseScale
            

            val glowRadius = r * 0.08f
            glowPaint.maskFilter = if (performanceProfile.enableGlowBlur) {
                BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
            } else {
                null
            }
            glowPaint.color = Color.argb((glowAlpha * 255).toInt(), sphereColor shr 16 and 0xFF, sphereColor shr 8 and 0xFF, sphereColor and 0xFF)
            canvas.drawCircle(cx, cy, r * glowScale, glowPaint)
            glowPaint.maskFilter = null
            

            spherePaint.color = sphereColor
            canvas.drawCircle(cx, cy, r, spherePaint)
            

            spherePaint.style = Paint.Style.STROKE
            spherePaint.strokeWidth = r * 0.05f
            spherePaint.color = Color.argb(30, 255, 255, 255)
            canvas.drawCircle(cx, cy, r * 0.95f, spherePaint)
            spherePaint.style = Paint.Style.FILL
            

            drawEyes(canvas, cx, cy, r)
            

            if (showStatusBadge) {
                val fixedBadgeY = sphereCenterY + sphereRadius + dp(40f)
                drawStatusBadge(canvas, sphereCenterX, fixedBadgeY)
            }
        }
        
        private fun drawStatusBadge(canvas: Canvas, cx: Float, badgeY: Float) {
            val textWidth = badgeTextPaint.measureText(statusText)
            val dotRadius = dp(4f)
            val padding = dp(16f)
            val badgeWidth = textWidth + dotRadius * 2 + dp(10f) + padding * 2
            val badgeHeight = dp(32f)
            val badgeLeft = cx - badgeWidth / 2
            val badgeTop = badgeY - badgeHeight / 2
            
            badgePaint.color = Color.argb(20, 255, 255, 255)
            canvas.drawRoundRect(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight, dp(16f), dp(16f), badgePaint)
            
            badgePaint.color = statusDotColor
            val dotCx = badgeLeft + padding + dotRadius
            canvas.drawCircle(dotCx, badgeY, dotRadius, badgePaint)
            
            val textX = dotCx + dotRadius + dp(10f) + textWidth / 2
            canvas.drawText(statusText, textX, badgeY + dp(5f), badgeTextPaint)
        }
        
        fun setStatusBadge(text: String, dotColor: Int, visible: Boolean) {
            statusText = text
            statusDotColor = dotColor
            showStatusBadge = visible
            invalidate()
        }
        
        fun hideStatusBadge() {
            showStatusBadge = false
            invalidate()
        }
        
        private fun drawEyes(canvas: Canvas, cx: Float, cy: Float, r: Float) {

            val sphereDiam = r * 2
            

            val eyeCenterY = (cy - r) + sphereDiam * 0.45f
            


            val leftEyeX = (cx - r) + sphereDiam * eyeLeftPercent
            val rightEyeX = leftEyeX + eyeGap
            
            val currentEyeHeight = eyeHeight * eyeHeightFactor
            
            when (currentExpression) {
                Expression.NEUTRAL -> drawNeutralEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.HAPPY -> drawHappyEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.SAD -> drawSadEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.SURPRISED -> drawSurprisedEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.THINKING -> drawThinkingEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.SLEEPY -> drawSleepyEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.EXCITED -> drawExcitedEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.CONFUSED -> drawConfusedEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.LISTENING -> drawListeningEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.SPEAKING -> drawSpeakingEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.ANGRY -> drawAngryEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.SHY -> drawShyEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.PROUD -> drawProudEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
                Expression.CURIOUS -> drawCuriousEyes(canvas, leftEyeX, rightEyeX, eyeCenterY, currentEyeHeight)
            }
        }
        

        private fun drawNeutralEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val eyeTop = y - h / 2f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, eyeTop, leftX + eyeWidth / 2f, eyeTop + h), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f, eyeTop, rightX + eyeWidth / 2f, eyeTop + h), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawHappyEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            drawNeutralEyes(canvas, leftX, rightX, y, h)
        }
        
        private fun drawSadEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            drawNeutralEyes(canvas, leftX, rightX, y, h)
        }
        
        private fun drawSurprisedEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val r = h * 0.35f
            canvas.drawCircle(leftX, y, r, eyePaint)
            canvas.drawCircle(rightX, y, r, eyePaint)
        }
        
        private fun drawThinkingEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val offsetX = eyeWidth * 0.4f
            val offsetY = -h * 0.1f
            val eyeTop = y - h / 2f + offsetY
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f + offsetX, eyeTop, leftX + eyeWidth / 2f + offsetX, eyeTop + h * 0.75f), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f + offsetX, eyeTop, rightX + eyeWidth / 2f + offsetX, eyeTop + h * 0.75f), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawSleepyEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val sleepyH = h * 0.25f
            val eyeTop = y - sleepyH / 2f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, eyeTop, leftX + eyeWidth / 2f, eyeTop + sleepyH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f, eyeTop, rightX + eyeWidth / 2f, eyeTop + sleepyH), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawExcitedEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val starSize = h * 0.3f
            eyePaint.style = Paint.Style.STROKE
            eyePaint.strokeWidth = eyeWidth * 0.6f
            eyePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(leftX - starSize, y, leftX + starSize, y, eyePaint)
            canvas.drawLine(leftX, y - starSize, leftX, y + starSize, eyePaint)
            canvas.drawLine(rightX - starSize, y, rightX + starSize, y, eyePaint)
            canvas.drawLine(rightX, y - starSize, rightX, y + starSize, eyePaint)
            eyePaint.style = Paint.Style.FILL
        }
        
        private fun drawConfusedEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val leftH = h * 0.55f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, y - leftH / 2f, leftX + eyeWidth / 2f, y + leftH / 2f), eyeCornerRadius, eyeCornerRadius, eyePaint)
            val rightH = h * 1.1f
            canvas.drawRoundRect(RectF(rightX - eyeWidth * 0.6f, y - rightH / 2f, rightX + eyeWidth * 0.6f, y + rightH / 2f), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawListeningEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val listenH = h * 0.8f
            val eyeTop = y - listenH / 2f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, eyeTop, leftX + eyeWidth / 2f, eyeTop + listenH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f, eyeTop, rightX + eyeWidth / 2f, eyeTop + listenH), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawSpeakingEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val pulse = 0.9f + 0.1f * kotlin.math.sin(System.currentTimeMillis() / 180.0).toFloat()
            val speakH = h * pulse
            val eyeTop = y - speakH / 2f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, eyeTop, leftX + eyeWidth / 2f, eyeTop + speakH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f, eyeTop, rightX + eyeWidth / 2f, eyeTop + speakH), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawAngryEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val angryH = h * 0.6f
            val eyeTop = y - angryH / 2f
            canvas.save()
            canvas.rotate(-15f, leftX, y)
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, eyeTop, leftX + eyeWidth / 2f, eyeTop + angryH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.restore()
            canvas.save()
            canvas.rotate(15f, rightX, y)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f, eyeTop, rightX + eyeWidth / 2f, eyeTop + angryH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.restore()
        }
        
        private fun drawShyEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val shyH = h * 0.7f
            val offsetX = eyeWidth * 0.3f
            val offsetY = h * 0.15f
            val eyeTop = y - shyH / 2f + offsetY
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f - offsetX, eyeTop, leftX + eyeWidth / 2f - offsetX, eyeTop + shyH), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth / 2f - offsetX, eyeTop, rightX + eyeWidth / 2f - offsetX, eyeTop + shyH), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        private fun drawProudEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val proudH = h * 0.4f
            val offsetY = -h * 0.1f
            val eyeTop = y - proudH / 2f + offsetY
            canvas.drawRoundRect(RectF(leftX - eyeWidth * 0.6f, eyeTop, leftX + eyeWidth * 0.6f, eyeTop + proudH), eyeCornerRadius * 2, eyeCornerRadius * 2, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth * 0.6f, eyeTop, rightX + eyeWidth * 0.6f, eyeTop + proudH), eyeCornerRadius * 2, eyeCornerRadius * 2, eyePaint)
        }
        
        private fun drawCuriousEyes(canvas: Canvas, leftX: Float, rightX: Float, y: Float, h: Float) {
            val leftH = h * 0.7f
            val rightH = h * 1.0f
            canvas.drawRoundRect(RectF(leftX - eyeWidth / 2f, y - leftH / 2f, leftX + eyeWidth / 2f, y + leftH / 2f), eyeCornerRadius, eyeCornerRadius, eyePaint)
            canvas.drawRoundRect(RectF(rightX - eyeWidth * 0.7f, y - rightH / 2f, rightX + eyeWidth * 0.7f, y + rightH / 2f), eyeCornerRadius, eyeCornerRadius, eyePaint)
        }
        
        fun setExpression(expression: Expression) {
            if (currentExpression != expression) {
                currentExpression = expression
                targetColor = expressionColors[expression] ?: 0xFF6366F1.toInt()
                colorTransition = 0f
                blinkInterval = if (expression == Expression.SPEAKING) 1500L else 4000L
                invalidate()
            }
        }
        
        fun destroy() {
            stopAnimations()
            blinkAnimator.removeAllUpdateListeners()
            mainAnimator.removeAllUpdateListeners()
        }
    }

    private var windowManager: WindowManager? = null
    private var capsuleView: FrameLayout? = null
    private var capsuleParams: WindowManager.LayoutParams? = null
    private var capsuleTextView: TextView? = null
    private var capsuleScrollView: ScrollView? = null
    private var esperSphereView: EsperSphereView? = null
    private var textCardView: FrameLayout? = null
    
    
    private var cursorBlinkRunnable: Runnable? = null
    private var isCursorVisible = false
    private var currentState = State.IDLE
    private var displayMode = DisplayMode.NONE
    private var performanceProfile = PerformanceProfile.balanced()
    private var lastStreamingUiUpdateMs = 0L
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No overlay permission, stopping FloatingWindowService")
            stopSelf()
            return
        }
        performanceProfile = detectPerformanceProfile()
        Log.d(
            TAG,
            "PerformanceProfile: blur=${performanceProfile.enableGlowBlur}, smoothScroll=${performanceProfile.useSmoothScroll}, throttle=${performanceProfile.streamingUiThrottleMs}ms"
        )
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createCapsuleView()
    }

    private fun detectPerformanceProfile(): PerformanceProfile {
        val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return PerformanceProfile.balanced()
        val isLowRam = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) am.isLowRamDevice else false
        val weakDevice = isLowRam || am.memoryClass <= 192
        return if (weakDevice) PerformanceProfile.lowPower() else PerformanceProfile.balanced()
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    private fun dpF(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createCapsuleView() {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        

        val density = resources.displayMetrics.density
        val smallestWidthDp = minOf(screenWidth, screenHeight) / density
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val isLandscape = screenWidth > screenHeight
        val isSquareScreen = aspectRatio in 0.9f..1.1f
        val isTablet = smallestWidthDp >= 600f
        
        val sphereRatio = when {
            isSquareScreen -> 0.25f
            isTablet -> 0.35f
            isLandscape -> 0.50f
            else -> 0.42f
        }
        
        val gradientAlphaBottom = when {
            isSquareScreen -> 200
            isTablet -> 180
            isLandscape -> 180
            else -> 220
        }
        val gradientAlphaMiddle = when {
            isSquareScreen -> 20
            isTablet -> 15
            isLandscape -> 10
            else -> 60
        }
        
        android.util.Log.d(TAG, "Screen: ${screenWidth}x${screenHeight}, density=$density, smallestWidthDp=$smallestWidthDp, aspectRatio=$aspectRatio")
        android.util.Log.d(TAG, "Layout: landscape=$isLandscape, tablet=$isTablet, square=$isSquareScreen, sphereRatio=$sphereRatio, gradientBottom=$gradientAlphaBottom, gradientMiddle=$gradientAlphaMiddle")
        
        val fullscreenContainer = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(
                Color.argb(gradientAlphaBottom, 0, 0, 0),
                Color.argb(gradientAlphaMiddle, 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            ))
            fitsSystemWindows = false
        }
        

        val sphereView = EsperSphereView(this, screenWidth, screenHeight).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }
        esperSphereView = sphereView
        fullscreenContainer.addView(sphereView)
        
        val cardGradientBottom = when {
            isSquareScreen -> 200
            isTablet -> 180
            isLandscape -> 180
            else -> 220
        }
        val cardGradientMiddle = when {
            isSquareScreen -> 20
            isTablet -> 15
            isLandscape -> 10
            else -> 60
        }
        
        val cardFixedHeight = when {
            isSquareScreen -> (screenHeight * 0.22f).toInt()
            isTablet -> (screenHeight * 0.18f).toInt()
            isLandscape -> (screenHeight * 0.25f).toInt()
            else -> (screenHeight * 0.28f).toInt()
        }
        
        val cardBottomMargin = when {
            isSquareScreen -> dp(12f)
            isTablet -> dp(16f)
            isLandscape -> dp(20f)
            else -> dp(16f)
        }
        val cardBottomPad = dp(8f)
        
        val textCardContainer = FrameLayout(this).apply {
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                cardFixedHeight + cardBottomMargin
            )
            lp.gravity = Gravity.BOTTOM
            layoutParams = lp
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(
                Color.argb(cardGradientBottom, 8, 8, 12),
                Color.argb(cardGradientMiddle, 8, 8, 12),
                Color.argb(0, 8, 8, 12)
            ))
            setPadding(dp(20f), dp(4f), dp(20f), cardBottomPad + cardBottomMargin)
            visibility = View.GONE
            alpha = 0f
        }
        textCardView = textCardContainer
        
        val cardInnerContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
        }
        

        val scrollViewHeight = cardFixedHeight - dp(14f)
        
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 0f
            }
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        capsuleScrollView = scrollView
        
        val baseFontSize = minOf(screenWidth, screenHeight) / 20f
        capsuleTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
            setTextColor(Color.argb(230, 255, 255, 255))
            textSize = baseFontSize.coerceIn(18f, 24f)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            text = ""
            gravity = Gravity.START or Gravity.TOP
            isSingleLine = false
            setLineSpacing(0f, 1.2f)
            setShadowLayer(4f, 0f, 1f, Color.argb(80, 0, 0, 0))
        }
        scrollView.addView(capsuleTextView)
        cardInnerContainer.addView(scrollView)
        textCardContainer.addView(cardInnerContainer)
        fullscreenContainer.addView(textCardContainer)
        
        capsuleView = fullscreenContainer
        fullscreenContainer.alpha = 0f
        fullscreenContainer.visibility = View.GONE

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        @Suppress("DEPRECATION")
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        capsuleParams = params

        try {
            windowManager?.addView(fullscreenContainer, params)
            fullscreenContainer.visibility = View.GONE

            @Suppress("DEPRECATION")
            fullscreenContainer.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capsule view", e)
        }
    }

    private var needsBringToFront = true
    
    private fun bringToFrontLazy() {
        val view = capsuleView ?: return
        val params = capsuleParams ?: return
        if (!needsBringToFront) return
        needsBringToFront = false
        
        try {

            windowManager?.updateViewLayout(view, params)
            return
        } catch (_: Exception) {
        }

        try {
            val wasVisible = view.visibility == View.VISIBLE
            val wasAnimating = esperSphereView?.isAnimating() ?: false
            if (wasAnimating) esperSphereView?.stopAnimations()
            
            windowManager?.removeView(view)
            windowManager?.addView(view, params)
            
            if (wasVisible) {
                view.visibility = View.VISIBLE
                view.alpha = 1f
            }
            if (wasAnimating) {
                view.post { esperSphereView?.startAnimations() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bring to front", e)
        }
    }
    
    private fun showSphereOnly() {
        bringToFrontLazy()
        if (capsuleView?.visibility != View.VISIBLE) {
            capsuleView?.visibility = View.VISIBLE
            capsuleView?.alpha = 0f
            capsuleView?.animate()?.cancel()
            capsuleView?.animate()
                ?.alpha(1f)
                ?.setDuration(400)
                ?.setInterpolator(DecelerateInterpolator(2f))
                ?.start()
            capsuleView?.post { esperSphereView?.startAnimations() }
        }
        
        esperSphereView?.moveToCenter()
        
        if (textCardView?.visibility == View.VISIBLE) {
            textCardView?.animate()?.cancel()
            textCardView?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.withEndAction { textCardView?.visibility = View.GONE }
                ?.start()
        }
        
    }
    
    private fun showCapsuleWithCard() {
        bringToFrontLazy()
        if (capsuleView?.visibility != View.VISIBLE) {
            capsuleView?.visibility = View.VISIBLE
            capsuleView?.alpha = 0f
            capsuleView?.animate()?.cancel()
            capsuleView?.animate()
                ?.alpha(1f)
                ?.setDuration(400)
                ?.setInterpolator(DecelerateInterpolator(2f))
                ?.start()
            capsuleView?.post { esperSphereView?.startAnimations() }
        }
        
        esperSphereView?.moveToTop()
        esperSphereView?.hideStatusBadge()
        
        textCardView?.visibility = View.VISIBLE
        textCardView?.alpha = 0f
        textCardView?.animate()?.cancel()
        textCardView?.animate()
            ?.alpha(1f)
            ?.setDuration(400)
            ?.setStartDelay(150)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()
    }
    
    
    private fun hideCapsule(onEnd: (() -> Unit)? = null) {
        esperSphereView?.stopAnimations()
        capsuleView?.animate()?.cancel()
        capsuleView?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction {
                capsuleView?.visibility = View.GONE
                onEnd?.invoke()
            }?.start()
    }

    
    
    private fun enterListeningMode() {
        currentState = State.LISTENING
        displayMode = DisplayMode.NONE
        lastStreamingUiUpdateMs = 0L
        currentText = ""
        streamingBuffer.clear()
        esperSphereView?.setStatusBadge("LISTENING", Color.parseColor("#00FFAA"), true)
        esperSphereView?.setExpression(Expression.LISTENING)
        showSphereOnly()
    }

    private fun enterProcessingMode() {
        currentState = State.PROCESSING
        displayMode = DisplayMode.NONE
        lastStreamingUiUpdateMs = 0L
        currentText = ""
        esperSphereView?.setStatusBadge("PROCESSING", Color.parseColor("#FBBF24"), true)
        esperSphereView?.setExpression(Expression.THINKING)
        showSphereOnly()
    }

    private fun enterSpeakingMode() {
        currentState = State.SPEAKING
        currentText = ""
        capsuleTextView?.text = ""
        capsuleScrollView?.scrollTo(0, 0)
        esperSphereView?.setExpression(Expression.SPEAKING)
        showCapsuleWithCard()
    }

    private fun hideAll(onEnd: (() -> Unit)? = null) {
        stopCursorBlink()
        currentState = State.IDLE
        displayMode = DisplayMode.NONE
        needsBringToFront = true
        if (capsuleView?.visibility == View.VISIBLE) {
            hideCapsule { onEnd?.invoke() }
        } else {
            onEnd?.invoke()
        }
    }

    private var scrollRunnable: Runnable? = null
    
    private var lastScrollTarget = 0
    
    private fun scrollToFollow() {
        val sv = capsuleScrollView ?: return
        val tv = capsuleTextView ?: return
        
        tv.post {
            val textHeight = tv.height
            val scrollViewHeight = sv.height
            if (textHeight > scrollViewHeight) {
                val targetScroll = textHeight - scrollViewHeight
                if (performanceProfile.useSmoothScroll) {
                    sv.smoothScrollTo(0, targetScroll)
                } else {
                    sv.scrollTo(0, targetScroll)
                }
            }
        }
    }
    
    private fun checkAndScrollIfNeeded() {
        val sv = capsuleScrollView ?: return
        val tv = capsuleTextView ?: return
        
        tv.post {
            val textHeight = tv.height
            val scrollViewHeight = sv.height
            if (textHeight > scrollViewHeight) {
                val targetScroll = textHeight - scrollViewHeight
                val lineHeight = tv.lineHeight
                if (targetScroll - lastScrollTarget >= lineHeight) {
                    lastScrollTarget = targetScroll
                    sv.smoothScrollTo(0, targetScroll)
                }
            }
        }
    }

    private fun updateDisplayText() {
        capsuleTextView?.text = if (isCursorVisible) currentText + CURSOR_CHAR else currentText
        scrollToFollow()
    }

    private fun startCursorBlink() {
        isCursorVisible = true
        updateDisplayText()
        cursorBlinkRunnable?.let { handler.removeCallbacks(it) }
        cursorBlinkRunnable = object : Runnable {
            override fun run() {
                isCursorVisible = !isCursorVisible
                updateDisplayText()
                handler.postDelayed(this, 530)
            }
        }
        handler.postDelayed(cursorBlinkRunnable!!, 530)
    }

    private fun stopCursorBlink() {
        cursorBlinkRunnable?.let { handler.removeCallbacks(it) }
        cursorBlinkRunnable = null
        isCursorVisible = false
        capsuleTextView?.text = currentText
        scrollToFollow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_LISTENING -> {
                handler.post {
                    karaokeRunnable?.let { handler.removeCallbacks(it) }
                    autoHideRunnable?.let { handler.removeCallbacks(it) }
                    stopCursorBlink()
                    enterListeningMode()
                }
            }
            ACTION_SHOW_PROCESSING -> {
                handler.post {
                    karaokeRunnable?.let { handler.removeCallbacks(it) }
                    autoHideRunnable?.let { handler.removeCallbacks(it) }
                    stopCursorBlink()
                    enterProcessingMode()
                }
            }
            ACTION_SHOW_ASSISTANT_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                showAssistantTextKaraoke(text)
            }
            ACTION_APPEND_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                appendText(text)
            }
            ACTION_FINISH_STREAMING -> {
                handler.post {
                    stopCursorBlink()
                    if (displayMode != DisplayMode.KARAOKE) {
                        scheduleAutoHide()
                    }
                }
            }
            ACTION_SHOW_KARAOKE -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                val durationMs = intent.getLongExtra(EXTRA_DURATION, 0L)
                initKaraokeWithDuration(text, durationMs)
            }
            ACTION_UPDATE_KARAOKE_PROGRESS -> {
                val currentMs = intent.getLongExtra(EXTRA_CURRENT_MS, 0L)
                val totalMs = intent.getLongExtra(EXTRA_TOTAL_MS, 0L)
                updateKaraokeProgress(currentMs, totalMs)
            }
            ACTION_HIDE -> hideOverlay()
            ACTION_CLEAR -> clearText()
        }
    }

    private var currentText = ""
    private var karaokeRunnable: Runnable? = null
    private var autoHideRunnable: Runnable? = null
    
    private var karaokeSentences = listOf<String>()
    private var karaokeCurrentSentenceIndex = 0
    private var karaokeSentenceLengths = listOf<Int>()
    private var karaokeTotalLength = 0
    
    private fun showAssistantTextKaraoke(text: String) {
        handler.post {
            karaokeRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            displayMode = DisplayMode.STATIC_TEXT
            streamingBuffer.clear()

            if (currentState != State.SPEAKING) {
                enterSpeakingMode()
            }
            
            karaokeSentences = splitIntoSentences(text)
            karaokeCurrentSentenceIndex = 0
            
            val detectedExpression = EmotionKeywordDetector.detectExpression(text)
            esperSphereView?.setExpression(detectedExpression)
            
            if (karaokeSentences.isNotEmpty()) {
                currentText = karaokeSentences[0]
                capsuleTextView?.text = currentText
            }
            
            if (capsuleView?.visibility != View.VISIBLE) showCapsuleWithCard()
        }
    }
    
    private fun splitIntoSentences(text: String): List<String> {
        val pattern = Regex("(?<=[。！？.!?])")
        val sentences = text.split(pattern).filter { it.isNotBlank() }.map { it.trim() }
        return sentences.ifEmpty { listOf(text) }
    }
    
    private fun calculateSpeechWeight(text: String): Int {

        var weight = 0
        for (char in text) {
            when {
                char.code in 0x4E00..0x9FFF -> weight += 1
                char.code in 0x3040..0x30FF -> weight += 1
                char.code in 0xAC00..0xD7AF -> weight += 1
                char.isLetter() -> weight += 2
                char.isDigit() -> weight += 1
            }
        }
        return weight.coerceAtLeast(1)
    }
    
    private fun initKaraokeWithDuration(text: String, durationMs: Long) {
        handler.post {
            karaokeRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            displayMode = DisplayMode.KARAOKE
            streamingBuffer.clear()

            if (currentState != State.SPEAKING) {
                enterSpeakingMode()
            }
            
            val rawSentences = splitIntoSentences(text)
            karaokeSentences = rawSentences.map { cleanSentence(it) }.filter { it.isNotBlank() }
            karaokeCurrentSentenceIndex = 0
            
            karaokeSentenceLengths = karaokeSentences.map { calculateSpeechWeight(it) }
            karaokeTotalLength = karaokeSentenceLengths.sum()
            
            android.util.Log.w("KaraokeDebug", "=== KARAOKE INIT ===")
            karaokeSentences.forEachIndexed { index, sentence ->
                val weight = karaokeSentenceLengths[index]
                val ratio = weight.toDouble() / karaokeTotalLength * 100
                android.util.Log.w("KaraokeDebug", "sentence[$index]: weight=$weight (${ratio.toInt()}%), text='${sentence.take(20)}...'")
            }
            android.util.Log.w("KaraokeDebug", "Total: ${karaokeSentences.size} sentences, totalWeight=$karaokeTotalLength")
            
            if (karaokeSentences.isEmpty()) return@post
            
            val detectedExpression = EmotionKeywordDetector.detectExpression(text)
            esperSphereView?.setExpression(detectedExpression)
            
            currentText = karaokeSentences[0]
            capsuleTextView?.text = currentText
            
            if (capsuleView?.visibility != View.VISIBLE) showCapsuleWithCard()
        }
    }
    
    private var lastLoggedProgress = -1L
    
    private fun updateKaraokeProgress(currentMs: Long, totalMs: Long) {
        if (displayMode != DisplayMode.KARAOKE) return
        if (karaokeSentences.isEmpty()) {
            android.util.Log.w("KaraokeDebug", "updateKaraokeProgress: sentences is EMPTY!")
            return
        }
        if (totalMs <= 0 || karaokeTotalLength <= 0) {
            android.util.Log.w("KaraokeDebug", "updateKaraokeProgress: totalMs=$totalMs, totalLength=$karaokeTotalLength")
            return
        }
        
        handler.post {
            if (currentMs / 1000 != lastLoggedProgress) {
                lastLoggedProgress = currentMs / 1000
                val progressRatio = currentMs.toDouble() / totalMs
                android.util.Log.w("KaraokeDebug", "Progress: ${currentMs}ms/${totalMs}ms (${(progressRatio*100).toInt()}%), sentences=${karaokeSentences.size}, current=$karaokeCurrentSentenceIndex")
            }
            
            val progressRatio = currentMs.toDouble() / totalMs
            
            var accumulatedWeight = 0
            var targetIndex = 0
            
            for ((index, weight) in karaokeSentenceLengths.withIndex()) {
                accumulatedWeight += weight
                val sentenceEndRatio = accumulatedWeight.toDouble() / karaokeTotalLength
                if (progressRatio < sentenceEndRatio) {
                    targetIndex = index
                    break
                }
                targetIndex = index + 1
            }
            
            targetIndex = targetIndex.coerceIn(0, karaokeSentences.size - 1)
            
            if (targetIndex != karaokeCurrentSentenceIndex) {
                karaokeCurrentSentenceIndex = targetIndex
                currentText = karaokeSentences[targetIndex]
                capsuleTextView?.text = currentText
                android.util.Log.d(TAG, "Karaoke: progress=${currentMs}ms/${totalMs}ms (${(progressRatio * 100).toInt()}%), sentence[$targetIndex]='${currentText.take(10)}...'")
            }
        }
    }
    
    
    private fun cleanSentence(sentence: String): String {
        return sentence.trim().replace(Regex("[。！？.!?，,、；;：:]+$")) { match ->
            val first = match.value.firstOrNull() ?: ""
            if (first in listOf('。', '！', '？', '.', '!', '?')) first.toString() else ""
        }
    }

    private fun scheduleAutoHide() {
        autoHideRunnable?.let { handler.removeCallbacks(it) }
        autoHideRunnable = Runnable {
            if (displayMode == DisplayMode.KARAOKE) return@Runnable
            hideAll {
                currentText = ""
                capsuleTextView?.text = ""
            }
        }
        handler.postDelayed(autoHideRunnable!!, 6000)
    }

    private var streamingBuffer = StringBuilder()
    
    private fun appendText(text: String) {
        handler.post {
            if (displayMode == DisplayMode.KARAOKE) return@post
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            displayMode = DisplayMode.STREAMING
            
            if (currentState != State.SPEAKING) {
                enterSpeakingMode()
            }
            
            streamingBuffer.append(text)
            val fullText = streamingBuffer.toString()

            val now = System.currentTimeMillis()
            val forceUiUpdate = text.any { it == '。' || it == '！' || it == '？' || it == '.' || it == '!' || it == '?' || it == '\n' }
            if (!forceUiUpdate && performanceProfile.streamingUiThrottleMs > 0L) {
                if (now - lastStreamingUiUpdateMs < performanceProfile.streamingUiThrottleMs) {
                    return@post
                }
            }

            currentText = fullText
            capsuleTextView?.text = currentText
            lastStreamingUiUpdateMs = now
            
            val detectedExpression = EmotionKeywordDetector.detectExpression(fullText)
            esperSphereView?.setExpression(detectedExpression)
            
            if (capsuleView?.visibility != View.VISIBLE) showCapsuleWithCard()
        }
    }

    private fun hideOverlay() {
        handler.post {
            stopCursorBlink()
            karaokeRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            scrollRunnable?.let { handler.removeCallbacks(it) }
            displayMode = DisplayMode.NONE
            lastStreamingUiUpdateMs = 0L
            streamingBuffer.clear()
            karaokeSentences = emptyList()
            karaokeCurrentSentenceIndex = 0
            hideAll {
                currentText = ""
                capsuleTextView?.text = ""
            }
        }
    }

    private fun clearText() {
        handler.post {
            stopCursorBlink()
            karaokeRunnable?.let { handler.removeCallbacks(it) }
            autoHideRunnable?.let { handler.removeCallbacks(it) }
            displayMode = DisplayMode.NONE
            lastStreamingUiUpdateMs = 0L
            streamingBuffer.clear()
            currentText = ""
            capsuleTextView?.text = ""
            hideAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esperSphereView?.destroy()
        handler.removeCallbacksAndMessages(null)
        try {
            capsuleView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove floating window", e)
        }
    }

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val CURSOR_CHAR = "\u2758"
        
        const val ACTION_SHOW_LISTENING = "com.example.ava.SHOW_LISTENING"
        const val ACTION_SHOW_PROCESSING = "com.example.ava.SHOW_PROCESSING"
        const val ACTION_SHOW_ASSISTANT_TEXT = "com.example.ava.SHOW_ASSISTANT_TEXT"
        const val ACTION_APPEND_TEXT = "com.example.ava.APPEND_TEXT"
        const val ACTION_FINISH_STREAMING = "com.example.ava.FINISH_STREAMING"
        const val ACTION_SHOW_KARAOKE = "com.example.ava.SHOW_KARAOKE"
        const val ACTION_UPDATE_KARAOKE_PROGRESS = "com.example.ava.UPDATE_KARAOKE_PROGRESS"
        const val ACTION_HIDE = "com.example.ava.HIDE_FLOATING"
        const val ACTION_CLEAR = "com.example.ava.CLEAR_FLOATING"
        const val EXTRA_TEXT = "text"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_CURRENT_MS = "current_ms"
        const val EXTRA_TOTAL_MS = "total_ms"

        private fun hasOverlayPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)
        }

        fun showListening(context: Context) {
            if (!hasOverlayPermission(context)) return
            context.startService(Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_LISTENING
            })
        }

        fun showProcessing(context: Context) {
            if (!hasOverlayPermission(context)) return
            context.startService(Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_PROCESSING
            })
        }

        fun showAssistantText(context: Context, text: String) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_ASSISTANT_TEXT
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }

        fun appendText(context: Context, text: String) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_APPEND_TEXT
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }

        fun finishStreaming(context: Context) {
            if (!hasOverlayPermission(context)) return
            context.startService(Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_FINISH_STREAMING
            })
        }

        fun showKaraokeText(context: Context, text: String, durationMs: Long) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_KARAOKE
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_DURATION, durationMs)
            }
            context.startService(intent)
        }
        
        fun updateKaraokeProgress(context: Context, currentMs: Long, totalMs: Long) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_UPDATE_KARAOKE_PROGRESS
                putExtra(EXTRA_CURRENT_MS, currentMs)
                putExtra(EXTRA_TOTAL_MS, totalMs)
            }
            context.startService(intent)
        }
        

        fun hide(context: Context) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun clear(context: Context) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_CLEAR
            }
            context.startService(intent)
        }
    }
}
