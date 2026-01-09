package com.example.ava.utils

import android.util.Log


object SystemPropertiesProxy {
    private const val TAG = "SystemPropertiesProxy"
    
    fun get(key: String, def: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getDeclaredMethod("get", String::class.java, String::class.java)
            getMethod.isAccessible = true
            getMethod.invoke(systemPropertiesClass, key, def) as String
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SystemProperty: $key", e)
            def
        }
    }
    
    fun set(key: String, value: String): Boolean {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val setMethod = systemPropertiesClass.getDeclaredMethod("set", String::class.java, String::class.java)
            setMethod.isAccessible = true
            setMethod.invoke(systemPropertiesClass, key, value)
            Log.d(TAG, "Set SystemProperty: $key = $value")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set SystemProperty: $key = $value", e)
            false
        }
    }
    
    fun getInt(key: String, def: Int): Int {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getIntMethod = systemPropertiesClass.getDeclaredMethod("getInt", String::class.java, Int::class.javaPrimitiveType)
            getIntMethod.isAccessible = true
            getIntMethod.invoke(systemPropertiesClass, key, def) as Int
        } catch (e: Exception) {
            Log.e(TAG, "Failed to getInt SystemProperty: $key", e)
            def
        }
    }
}
