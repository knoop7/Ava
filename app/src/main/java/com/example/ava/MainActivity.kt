package com.example.ava

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.ava.utils.A64KeyHandler
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.ava.services.DreamClockService
import com.example.ava.services.VinylCoverService
import com.example.ava.services.WeatherOverlayService
import com.example.ava.services.WebViewService
import com.example.ava.ui.MainNavHost
import com.example.ava.ui.theme.AvaTheme
import com.example.ava.update.AppUpdater
import com.example.ava.update.UpdateInfo
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var lastTapTime: Long = 0
    
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            com.example.ava.services.ScreensaverController.onUserInteraction()
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 300) {
                
                val prefs = getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)
                val userStopped = prefs.getBoolean("service_user_stopped", true) 
                
                if (!userStopped) {
                    
                    DreamClockService.toggle(this)
                    WeatherOverlayService.toggle(this)
                }
                lastTapTime = 0
            } else {
                lastTapTime = currentTime
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        
        if (A64KeyHandler.onKeyDown(this, keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        
        if (A64KeyHandler.onKeyUp(this, keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
    
    
    private fun clearClarityCache() {
        try {
            val clarityDir = java.io.File(cacheDir, "microsoft_clarity")
            if (clarityDir.exists() && clarityDir.isDirectory) {
                clarityDir.deleteRecursively()
            }
        } catch (e: Exception) {
            
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        WebViewService.resetSettingsState()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        val config = ClarityConfig(
            projectId = "upuvf4equo",
            logLevel = com.microsoft.clarity.models.LogLevel.None
        )
        Clarity.initialize(applicationContext, config)
        
        
        Thread {
            try {
                
                val url = java.net.URL("https://m.clarity.ms")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.requestMethod = "HEAD"
                val responseCode = connection.responseCode
                connection.disconnect()
                
                if (responseCode !in 200..399) {
                    
                    Clarity.pause()
                    clearClarityCache()
                }
            } catch (e: Exception) {
                
                Clarity.pause()
                clearClarityCache()
            }
        }.start()
        
        setContent {
            AvaTheme {
                var showWelcomeDialog by remember { mutableStateOf(false) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                val context = LocalContext.current
                val isEnglish = remember {
                    Locale.getDefault().language.startsWith("en")
                }
                
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)
                    val hasShownWelcome = prefs.getBoolean("has_shown_welcome", false)
                    if (!hasShownWelcome && !isEnglish) {
                        showWelcomeDialog = true
                        prefs.edit().putBoolean("has_shown_welcome", true).apply()
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            showWelcomeDialog = false
                        }, 20000)
                    }
                    
                    
                    val info = AppUpdater.checkUpdate(context)
                    if (info != null) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                }
                
                
                val navigateTo = intent?.getStringExtra("navigate_to")
                MainNavHost(startDestination = navigateTo ?: "home")
                
                if (showWelcomeDialog) {
                    WelcomeDialog(onDismiss = { showWelcomeDialog = false })
                }
                
                if (showUpdateDialog && updateInfo != null) {
                    UpdateDialog(
                        updateInfo = updateInfo!!,
                        onDismiss = { showUpdateDialog = false },
                        onUpdate = {
                            AppUpdater.downloadAndInstall(context, updateInfo!!)
                            showUpdateDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.ava.services.ScreensaverController.onUserInteraction()
    }
}

@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val qrBitmap = remember {
        try {
            context.assets.open("qrcode_laowang.jpg").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_code_desc),
                        modifier = Modifier.size(200.dp)
                    )
                }
                
                Text(
                    text = stringResource(R.string.wechat_account),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Text(
                    text = stringResource(R.string.auto_close_countdown),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    
    val accentColor = Color(0xFF4F46E5) 
    val titleColor = Color(0xFF1E293B) 
    val labelColor = Color(0xFF334155) 
    val subLabelColor = Color(0xFF94A3B8) 
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_available),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                
                Text(
                    text = "v${updateInfo.versionName}",
                    fontSize = 15.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (updateInfo.changelog.isNotEmpty()) {
                    Text(
                        text = updateInfo.changelog,
                        fontSize = 13.sp,
                        color = labelColor,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.update_later),
                            fontSize = 14.sp,
                            color = subLabelColor
                        )
                    }
                    
                    Button(
                        onClick = onUpdate,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_now),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
