// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberAndroidAudioOutputs(): List<AudioOutputDevice> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var outputs by remember(manager) { mutableStateOf(readOutputs(manager)) }

    DisposableEffect(manager) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val callback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out android.media.AudioDeviceInfo>?) {
                outputs = readOutputs(manager)
            }
            override fun onAudioDevicesRemoved(removed: Array<out android.media.AudioDeviceInfo>?) {
                outputs = readOutputs(manager)
            }
        }
        manager.registerAudioDeviceCallback(callback, handler)
        onDispose { manager.unregisterAudioDeviceCallback(callback) }
    }

    return outputs
}

private fun readOutputs(manager: AudioManager): List<AudioOutputDevice> {
    val devices = runCatching {
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    }.getOrDefault(emptyList())
    // On API 33+ the OS reports the *anticipated media route* for USAGE_MEDIA.
    // That is a stronger signal than the list of attached devices, but it is
    // still not a decoder/hardware bit-perfect attestation.
    val routedIds: Set<Int> = if (Build.VERSION.SDK_INT >= 33) {
        runCatching {
            manager.getAudioDevicesForAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
            ).map(AudioDeviceInfo::getId).toSet()
        }.getOrDefault(emptySet())
    } else emptySet()
    return devices.mapNotNull { device ->
        val kind = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> AudioOutputKind.PHONE
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputKind.WIRED
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> AudioOutputKind.BLUETOOTH
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputKind.USB
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> AudioOutputKind.HDMI
            else -> return@mapNotNull null
        }
        AudioOutputDevice(
            id = device.id,
            name = device.productName?.toString().orEmpty(),
            kind = kind,
            isActive = device.id in routedIds,
            form = if (kind == AudioOutputKind.BLUETOOTH) BluetoothForm.UNKNOWN else BluetoothForm.UNKNOWN,
        )
    }
}
