/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.wireguard.android.Application
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.util.ErrorMessages
import kotlinx.coroutines.launch

class TaskerPluginReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (ACTION_FIRE_SETTING != action) return

        val bundle = intent.getBundleExtra(TaskerPluginActivity.EXTRA_BUNDLE) ?: return
        val tunnelName = bundle.getString(TaskerPluginActivity.EXTRA_TUNNEL_NAME) ?: return
        val actionType = bundle.getString(TaskerPluginActivity.EXTRA_ACTION_TYPE) ?: TaskerPluginActivity.ACTION_UP

        Application.getCoroutineScope().launch {
            try {
                val manager = Application.getTunnelManager()
                val tunnels = manager.getTunnels()
                val tunnel = tunnels[tunnelName]
                if (tunnel == null) {
                    Log.w(TAG, "Tunnel '$tunnelName' not found")
                    return@launch
                }

                val currentState = tunnel.state
                val targetState = when (actionType) {
                    TaskerPluginActivity.ACTION_DOWN -> Tunnel.State.DOWN
                    TaskerPluginActivity.ACTION_TOGGLE -> {
                        if (currentState == Tunnel.State.UP) Tunnel.State.DOWN else Tunnel.State.UP
                    }
                    else -> Tunnel.State.UP
                }

                if (currentState != targetState) {
                    manager.setTunnelState(tunnel, targetState)
                    Log.i(TAG, "Set tunnel '$tunnelName' to $targetState via Tasker plugin")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to toggle tunnel via Tasker: ${e.message}", e)
                Toast.makeText(context, ErrorMessages[e], Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val TAG = "WireGuard/TaskerReceiver"
        const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
    }
}

