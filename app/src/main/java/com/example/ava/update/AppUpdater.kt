package com.example.ava.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String = ""
)

object AppUpdater {
    private const val TAG = "AppUpdater"
    
    
    private const val VERSION_URL = "https://ghfast.top/https://raw.githubusercontent.com/knoop7/Ava/master/version.json"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun checkUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "Checking update from: $VERSION_URL")
            val url = URL(VERSION_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val updateInfo = json.decodeFromString<UpdateInfo>(response)
                val currentVersionCode = getVersionCode(context)
                
                if (updateInfo.versionCode > currentVersionCode) {
                    return@withContext updateInfo
                }
            } else {
                Log.e(TAG, "HTTP error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check update failed: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
        null
    }
    
    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            
            val proxyUrl = if (!updateInfo.downloadUrl.contains("ghfast.top")) {
                "https://ghfast.top/${updateInfo.downloadUrl}"
            } else {
                updateInfo.downloadUrl
            }
            
            
            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Ava-${updateInfo.versionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }
            
            val request = DownloadManager.Request(Uri.parse(proxyUrl)).apply {
                setTitle("Ava ${updateInfo.versionName}")
                setDescription("Downloading update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(apkFile))
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverRoaming(true)
                setAllowedOverMetered(true)
            }
            
            val downloadId = downloadManager.enqueue(request)
            
            
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            ctx.unregisterReceiver(this)
                        } catch (e: Exception) {
                            
                        }
                        installApk(ctx, apkFile)
                    }
                }
            }
            
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
        }
    }
    
    private fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file not found: ${apkFile.absolutePath}")
                return
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                cleanupOldApks(context)
            }, 60000)  
        } catch (e: Exception) {
            Log.e(TAG, "Install APK failed", e)
        }
    }
    
    private fun cleanupOldApks(context: Context) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
        }
    }
    
    private fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }
}
