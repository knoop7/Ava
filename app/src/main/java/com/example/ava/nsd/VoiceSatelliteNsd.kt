package com.example.ava.nsd

import android.content.Context

private const val VERSION = "2025.9.0"

fun registerVoiceSatelliteNsd(
    context: Context,
    name: String,
    port: Int,
    macAddress: String
): NsdRegistration {
    val nsdRegistration = NsdRegistration(
        name = name,
        type = "_esphomelib._tcp",
        port = port,
        attributes = mapOf(
            Pair("version", VERSION),
            Pair("mac", macAddress),
            Pair("board", "host"),
            Pair("platform", "HOST"),
            Pair("network", "wifi")
        )
    )
    nsdRegistration.register(context)
    return nsdRegistration
}