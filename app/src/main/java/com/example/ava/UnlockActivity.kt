package com.example.ava

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager


class UnlockActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        
        val shouldUnlock = intent.getBooleanExtra("unlock", false)
        if (shouldUnlock) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, null)
            }
        }
        
        
        finish()
    }
}
