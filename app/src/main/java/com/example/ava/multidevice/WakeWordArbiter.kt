package com.example.ava.multidevice

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.UUID

private const val TAG = "WakeWordArbiter"
private const val BROADCAST_PORT = 19847
private const val ARBITRATION_TIMEOUT_MS = 80L
private const val PACKET_SIZE = 128

private val deviceId: String = UUID.randomUUID().toString().substring(0, 8)

data class ArbiterResult(
    val shouldRespond: Boolean,
    val reason: String,
    val competitorCount: Int = 0
)

suspend fun arbitrateWakeWord(wakeWordId: String, timestamp: Long = System.currentTimeMillis()): ArbiterResult {
    return withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(java.net.InetSocketAddress(BROADCAST_PORT))
            socket.broadcast = true
            socket.soTimeout = ARBITRATION_TIMEOUT_MS.toInt()
            
            val message = buildMessage(wakeWordId, timestamp)
            broadcastMessage(socket, message)
            Log.d(TAG, "broadcast sent: deviceId=$deviceId ts=$timestamp wakeWord=$wakeWordId")
            
            val competitors = collectCompetitors(socket, timestamp, wakeWordId)
            Log.d(TAG, "collected ${competitors.size} competitors")
            
            if (competitors.isEmpty()) {
                return@withContext ArbiterResult(true, "no_competitors", 0)
            }
            
            val winner = selectWinner(deviceId, timestamp, competitors)
            val shouldRespond = winner == deviceId
            Log.d(TAG, "winner=$winner shouldRespond=$shouldRespond")
            
            ArbiterResult(shouldRespond, if (shouldRespond) "won" else "lost_to_$winner", competitors.size)
        } catch (e: Exception) {
            Log.e(TAG, "arbitration failed: ${e.message}")
            ArbiterResult(true, "error_fallback", 0)
        } finally {
            socket?.close()
        }
    }
}

private fun buildMessage(wakeWordId: String, timestamp: Long): String {
    return "$deviceId|$timestamp|$wakeWordId"
}

private fun parseMessage(data: String): Triple<String, Long, String>? {
    val parts = data.trim().split("|")
    if (parts.size != 3) return null
    val id = parts[0]
    val ts = parts[1].toLongOrNull() ?: return null
    val wakeWord = parts[2]
    return Triple(id, ts, wakeWord)
}

private fun broadcastMessage(socket: DatagramSocket, message: String) {
    val bytes = message.toByteArray(Charsets.UTF_8)
    val packet = DatagramPacket(
        bytes,
        bytes.size,
        InetAddress.getByName("255.255.255.255"),
        BROADCAST_PORT
    )
    socket.send(packet)
}

private suspend fun collectCompetitors(
    socket: DatagramSocket,
    myTimestamp: Long,
    myWakeWordId: String
): List<Pair<String, Long>> {
    val competitors = mutableListOf<Pair<String, Long>>()
    val buffer = ByteArray(PACKET_SIZE)
    val startTime = System.currentTimeMillis()
    
    while (System.currentTimeMillis() - startTime < ARBITRATION_TIMEOUT_MS) {
        try {
            val packet = DatagramPacket(buffer, buffer.size)
            val remaining = ARBITRATION_TIMEOUT_MS - (System.currentTimeMillis() - startTime)
            if (remaining <= 0) break
            socket.soTimeout = remaining.toInt().coerceAtLeast(1)
            socket.receive(packet)
            
            val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val parsed = parseMessage(data) ?: continue
            val (id, ts, wakeWord) = parsed
            
            if (id == deviceId) continue
            if (wakeWord != myWakeWordId) continue
            if (kotlin.math.abs(ts - myTimestamp) > 500) continue
            
            Log.d(TAG, "received competitor: id=$id ts=$ts")
            competitors.add(id to ts)
        } catch (e: SocketTimeoutException) {
            break
        }
    }
    return competitors
}

private fun selectWinner(myId: String, myTimestamp: Long, competitors: List<Pair<String, Long>>): String {
    val allDevices = competitors + (myId to myTimestamp)
    val sorted = allDevices.sortedWith(compareBy({ it.second }, { it.first }))
    return sorted.first().first
}

fun getDeviceId(): String = deviceId
