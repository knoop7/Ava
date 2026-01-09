package com.example.ava.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.core.content.ContextCompat

class NsdRegistration(
    var name: String,
    type: String,
    port: Int,
    attributes: Map<String, String> = emptyMap(),
    private val onNameChanged: ((String) -> Unit)? = null
) {
    private val serviceInfo = NsdServiceInfo().apply {
        serviceName = name
        serviceType = type
        this.port = port
        for ((key, value) in attributes) {
            setAttribute(key, value)
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
            
            val newName = nsdServiceInfo.serviceName
            if (newName != name) {
                Log.w(TAG, "NSD name changed from '$name' to '$newName'")
                name = newName
                onNameChanged?.invoke(newName)
            }
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Service registration failed: $errorCode")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Service unregistration failed: $errorCode")
        }
    }

    fun register(context: Context) {
        try {
            ContextCompat.getSystemService(context, NsdManager::class.java)?.apply {
                registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Service registration failed", e)
        }
    }

    fun unregister(context: Context) {
        try {
            ContextCompat.getSystemService(context, NsdManager::class.java)?.apply {
                unregisterService(registrationListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Service unregistration failed", e)
        }
    }

    companion object {
        const val TAG = "NsdRegistration"
    }
}