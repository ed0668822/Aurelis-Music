// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.R
import com.music.aurelis.playback.cast.CastController
import com.music.aurelis.playback.cast.DlnaController
import com.music.aurelis.playback.cast.CastOptionsProvider
import com.music.aurelis.playback.cast.SmartCastRanking
import com.music.aurelis.playback.cast.SmartCastCoordinator
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState

/**
 * Where to send the music: the receivers on this network, opened from the
 * output drawer's Cast row.
 *
 * Drawn in [AudioPopupCard], the card the audio pipeline readout uses, so the
 * two popups off the same drawer are visibly one family. The list is live —
 * receivers are found, and lost, while it is open — and the router is asked to
 * scan for as long as it is, which is the only time that is worth its cost.
 */
@Composable
fun CastDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cast by CastController.state.collectAsStateWithLifecycle()
    val dlna by DlnaController.devices.collectAsStateWithLifecycle()
    val dlnaConnected by DlnaController.connected.collectAsStateWithLifecycle()
    val dlnaConnectedId by DlnaController.connectedId.collectAsStateWithLifecycle()
    val dlnaFailure by DlnaController.failure.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val customReceiverEnabled = CastOptionsProvider.customReceiverId(context) != null
    val televisions = remember(cast.devices, dlna, customReceiverEnabled) {
        SmartCastRanking.choose(
            cast = cast.devices.map { SmartCastRanking.AdvertisedDevice(it.id, it.name) },
            dlna = dlna.map { SmartCastRanking.AdvertisedDevice(it.id, it.name) },
            customReceiverEnabled = customReceiverEnabled,
        )
    }
    val haptics = rememberHaptics()
    // Set by tapping a receiver, so the popup closes itself when that receiver
    // connects — but not when somebody opens it while already casting.
    var picked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { DlnaController.scan() }

    DisposableEffect(cast.supported) {
        val stop = CastController.discover(active = true)
        onDispose(stop)
    }
    LaunchedEffect(cast.connectedName, dlnaConnected, picked) {
        if (picked && (cast.connectedName != null || dlnaConnected != null)) onDismiss()
    }

    AudioPopupCard(
        hazeState = hazeState,
        title = stringResource(R.string.cast_dialog_title),
        subtitle = stringResource(R.string.tv_smartcast_subtitle),
        doneLabel = stringResource(R.string.done),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth()) {
            PipelineRule(Modifier)
            if (televisions.isEmpty()) {
                SearchingRow()
            } else {
                televisions.forEach { choice ->
                    val renderer = dlna.firstOrNull { it.id == choice.dlnaId }
                    val connecting = cast.devices.firstOrNull { it.id == choice.castId }?.connecting == true
                    val connected = (choice.castId != null &&
                        cast.devices.any { it.id == choice.castId && it.connected } && cast.casting) ||
                        (renderer != null && dlnaConnectedId == renderer.id)
                    val detail = when {
                        choice.fallback == SmartCastRanking.Protocol.DLNA ->
                            stringResource(R.string.tv_smartcast_auto_cast_dlna)
                        choice.castId != null && choice.lyricsOnTv ->
                            stringResource(R.string.tv_smartcast_cast_lyrics)
                        choice.castId != null ->
                            stringResource(R.string.tv_smartcast_cast_audio)
                        else -> stringResource(R.string.tv_smartcast_dlna_audio)
                    }
                    CastDeviceRow(
                        name = choice.name,
                        detail = detail,
                        connecting = connecting,
                        connected = connected,
                        isDlna = choice.castId == null,
                        onClick = {
                            if (cast.casting || dlnaConnected != null) {
                                if (!connected) {
                                    Toast.makeText(context, R.string.tv_disconnect_first, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                haptics.play(Haptic.Select)
                                picked = true
                                SmartCastCoordinator.connect(choice, renderer)
                            }
                        },
                    )
                    if (choice.fallback == SmartCastRanking.Protocol.DLNA && renderer != null) {
                        CastActionRow(
                            label = stringResource(R.string.tv_manual_dlna_format, choice.name),
                            destructive = false,
                            onClick = {
                                if (cast.casting || dlnaConnected != null) {
                                    Toast.makeText(context, R.string.tv_disconnect_first, Toast.LENGTH_SHORT).show()
                                } else {
                                    haptics.play(Haptic.Select)
                                    picked = true
                                    SmartCastCoordinator.connectDlnaManually(renderer)
                                }
                            },
                        )
                    }
                    PipelineRule(Modifier.padding(start = 56.dp))
                }
            }
            if (dlnaFailure != null) {
                val failureLabel = when (dlnaFailure) {
                    DlnaController.Failure.UNSUPPORTED -> R.string.tv_dlna_unsupported
                    DlnaController.Failure.CONNECTION_LOST -> R.string.tv_connection_lost
                    else -> R.string.tv_dlna_connect_failed
                }
                Text(
                    text = stringResource(failureLabel),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFAEAB),
                )
            }
            CastActionRow(
                label = stringResource(R.string.tv_screen_mirroring_settings),
                destructive = false,
                onClick = {
                    haptics.play(Haptic.Select)
                    try {
                        context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                        onDismiss()
                    } catch (_: android.content.ActivityNotFoundException) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                            onDismiss()
                        } catch (_: Exception) {
                            Toast.makeText(context, R.string.tv_mirroring_unavailable, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: SecurityException) {
                        Toast.makeText(context, R.string.tv_mirroring_unavailable, Toast.LENGTH_SHORT).show()
                    }
                },
            )
            if (cast.casting || dlnaConnected != null) {
                CastActionRow(
                    label = stringResource(R.string.cast_stop),
                    onClick = {
                        haptics.play(Haptic.Select)
                        if (cast.casting) CastController.disconnect(resumeHere = true)
                        if (dlnaConnected != null) DlnaController.disconnect(resumeHere = true)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun CastDeviceRow(
    name: String,
    detail: String? = null,
    isDlna: Boolean = false,
    connecting: Boolean,
    connected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) Color.White.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isDlna) Icons.Rounded.Tv
                else if (connected) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (connected) 1f else 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = if (connected) FontWeight.W600 else FontWeight.Normal,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
            if (connecting || connected) {
                Text(
                    text = stringResource(
                        if (connecting) R.string.cast_device_connecting else R.string.cast_device_connected,
                    ),
                    modifier = Modifier.padding(top = 1.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        when {
            connecting -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White.copy(alpha = 0.8f),
                strokeWidth = 1.75.dp,
            )
            connected -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Shown until the first receiver turns up; it is also what an empty network looks like. */
@Composable
private fun SearchingRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White.copy(alpha = 0.7f),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.cast_searching),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.cast_searching_hint),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
            color = Color.White.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CastActionRow(
    label: String,
    destructive: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) Color.White.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
            ),
            color = if (destructive) Color(0xFFFF6B63) else Color.White.copy(alpha = 0.9f),
        )
    }
}
