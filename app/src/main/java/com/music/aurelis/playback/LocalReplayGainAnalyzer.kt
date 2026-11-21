// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.music.aurelis.playback.smart.AudioDecoder
import com.music.aurelis.playback.smart.LocalAudioSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * B04: read-only, on-demand, single-track local R128 analysis.
 *
 * Called from an IO coroutine, never from the audio thread. A successful
 * measurement is cached only when file size and modification time can be
 * verified; edits invalidate it. Unseekable or incomplete files are skipped.
 * No changes to original audio, tags, or MediaStore.
 */
internal object LocalReplayGainAnalyzer {
    private const val PREFS = "aurelis_replaygain_v1"
    private const val MAX_CACHE = 256

    suspend fun analyze(
        context: Context,
        uri: Uri,
        cancelled: () -> Boolean = { false },
    ): ReplayGainMeasurement? = withContext(Dispatchers.IO) {
        if (!LocalAudioSource.isLocal(uri)) return@withContext null
        if (uri.scheme == "content" && uri.authority != "media") return@withContext null
        val stamp = stamp(context, uri) ?: return@withContext null
        val key = digestKey(uri.toString())
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(key, null)?.let { stored ->
            runCatching {
                val json = JSONObject(stored)
                if (json.getLong("size") == stamp.first && json.getLong("modified") == stamp.second) {
                    val loudness = json.getDouble("lufs")
                    val peak = json.getDouble("peak")
                    if (loudness.isFinite() && loudness in -100.0..10.0 &&
                        peak.isFinite() && peak > 0.0
                    ) return@withContext ReplayGainMeasurement(loudness, peak)
                }
            }
        }

        if (cancelled()) return@withContext null
        currentCoroutineContext().ensureActive()
        val measurement = LocalAudioSource.open(context.contentResolver, uri)?.use { source ->
            val duration = AudioDecoder.containerDurationSeconds(source)
                ?.takeIf { it.isFinite() && it in 0.5..(24.0 * 3600.0) }
                ?: return@use null
            var meter: R128Meter? = null
            var incompatible = false
            var meterRate: Int? = null
            val result = AudioDecoder.streamRegionStereo(
                source = source,
                startSeconds = 0.0,
                endSeconds = duration + 1.0,
                abort = { cancelled() || Thread.currentThread().isInterrupted },
            ) { left, right, rate ->
                if (meter == null && rate > 0) meter = R128Meter(rate, 2)
                if (rate <= 0 || incompatible) return@streamRegionStereo
                if (meterRate != null && meterRate != rate) incompatible = true
                if (!incompatible) {
                    if (meterRate == null) meterRate = rate
                    meter?.add(left, right)
                }
            }
            if (result == null || incompatible || cancelled()) null else meter?.finish()
        }
        if (measurement != null && !cancelled() && stamp(context, uri) == stamp) {
            // Bounded preference store; eviction is conservative and affects
            // only cached analysis, never originals or playback metadata.
            val editor = prefs.edit()
            if (!prefs.contains(key) && prefs.all.size >= MAX_CACHE) {
                prefs.all.keys.firstOrNull()?.let(editor::remove)
            }
            editor.putString(key, JSONObject()
                .put("size", stamp.first)
                .put("modified", stamp.second)
                .put("lufs", measurement.integratedLufs)
                .put("peak", measurement.truePeakLinear)
                .toString()
            ).apply()
        }
        measurement
    }

    private fun stamp(context: Context, uri: Uri): Pair<Long, Long>? {
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: return null
            return file.takeIf { it.isFile && it.length() > 0 && it.lastModified() > 0L }
                ?.let { it.length() to it.lastModified() }
        }
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
                null, null, null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val modifiedCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                if (sizeCol < 0 || modifiedCol < 0 ||
                    cursor.isNull(sizeCol) || cursor.isNull(modifiedCol)
                ) return@use null
                val size = cursor.getLong(sizeCol)
                val changed = cursor.getLong(modifiedCol)
                if (size <= 0 || changed <= 0) null else size to changed
            }
        }.getOrNull()
    }

    private fun digestKey(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
