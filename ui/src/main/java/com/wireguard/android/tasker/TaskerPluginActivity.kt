/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.tasker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.Application
import com.wireguard.android.R
import kotlinx.coroutines.launch

class TaskerPluginActivity : AppCompatActivity() {

    private lateinit var tunnelSpinner: Spinner
    private lateinit var actionRadioGroup: RadioGroup
    private lateinit var radioUp: RadioButton
    private lateinit var radioDown: RadioButton
    private lateinit var radioToggle: RadioButton

    private var tunnelNames: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tasker_plugin_activity)
        setTitle(R.string.app_name)

        tunnelSpinner = findViewById(R.id.tunnel_spinner)
        actionRadioGroup = findViewById(R.id.action_radio_group)
        radioUp = findViewById(R.id.radio_up)
        radioDown = findViewById(R.id.radio_down)
        radioToggle = findViewById(R.id.radio_toggle)

        val buttonCancel = findViewById<Button>(R.id.button_cancel)
        val buttonSave = findViewById<Button>(R.id.button_save)

        buttonCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        buttonSave.setOnClickListener {
            saveAndFinish()
        }

        val previousBundle = intent.getBundleExtra(EXTRA_BUNDLE)
        val prevTunnel = previousBundle?.getString(EXTRA_TUNNEL_NAME)
        val prevAction = previousBundle?.getString(EXTRA_ACTION_TYPE) ?: ACTION_UP

        lifecycleScope.launch {
            try {
                val tunnels = Application.getTunnelManager().getTunnels()
                tunnelNames = tunnels.map { it.name }.sorted()
                if (tunnelNames.isEmpty()) {
                    Toast.makeText(this@TaskerPluginActivity, "No WireGuard tunnels found", Toast.LENGTH_SHORT).show()
                }

                val adapter = ArrayAdapter(
                    this@TaskerPluginActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    tunnelNames
                )
                tunnelSpinner.adapter = adapter

                if (prevTunnel != null) {
                    val idx = tunnelNames.indexOf(prevTunnel)
                    if (idx >= 0) {
                        tunnelSpinner.setSelection(idx)
                    }
                }

                when (prevAction) {
                    ACTION_DOWN -> radioDown.isChecked = true
                    ACTION_TOGGLE -> radioToggle.isChecked = true
                    else -> radioUp.isChecked = true
                }
            } catch (e: Throwable) {
                Toast.makeText(this@TaskerPluginActivity, "Failed to load tunnels: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAndFinish() {
        val selectedTunnel = tunnelSpinner.selectedItem as? String
        if (selectedTunnel.isNullOrBlank()) {
            Toast.makeText(this, "Please select a tunnel", Toast.LENGTH_SHORT).show()
            return
        }

        val action = when (actionRadioGroup.checkedRadioButtonId) {
            R.id.radio_down -> ACTION_DOWN
            R.id.radio_toggle -> ACTION_TOGGLE
            else -> ACTION_UP
        }

        val resultBundle = Bundle().apply {
            putString(EXTRA_TUNNEL_NAME, selectedTunnel)
            putString(EXTRA_ACTION_TYPE, action)
        }

        val blurb = "$selectedTunnel: $action"

        val resultIntent = Intent().apply {
            putExtra(EXTRA_BUNDLE, resultBundle)
            putExtra(EXTRA_BLURB, blurb)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
        const val EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

        const val EXTRA_TUNNEL_NAME = "com.wireguard.android.tasker.extra.TUNNEL_NAME"
        const val EXTRA_ACTION_TYPE = "com.wireguard.android.tasker.extra.ACTION_TYPE"

        const val ACTION_UP = "UP"
        const val ACTION_DOWN = "DOWN"
        const val ACTION_TOGGLE = "TOGGLE"
    }
}

