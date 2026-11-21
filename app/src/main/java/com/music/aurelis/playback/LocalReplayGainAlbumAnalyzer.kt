// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import android.content.Context
import android.net.Uri
import com.music.aurelis.data.LocalMediaRepository
import com.music.aurelis.data.model.Song
import com.music.aurelis.playback.smart.AudioDecoder
import com.music.aurelis.playback.smart.LocalAudioSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Full album scan, selected explicitly in settings (never the entire library).
 * Combines 400 ms K-weighted block energies before applying the R128 gates;
 * no averaging of per-track LUFS. On any incomplete track, no album value is
 * published. No audio files, metadata or MediaStore rows are changed.
 */
internal object LocalReplayGainAlbumAnalyzer {
    private const val MAX_ALBUM_TRACKS = 24

    suspend fun analyze(
        context: Context,
        mediaId: String,
        currentUri: Uri,
        cancelled: () -> Boolean,
    ): Map<String, ReplayGainMeasurement>? = withContext(Dispatchers.IO) {
        val library = LocalMediaRepository.getLocalMusic(context)
        val current = library.firstOrNull { it.localUri == currentUri.toString() || it.videoId == mediaId }
            ?: return@withContext null
        val folder = current.localPath?.substringBeforeLast('/', "").orEmpty()
        val albumName = current.albumName?.trim().orEmpty()
        if (albumName.isBlank()) return@withContext null
        val album = library.filter { song ->
            song.albumName?.trim()?.equals(albumName, ignoreCase = true) == true &&
                song.localUri != null &&
                (if (folder.isNotBlank()) song.localPath?.substringBeforeLast('/', "") == folder
                 else song.artist.equals(current.artist, ignoreCase = true))
        }.distinctBy { it.localUri }
        if (album.size !in 2..MAX_ALBUM_TRACKS) return@withContext null
        val energies = ArrayList<Double>()
        val tracks = LinkedHashMap<String, ReplayGainMeasurement>()
        var albumPeak = 0.0

        for (song in album) {
            if (cancelled()) return@withContext null
            currentCoroutineContext().ensureActive()
            val uri = song.localUri?.let { Uri.parse(it) } ?: return@withContext null
            if (!LocalAudioSource.isLocal(uri)) return@withContext null
            val data = LocalAudioSource.open(context.contentResolver, uri)?.use { source ->
                val duration = AudioDecoder.containerDurationSeconds(source)
                    ?.takeIf { it.isFinite() && it in 0.5..(60.0 * 60.0) }
                    ?: return@use null
                var meter: R128Meter? = null
                var inputRate: Int? = null
                var inputChannels: Int? = null
                var incompatible = false
                val output = AudioDecoder.streamRegionStereo(
                    source, 0.0, duration + 1.0,
                    abort = { cancelled() || Thread.currentThread().isInterrupted },
                ) { left, right, rate, channels ->
                    if (incompatible) return@streamRegionStereo
                    if (rate !in 8000..384000 || channels !in 1..2) {
                        incompatible = true
                        return@streamRegionStereo
                    }
                    if (meter == null) {
                        meter = R128Meter(rate, channels)
                        inputRate = rate
                        inputChannels = channels
                    }
                    if (rate != inputRate || channels != inputChannels) {
                        incompatible = true
                        return@streamRegionStereo
                    }
                    meter?.add(left, right)
                }
                if (output == null || incompatible || cancelled()) null else meter
            } ?: return@withContext null
            val blocks = data.blockEnergies()
            if (blocks.isEmpty()) return@withContext null
            energies.addAll(blocks)
            val trackMeasurement = data.finish()
            if (trackMeasurement != null) {
                albumPeak = maxOf(albumPeak, trackMeasurement.truePeakLinear ?: 0.0)
                tracks[song.videoId] = trackMeasurement
            }
        }
        if (cancelled()) return@withContext null
        val lufs = R128Meter.integratedLufs(energies) ?: return@withContext null
        if (tracks.isEmpty() || albumPeak <= 0.0) return@withContext null
        tracks.mapValues { (_, m) ->
            m.copy(albumIntegratedLufs = lufs, albumTruePeakLinear = albumPeak)
        }
    }
}
