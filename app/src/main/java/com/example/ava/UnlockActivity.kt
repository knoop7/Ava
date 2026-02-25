package com.example.ava

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager


class UnlockActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val finishRunnable = Runnable { finish() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        val shouldUnlock = intent.getBooleanExtra("unlock", false)
        if (shouldUnlock) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        handler.removeCallbacks(finishRunnable)
                        finish()
                    }
                    override fun onDismissCancelled() {
                        handler.removeCallbacks(finishRunnable)
                        finish()
                    }
                    override fun onDismissError() {
                        handler.removeCallbacks(finishRunnable)
                        finish()
                    }
                })
                handler.postDelayed(finishRunnable, 5000)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(finishRunnable)
        super.onDestroy()
    }
}
