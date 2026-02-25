package com.example.ava.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ImmersiveMode(isLandscape: Boolean = false) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isLandscape) {
        val activity = view.context as? Activity ?: return@DisposableEffect onDispose {}
        val window = activity.window
        val decorView = window.decorView

        @Suppress("DEPRECATION")
        val uiOptions = if (isLandscape) {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        fun applyImmersiveMode() {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = uiOptions
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }

        applyImmersiveMode()

        val pendingRunnable = Runnable { applyImmersiveMode() }

        @Suppress("DEPRECATION")
        val listener = View.OnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0 ||
                visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                decorView.removeCallbacks(pendingRunnable)
                decorView.postDelayed(pendingRunnable, 500)
            }
        }
        @Suppress("DEPRECATION")
        decorView.setOnSystemUiVisibilityChangeListener(listener)

        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                applyImmersiveMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            decorView.removeCallbacks(pendingRunnable)
            lifecycleOwner.lifecycle.removeObserver(observer)
            @Suppress("DEPRECATION")
            decorView.setOnSystemUiVisibilityChangeListener(null)
        }
    }
}
