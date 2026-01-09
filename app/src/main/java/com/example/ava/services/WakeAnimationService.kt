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
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout


class WakeAnimationService : Service() {

    private var windowManager: WindowManager? = null
    private var containerView: FrameLayout? = null
    private var webView: WebView? = null
    private var isShowing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_SHOW" -> showAnimation()
            "ACTION_HIDE" -> hideAnimation()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showAnimation() {
        if (isShowing) return

        try {
            webView = WebView(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, WAKE_ANIMATION_HTML, "text/html", "UTF-8", null)
            }

            containerView = FrameLayout(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                addView(webView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }

            val layoutParams = WindowManager.LayoutParams().apply {
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
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
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

            windowManager?.addView(containerView, layoutParams)
            isShowing = true
            Log.d(TAG, "Wake animation shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show wake animation", e)
        }
    }

    private fun hideAnimation() {
        if (!isShowing) return

        try {
            containerView?.let { container ->
                windowManager?.removeView(container)
                webView?.let { wv ->
                    container.removeView(wv)
                    wv.destroy()
                }
            }
            containerView = null
            webView = null
            isShowing = false
            Log.d(TAG, "Wake animation hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide wake animation", e)
        }
    }

    override fun onDestroy() {
        hideAnimation()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WakeAnimationService"

        fun show(context: Context) {
            val intent = Intent(context, WakeAnimationService::class.java).apply {
                action = "ACTION_SHOW"
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, WakeAnimationService::class.java).apply {
                action = "ACTION_HIDE"
            }
            context.startService(intent)
        }

        private const val WAKE_ANIMATION_HTML = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <style>
        body, html {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background-color: transparent;
        }
        #canvas-silk {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            filter: blur(100px) contrast(1.2);
            mix-blend-mode: screen;
            opacity: 0.8;
            transition: opacity 1s ease;
        }
        .film-grain {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none;
            opacity: 0.06;
            z-index: 10;
            background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
        }
        .glass-edge {
            position: absolute;
            inset: 0;
            border: 1px solid rgba(255, 255, 255, 0.08);
            pointer-events: none;
            z-index: 11;
        }
    </style>
</head>
<body>
    <canvas id="canvas-silk"></canvas>
    <div class="film-grain"></div>
    <div class="glass-edge"></div>
    <script>
        const canvas = document.getElementById('canvas-silk');
        const ctx = canvas.getContext('2d');
        let width, height;
        let frame = 0;
        const colors = {
            idle: [
                'rgba(100, 100, 120, 0.15)', 
                'rgba(50, 50, 60, 0.1)',
                'rgba(150, 150, 170, 0.05)'
            ],
            active: [
                'rgba(0, 255, 220, 0.6)',
                'rgba(0, 100, 255, 0.5)',
                'rgba(150, 100, 255, 0.4)'
            ]
        };
        let isAwake = true;
        let wakeIntensity = 1;
        let audioLevel = 0;

        function resize() {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
        }
        window.addEventListener('resize', resize);
        resize();

        setInterval(() => {
            if (!isAwake) {
                audioLevel *= 0.96;
                return;
            }
            const t = Date.now() * 0.002;
            const wave = (Math.sin(t) + 1) * 0.5; 
            const random = Math.random() * 0.3;
            const target = wave * 0.4 + random * 0.6;
            audioLevel += (target - audioLevel) * 0.1;
        }, 30);

        class SilkOrb {
            constructor(index) {
                this.index = index;
                this.angle = (index / 3) * Math.PI * 2;
                this.speed = 0.002 + Math.random() * 0.002;
                this.radiusVar = Math.random();
            }
            draw() {
                this.angle += this.speed * (1 + audioLevel);
                const cx = width / 2;
                const cy = height / 2;
                const minDim = Math.min(width, height);
                const pathRadiusX = width * 0.6;
                const pathRadiusY = height * 0.6;
                const x = cx + Math.cos(this.angle) * pathRadiusX;
                const y = cy + Math.sin(this.angle) * pathRadiusY;
                let baseSize = minDim * 0.6; 
                let size = baseSize + (audioLevel * minDim * 0.3 * wakeIntensity);
                const idleColor = colors.idle[this.index % colors.idle.length];
                const activeColor = colors.active[this.index % colors.active.length];
                const drawGrad = (colorStr, alphaMult) => {
                    const g = ctx.createRadialGradient(x, y, 0, x, y, size);
                    g.addColorStop(0, colorStr.replace(/[\d\.]+\)$/, alphaMult + ')'));
                    g.addColorStop(1, 'rgba(0,0,0,0)');
                    ctx.fillStyle = g;
                    ctx.beginPath();
                    ctx.arc(x, y, size, 0, Math.PI * 2);
                    ctx.fill();
                };
                if (wakeIntensity < 1) {
                    drawGrad(idleColor, (1 - wakeIntensity) * 0.8);
                }
                if (wakeIntensity > 0) {
                    const pulse = 0.5 + audioLevel * 0.5;
                    drawGrad(activeColor, wakeIntensity * pulse);
                }
            }
        }

        const orbs = [new SilkOrb(0), new SilkOrb(1), new SilkOrb(2)];

        function animate() {
            const targetIntensity = isAwake ? 1 : 0;
            wakeIntensity += (targetIntensity - wakeIntensity) * 0.05;
            ctx.clearRect(0, 0, width, height);
            orbs.forEach(orb => orb.draw());
            if (wakeIntensity > 0.1) {
                const cx = width / 2;
                const cy = height / 2;
                const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.min(width, height) * 0.8);
                const alpha = wakeIntensity * audioLevel * 0.15;
                g.addColorStop(0, 'rgba(200, 255, 255, ' + alpha + ')');
                g.addColorStop(1, 'rgba(0,0,0,0)');
                ctx.fillStyle = g;
                ctx.fillRect(0,0,width,height);
            }
            frame++;
            requestAnimationFrame(animate);
        }
        animate();
    </script>
</body>
</html>
"""
    }
}
