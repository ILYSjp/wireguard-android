/*
 * Copyright © 2026 WireGuard LLC & Antigravity. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.Application

data class SpaSettings(
    val enabled: Boolean = false,
    val port: Int = 62201,
    val key: String = "",
    val waitMs: Long = 100L
)

object SpaConfig {
    private const val PREFS_NAME = "spa_settings"

    fun get(tunnelName: String): SpaSettings {
        if (tunnelName.isBlank()) return SpaSettings()
        val prefs = Application.get().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("${tunnelName}_enabled", false)
        val port = prefs.getInt("${tunnelName}_port", 62201)
        val key = prefs.getString("${tunnelName}_key", "") ?: ""
        val waitMs = prefs.getLong("${tunnelName}_wait_ms", 100L)
        return SpaSettings(enabled, port, key, waitMs)
    }

    fun save(tunnelName: String, settings: SpaSettings) {
        if (tunnelName.isBlank()) return
        val prefs = Application.get().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("${tunnelName}_enabled", settings.enabled)
            .putInt("${tunnelName}_port", settings.port)
            .putString("${tunnelName}_key", settings.key.trim())
            .putLong("${tunnelName}_wait_ms", settings.waitMs)
            .apply()
    }

    fun rename(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank()) return
        val settings = get(oldName)
        save(newName, settings)
        delete(oldName)
    }

    fun delete(tunnelName: String) {
        if (tunnelName.isBlank()) return
        val prefs = Application.get().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("${tunnelName}_enabled")
            .remove("${tunnelName}_port")
            .remove("${tunnelName}_key")
            .remove("${tunnelName}_wait_ms")
            .apply()
    }

    /**
     * Parses SPA parameters embedded as comments in WireGuard configuration text.
     */
    fun parseFromConfigText(text: String): SpaSettings {
        val enabled = Regex("(?mi)^\\s*#\\s*SPA_ENABLED\\s*=\\s*true\\s*$").containsMatchIn(text)
        val port = Regex("(?mi)^\\s*#\\s*SPA_PORT\\s*=\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 62201
        val key = Regex("(?mi)^\\s*#\\s*SPA_KEY\\s*=\\s*([^\\r\\n]+)").find(text)?.groupValues?.get(1)?.trim() ?: ""
        val waitMs = Regex("(?mi)^\\s*#\\s*SPA_WAIT_MS\\s*=\\s*(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 100L
        return SpaSettings(enabled, port, key, waitMs)
    }

    /**
     * Appends SPA parameters as comments to WireGuard configuration text.
     */
    fun appendToConfigText(baseConfigText: String, settings: SpaSettings): String {
        if (!settings.enabled && settings.key.isBlank()) {
            return baseConfigText
        }
        val spaLines = buildString {
            appendLine("# SPA_ENABLED = ${settings.enabled}")
            appendLine("# SPA_PORT = ${settings.port}")
            appendLine("# SPA_KEY = ${settings.key}")
            appendLine("# SPA_WAIT_MS = ${settings.waitMs}")
        }
        return if (baseConfigText.contains("[Interface]", ignoreCase = true)) {
            baseConfigText.replaceFirst(Regex("(?i)(\\[Interface\\]\\r?\\n)"), "$1$spaLines")
        } else {
            baseConfigText + "\n" + spaLines
        }
    }
}

