package com.example.ava.utils

import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
fun getRandomMacAddressString(): String {
    val random = Random.Default
    val bytes = random.nextBytes(6)
    return bytes.toHexString(HexFormat {
        upperCase = true
        bytes { byteSeparator = ":" }
    })
}