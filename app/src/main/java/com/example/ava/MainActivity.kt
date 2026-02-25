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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.example.ava.services.DreamClockService
import com.example.ava.services.VinylCoverService
import com.example.ava.services.WeatherOverlayService
import com.example.ava.services.WebViewService
import com.example.ava.ui.MainNavHost
import com.example.ava.ui.theme.AvaTheme
import com.example.ava.ui.prefs.rememberBooleanPreference
import com.example.ava.ui.screens.home.KEY_DARK_MODE
import com.example.ava.ui.screens.home.PREFS_NAME
import com.example.ava.update.AppUpdater
import com.example.ava.update.UpdateInfo
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.example.ava.utils.LocaleUtils
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    companion object {
        @Volatile
        private var clarityInitialized = false
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.applyLocale(newBase))
    }
    private var lastTapTime: Long = 0
    
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        com.example.ava.services.ScreensaverController.onUserInteraction()
        when (ev?.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                com.example.ava.services.VoiceSatelliteService.getInstance()?.onScreenTouch(true)
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
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                com.example.ava.services.VoiceSatelliteService.getInstance()?.onScreenTouch(false)
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
            val clarityFilesDir = java.io.File(filesDir, "microsoft_clarity")
            if (clarityFilesDir.exists() && clarityFilesDir.isDirectory) {
                clarityFilesDir.deleteRecursively()
            }
            val clarityDbDir = java.io.File(filesDir.parentFile, "databases")
            clarityDbDir.listFiles()?.filter { it.name.contains("clarity", ignoreCase = true) }?.forEach { it.delete() }
        } catch (e: Exception) {
            
        }
    }
    
    override fun onResume() {
        super.onResume()
        com.example.ava.services.ScreensaverController.onUserInteraction()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        WebViewService.resetSettingsState()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        clearClarityCache()
        

        if (!clarityInitialized && com.example.ava.services.VoiceSatelliteService.getInstance() != null) {
            Thread {
                var connection: java.net.HttpURLConnection? = null
                try {
                    val url = java.net.URL("https://m.clarity.ms")
                    connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "HEAD"
                    val responseCode = connection.responseCode
                    
                    if (responseCode in 200..399 && !clarityInitialized) {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                if (!clarityInitialized && com.example.ava.services.VoiceSatelliteService.getInstance() != null) {
                                    val config = ClarityConfig(
                                        projectId = "upuvf4equo",
                                        logLevel = com.microsoft.clarity.models.LogLevel.None
                                    )
                                    Clarity.initialize(applicationContext, config)
                                    clarityInitialized = true
                                }
                            } catch (e: Exception) {
                                clearClarityCache()
                            }
                        }
                    }
                } catch (e: Exception) {
                    clearClarityCache()
                } finally {
                    connection?.disconnect()
                }
            }.start()
        }
        
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            val isDarkMode by rememberBooleanPreference(prefs, KEY_DARK_MODE, false)

            AvaTheme(darkTheme = isDarkMode) {
                var showWelcomeDialog by remember { mutableStateOf(false) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
                val isEnglish = remember {
                    Locale.getDefault().language.startsWith("en")
                }
                
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("ava_prefs", Context.MODE_PRIVATE)
                    val hasShownWelcome = prefs.getBoolean("has_shown_welcome", false)
                    if (!hasShownWelcome && !isEnglish) {
                        showWelcomeDialog = true
                        prefs.edit().putBoolean("has_shown_welcome", true).apply()
                        
                        kotlinx.coroutines.delay(20000)
                        showWelcomeDialog = false
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
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    val titleColor = colorScheme.onSurface
    val labelColor = colorScheme.onSurfaceVariant
    val subLabelColor = colorScheme.outline
    val bgColor = colorScheme.surfaceVariant
    val dialogBgColor = colorScheme.surface
    
    Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .background(dialogBgColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.update_available),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            
            val context = LocalContext.current
            val currentVersion = remember {
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName ?: "?"
                } catch (e: Exception) { "?" }
            }
            
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = subLabelColor)) {
                        append("v$currentVersion")
                    }
                    withStyle(SpanStyle(color = subLabelColor)) {
                        append(" → ")
                    }
                    withStyle(SpanStyle(color = Color(0xFFEF4444))) {
                        append("v${updateInfo.versionName}")
                    }
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (updateInfo.changelog.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.update_changelog),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subLabelColor
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val changelogScrollState = rememberScrollState()
                val density = androidx.compose.ui.platform.LocalDensity.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 120.dp)
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .drawWithContent {
                            drawContent()
                            if (changelogScrollState.maxValue > 0) {
                                val trackHeight = size.height
                                val contentHeight = trackHeight + changelogScrollState.maxValue
                                val thumbHeight = (trackHeight / contentHeight * trackHeight).coerceAtLeast(with(density) { 16.dp.toPx() })
                                val scrollProgress = changelogScrollState.value.toFloat() / changelogScrollState.maxValue
                                val thumbOffset = scrollProgress * (trackHeight - thumbHeight)
                                drawRoundRect(
                                    color = subLabelColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(size.width - with(density) { 3.dp.toPx() }, thumbOffset),
                                    size = androidx.compose.ui.geometry.Size(with(density) { 3.dp.toPx() }, thumbHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(with(density) { 2.dp.toPx() })
                                )
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (changelogScrollState.maxValue > 0) 8.dp else 0.dp)
                            .verticalScroll(changelogScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MarkdownChangelog(
                            text = updateInfo.changelog,
                            color = labelColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (updateInfo.forceUpdate) {
                Button(
                    onClick = onUpdate,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.update_now),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.update_later),
                            fontSize = 13.sp,
                            color = subLabelColor
                        )
                    }
                    
                    Button(
                        onClick = onUpdate,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.update_now),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownChangelog(text: String, color: Color) {
    val lines = text.split("\n")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### "),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 13.sp,
                            color = color
                        )
                        Text(
                            text = parseBoldText(trimmed.substring(2)),
                            fontSize = 13.sp,
                            color = color,
                            lineHeight = 18.sp
                        )
                    }
                }
                trimmed.isNotEmpty() -> {
                    Text(
                        text = parseBoldText(trimmed),
                        fontSize = 13.sp,
                        color = color,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.contains("**")) {
            val startIndex = remaining.indexOf("**")
            if (startIndex > 0) {
                append(remaining.substring(0, startIndex))
            }
            remaining = remaining.substring(startIndex + 2)
            val endIndex = remaining.indexOf("**")
            if (endIndex >= 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(remaining.substring(0, endIndex))
                }
                remaining = remaining.substring(endIndex + 2)
            } else {
                append("**")
                break
            }
        }
        append(remaining)
    }
}
