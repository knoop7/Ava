package com.example.ava.webmonkey

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import android.webkit.WebView

object WmSettings {
    private const val PREFS_NAME = "webmonkey_prefs"
    private const val KEY_LAST_URL = "last_url"
    private const val KEY_USER_AGENT = "user_agent"
    private const val KEY_SHARED_SECRET = "shared_secret"
    
    private var defaultUserAgent: String? = null
    
    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun setLastUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_LAST_URL, url).apply()
    }
    
    fun getLastUrl(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_URL, "about:blank") ?: "about:blank"
    }
    
    fun setUserAgent(context: Context, value: String?) {
        getPrefs(context).edit().putString(KEY_USER_AGENT, value).apply()
    }
    
    fun getUserAgent(context: Context, resolveDefault: Boolean = true): String? {
        val saved = getPrefs(context).getString(KEY_USER_AGENT, null)
        if (!saved.isNullOrEmpty()) return saved
        return if (resolveDefault) getDefaultUserAgent(context) else null
    }
    
    fun getDefaultUserAgent(context: Context): String {
        if (defaultUserAgent == null) {
            try {
                defaultUserAgent = WebSettings.getDefaultUserAgent(context)
            } catch (e: Exception) {
                defaultUserAgent = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/100.0.0.0 Mobile Safari/537.36"
            }
        }
        return defaultUserAgent!!
    }
    
    fun setSharedSecret(context: Context, secret: String?) {
        getPrefs(context).edit().putString(KEY_SHARED_SECRET, secret).apply()
    }
    
    fun getSharedSecret(context: Context): String? {
        return getPrefs(context).getString(KEY_SHARED_SECRET, null)
    }
}
