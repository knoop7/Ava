package com.example.ava.utils

import java.nio.ByteBuffer

fun ByteBuffer.fillFrom(src: ByteBuffer): Int {
    val remaining = remaining()
    if (remaining == 0)
        return 0

    val srcRemaining = src.remaining()
    if (srcRemaining <= remaining) {
        put(src)
        return srcRemaining
    } else {
        val currentLimit = src.limit()
        src.limit(src.position() + remaining)
        put(src)
        src.limit(currentLimit)
        return remaining
    }
}