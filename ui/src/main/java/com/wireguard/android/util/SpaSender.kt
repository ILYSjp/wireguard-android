/*
 * Copyright © 2026 WireGuard LLC & Antigravity. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SpaSender {
    private const val TAG = "SpaSender"
    private const val PACKET_SIZE = 56 // 8 (timestamp) + 16 (nonce) + 32 (hmac-sha256)

    /**
     * SPA パケットを非同期で送信する
     * @param host ターゲットホスト名 (例: "h00.ilys.jp")
     * @param port ターゲット SPA ポート (例: 62201)
     * @param keyStr 事前共有キー (Base64 文字列または平文)
     * @param waitMs 送信後の待機ミリ秒数 (例: 100)
     */
    suspend fun sendSpaPacket(
        host: String,
        port: Int,
        keyStr: String,
        waitMs: Long = 100L
    ) = withContext(Dispatchers.IO) {
        try {
            if (keyStr.isBlank()) return@withContext

            // 鍵のデコード
            val keyBytes = try {
                Base64.decode(keyStr, Base64.DEFAULT)
            } catch (e: Exception) {
                keyStr.toByteArray(Charsets.UTF_8)
            }

            // IPv6 アドレスの解決
            val addresses = InetAddress.getAllByName(host)
            val ipv6Addr = addresses.firstOrNull { it is Inet6Address }
            if (ipv6Addr == null) {
                Log.w(TAG, "No IPv6 address found for $host")
                return@withContext
            }

            // パケットの組み立て (56バイト)
            val packet = ByteArray(PACKET_SIZE)
            val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)

            // 1. タイムスタンプ (8バイト)
            val nowEpoch = System.currentTimeMillis() / 1000L
            buffer.putLong(nowEpoch)

            // 2. ランダム Nonce (16バイト)
            val nonce = ByteArray(16)
            SecureRandom().nextBytes(nonce)
            buffer.put(nonce)

            // 3. HMAC-SHA256 (32バイト)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
            mac.update(packet, 0, 24)
            val hmacBytes = mac.doFinal()
            buffer.put(hmacBytes)

            // UDP パケット送信
            DatagramSocket().use { socket ->
                val datagram = DatagramPacket(packet, packet.size, ipv6Addr, port)
                socket.send(datagram)
            }

            Log.i(TAG, "Sent SPA packet to [$ipv6Addr]:$port")

            if (waitMs > 0) {
                kotlinx.coroutines.delay(waitMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SPA packet: ${e.message}", e)
        }
    }
}

