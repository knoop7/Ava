package com.example.ava.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.annotation.RequiresApi

class WakeRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private var time: Float = 0f
    private var origin: PointF = PointF()
    private var maxRadius: Float = 0f
    private var rippleColor: Int = Color.parseColor("#00FF88")
    
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var timeAnimator: ValueAnimator? = null
    
    private var rippleShader: Any? = null
    private var useShader = false
    private var pendingStart: (() -> Unit)? = null
    private var waitingForLayout = false
    private val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (width <= 0 || height <= 0) return
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
            waitingForLayout = false
            pendingStart?.also { action ->
                pendingStart = null
                action()
            }
        }
    }
    
    companion object {
        private const val RIPPLE_DURATION = 2000L
        
        private const val SHADER_CODE = """
            uniform vec2 in_origin;
            uniform float in_progress;
            uniform float in_maxRadius;
            uniform float in_time;
            uniform float in_distort_radial;
            uniform float in_distort_xy;
            uniform float in_radius;
            uniform float in_fadeSparkle;
            uniform float in_fadeCircle;
            uniform float in_fadeRing;
            uniform float in_blur;
            uniform float in_pixelDensity;
            uniform vec4 in_color;
            uniform float in_sparkle_strength;
            
            float triangleNoise(vec2 n) {
                n = fract(n * vec2(5.3987, 5.4421));
                n += dot(n.yx, n.xy + vec2(21.5351, 14.3137));
                float xy = n.x * n.y;
                return fract(xy * 95.4307) + fract(xy * 75.04961) - 1.0;
            }
            
            const float PI = 3.1415926535897932384626;
            
            float sparkles(vec2 uv, float t) {
                float n = triangleNoise(uv);
                float s = 0.0;
                for (float i = 0.0; i < 4.0; i += 1.0) {
                    float l = i * 0.01;
                    float h = l + 0.1;
                    float o = smoothstep(n - l, h, n);
                    o *= abs(sin(PI * o * (t + 0.55 * i)));
                    s += o;
                }
                return s;
            }
            
            float softCircle(vec2 uv, vec2 xy, float radius, float blur) {
                float blurHalf = blur * 0.5;
                float d = distance(uv, xy);
                return 1.0 - smoothstep(1.0 - blurHalf, 1.0 + blurHalf, d / radius);
            }
            
            float softRing(vec2 uv, vec2 xy, float radius, float blur) {
                float thickness_half = radius * 0.25;
                float circle_outer = softCircle(uv, xy, radius + thickness_half, blur);
                float circle_inner = softCircle(uv, xy, radius - thickness_half, blur);
                return circle_outer - circle_inner;
            }
            
            vec2 distort(vec2 p, vec2 origin, float time, float distort_amount_radial, float distort_amount_xy) {
                vec2 distance = origin - p;
                float angle = atan(distance.y, distance.x);
                return p + vec2(sin(angle * 8.0 + time * 0.003 + 1.641),
                                cos(angle * 5.0 + 2.14 + time * 0.00412)) * distort_amount_radial
                         + vec2(sin(p.x * 0.01 + time * 0.00215 + 0.8123),
                                cos(p.y * 0.01 + time * 0.005931)) * distort_amount_xy;
            }
            
            vec4 main(vec2 p) {
                vec2 p_distorted = distort(p, in_origin, in_time, in_distort_radial, in_distort_xy);
                
                float sparkleRing = softRing(p_distorted, in_origin, in_radius, in_blur);
                float sparkle = sparkles(p - mod(p, in_pixelDensity * 0.8), in_time * 0.00175)
                    * sparkleRing * in_fadeSparkle;
                float circle = softCircle(p_distorted, in_origin, in_radius * 1.2, in_blur);
                float rippleAlpha = max(circle * in_fadeCircle,
                    softRing(p_distorted, in_origin, in_radius, in_blur) * in_fadeRing) * 0.45;
                vec4 ripple = in_color * rippleAlpha;
                vec4 white = vec4(1.0, 1.0, 1.0, sparkle * in_sparkle_strength);
                return ripple + white;
            }
        """
    }
    
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                initShaderApi33()
            } catch (e: Exception) {
                useShader = false
            }
        }
        ripplePaint.style = Paint.Style.FILL
    }
    
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initShaderApi33() {
        rippleShader = android.graphics.RuntimeShader(SHADER_CODE)
        useShader = true
    }
    
    fun setColor(color: Int) {
        rippleColor = color
    }
    
    fun startRipple() {
        runWhenSized {
            origin.set(width / 2f, height / 2f)
            maxRadius = maxOf(width, height) * 0.8f
            
            animator?.cancel()
            timeAnimator?.cancel()
            
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = RIPPLE_DURATION
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener { animation ->
                    progress = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
            
            timeAnimator = ValueAnimator.ofFloat(0f, RIPPLE_DURATION.toFloat()).apply {
                duration = RIPPLE_DURATION
                repeatCount = 0
                addUpdateListener { animation ->
                    time = animation.animatedValue as Float
                }
                start()
            }
        }
    }
    
    fun startRippleAt(x: Float, y: Float) {
        runWhenSized {
            origin.set(x, y)
            maxRadius = maxOf(width, height) * 0.8f
            
            animator?.cancel()
            timeAnimator?.cancel()
            
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = RIPPLE_DURATION
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener { animation ->
                    progress = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
            
            timeAnimator = ValueAnimator.ofFloat(0f, RIPPLE_DURATION.toFloat()).apply {
                duration = RIPPLE_DURATION
                repeatCount = 0
                addUpdateListener { animation ->
                    time = animation.animatedValue as Float
                }
                start()
            }
        }
    }
    
    private val currentRadius: Float
        get() {
            val eased = 1 - (1 - progress).let { it * it * it }
            return eased * maxRadius
        }
    
    private val fadeSparkle: Float
        get() = if (progress < 0.5f) 1f else 1f - (progress - 0.5f) * 2f
    
    private val fadeCircle: Float
        get() = when {
            progress < 0.3f -> progress / 0.3f
            progress < 0.7f -> 1f
            else -> 1f - (progress - 0.7f) / 0.3f
        }
    
    private val fadeRing: Float
        get() = if (progress < 0.2f) progress / 0.2f else 1f - (progress - 0.2f) / 0.8f
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateShader() {
        (rippleShader as? android.graphics.RuntimeShader)?.let { shader ->
            shader.setFloatUniform("in_origin", origin.x, origin.y)
            shader.setFloatUniform("in_progress", progress)
            shader.setFloatUniform("in_maxRadius", maxRadius)
            shader.setFloatUniform("in_time", time)
            shader.setFloatUniform("in_distort_radial", 15f)
            shader.setFloatUniform("in_distort_xy", 5f)
            shader.setFloatUniform("in_radius", currentRadius)
            shader.setFloatUniform("in_fadeSparkle", fadeSparkle)
            shader.setFloatUniform("in_fadeCircle", fadeCircle)
            shader.setFloatUniform("in_fadeRing", fadeRing)
            shader.setFloatUniform("in_blur", 1.2f)
            shader.setFloatUniform("in_pixelDensity", resources.displayMetrics.density)
            shader.setFloatUniform("in_sparkle_strength", 0.5f)
            
            val r = Color.red(rippleColor) / 255f
            val g = Color.green(rippleColor) / 255f
            val b = Color.blue(rippleColor) / 255f
            shader.setFloatUniform("in_color", r, g, b, 1f)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (progress <= 0f || progress >= 1f) return
        
        if (useShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawWithShader(canvas)
        } else {
            drawFallbackRipple(canvas)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun drawWithShader(canvas: Canvas) {
        updateShader()
        (rippleShader as? android.graphics.RuntimeShader)?.let { shader ->
            ripplePaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ripplePaint)
        }
    }
    
    private fun drawFallbackRipple(canvas: Canvas) {
        val radius = currentRadius
        if (radius <= 0) return
        
        val alpha = (fadeCircle * 0.45f * 255).toInt().coerceIn(0, 255)
        ripplePaint.shader = null
        ripplePaint.color = Color.argb(alpha, Color.red(rippleColor), Color.green(rippleColor), Color.blue(rippleColor))
        ripplePaint.style = Paint.Style.FILL
        canvas.drawCircle(origin.x, origin.y, radius * 1.2f, ripplePaint)
        
        val ringAlpha = (fadeRing * 0.45f * 255).toInt().coerceIn(0, 255)
        ripplePaint.color = Color.argb(ringAlpha, Color.red(rippleColor), Color.green(rippleColor), Color.blue(rippleColor))
        ripplePaint.style = Paint.Style.STROKE
        ripplePaint.strokeWidth = radius * 0.5f
        canvas.drawCircle(origin.x, origin.y, radius, ripplePaint)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        timeAnimator?.cancel()
        if (waitingForLayout && viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        waitingForLayout = false
        pendingStart = null
    }

    private fun runWhenSized(action: () -> Unit) {
        if (width > 0 && height > 0) {
            action()
            return
        }
        pendingStart = action
        if (!waitingForLayout) {
            waitingForLayout = true
            viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        }
    }
}
