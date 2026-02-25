package com.example.ava.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object IntentLauncher {
    private const val TAG = "IntentLauncher"
    
    data class LaunchResult(
        val success: Boolean,
        val message: String,
        val intentUri: String
    )
    
    fun launch(context: Context, intentUri: String): LaunchResult {
        if (intentUri.isBlank()) {
            return LaunchResult(false, "Empty intent URI", intentUri)
        }
        
        return try {
            val intent = parseIntent(intentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.i(TAG, "Successfully launched: $intentUri")
            LaunchResult(true, "OK", intentUri)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found: $intentUri", e)
            tryFallback(context, intentUri, e.message ?: "Activity not found")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch: $intentUri", e)
            LaunchResult(false, e.message ?: "Unknown error", intentUri)
        }
    }
    
    private fun parseIntent(intentUri: String): Intent {
        return when {
            intentUri.startsWith("intent:") || intentUri.startsWith("intent:#Intent") -> {
                Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
            }
            intentUri.contains("://") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
            }
            intentUri.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$", RegexOption.IGNORE_CASE)) -> {
                Intent().apply {
                    setPackage(intentUri)
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            }
            else -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
            }
        }
    }
    
    private fun tryFallback(context: Context, intentUri: String, originalError: String): LaunchResult {
        val packageName = extractPackageName(intentUri)
        if (packageName != null) {
            return try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(marketIntent)
                LaunchResult(false, "App not installed, opened store: $packageName", intentUri)
            } catch (e: Exception) {
                LaunchResult(false, originalError, intentUri)
            }
        }
        return LaunchResult(false, originalError, intentUri)
    }
    
    private fun extractPackageName(intentUri: String): String? {
        val packagePattern = Regex("package=([^;]+)")
        packagePattern.find(intentUri)?.let {
            return it.groupValues[1]
        }
        if (intentUri.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$", RegexOption.IGNORE_CASE))) {
            return intentUri
        }
        return null
    }
}
