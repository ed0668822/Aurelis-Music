// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data.innertube

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** Genuine public YouTube Most Replayed curves. No audio analysis or inferred plays. */
data class ReplayHeatmap(val segmentMs: Long, val intensity: List<Float>) {
    val durationMs: Long get() = segmentMs * intensity.size
}

/**
 * B04: reusable watch-page heatmap reader for the Media3 player.
 * Only real heatmap markers yield a result. Local lyrics remain a separate
 * fallback and are never labeled as a public replay count.
 */
object MostReplayed {
    private const val MIN_MARKERS = 10
    private const val MIN_DURATION_MS = 60_000L
    private const val SKIP_HEAD_MS = 5_000L
    private const val SKIP_TAIL_MS = 30_000L
    private const val NETWORK_TIMEOUT_MS = 8_000L
    private const val POSITIVE_TTL_MS = 30L * 24 * 60 * 60 * 1000
    private const val NEGATIVE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    private const val MAX_ENTRIES = 350

    @Serializable
    private data class Entry(val segmentMs: Long, val intensity: List<Float>, val checkedAt: Long) {
        fun heatmap(): ReplayHeatmap? =
            if (segmentMs > 0L && intensity.size >= MIN_MARKERS) ReplayHeatmap(segmentMs, intensity)
            else null
    }

    @Serializable
    private data class Disk(val version: Int = 1, val entries: Map<String, Entry> = emptyMap())

    private val json = Json { ignoreUnknownKeys = true }
    private val entries = ConcurrentHashMap<String, Entry>()
    private val lock = Mutex()
    @Volatile private var cacheFile: File? = null
    @Volatile private var loaded = false

    /** Called during Android startup, not from a Composable. */
    fun configureCache(filesDir: File) {
        cacheFile = File(filesDir, "most_replayed_v1.json")
        loaded = false
        entries.clear()
    }

    fun isYouTubeId(id: String): Boolean =
        id.length == 11 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    private fun fresh(entry: Entry, now: Long): Boolean =
        now - entry.checkedAt in 0 until if (entry.heatmap() == null) NEGATIVE_TTL_MS else POSITIVE_TTL_MS

    /** A nonblocking first value for the seek button, when previously fetched. */
    fun knownPeak(videoId: String, durationMs: Long): Long? =
        entries[videoId]?.takeIf { fresh(it, System.currentTimeMillis()) }
            ?.heatmap()?.let { peakMs(it, durationMs) }

    suspend fun peakFor(videoId: String, durationMs: Long): Long? {
        if (!isYouTubeId(videoId) || durationMs < MIN_DURATION_MS) return null
        ensureLoaded()
        knownPeak(videoId, durationMs)?.let { return it }
        // A cached confirmed absence is not a network failure; do not refetch.
        entries[videoId]?.takeIf { fresh(it, System.currentTimeMillis()) && it.heatmap() == null }
            ?.let { return null }
        val answer = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            try {
                Innertube.webNext(videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } ?: return entries[videoId]?.heatmap()?.let { peakMs(it, durationMs) }
        val parsed = parse(answer)
        entries[videoId] = Entry(
            segmentMs = parsed?.segmentMs ?: 0L,
            intensity = parsed?.intensity.orEmpty(),
            checkedAt = System.currentTimeMillis(),
        )
        persist()
        return parsed?.let { peakMs(it, durationMs) }
    }

    private suspend fun ensureLoaded() = lock.withLock {
        if (loaded) return@withLock
        loaded = true
        val file = cacheFile ?: return@withLock
        withContext(Dispatchers.IO) {
            val previous = runCatching {
                if (file.isFile) json.decodeFromString(Disk.serializer(), file.readText()) else null
            }.getOrNull()
            previous?.takeIf { it.version == 1 }?.entries?.let { entries.putAll(it) }
        }
    }

    private suspend fun persist() = lock.withLock {
        val file = cacheFile ?: return@withLock
        withContext(Dispatchers.IO) {
            runCatching {
                val latest = entries.entries.sortedByDescending { it.value.checkedAt }
                    .take(MAX_ENTRIES).associate { it.key to it.value }
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(Disk.serializer(), Disk(entries = latest)))
                if (!tmp.renameTo(file)) {
                    file.writeText(tmp.readText())
                    tmp.delete()
                }
            }
        }
    }

    /** Parse macroMarkersListEntity irrespective of nesting in YouTube's next envelope. */
    fun parse(answer: JsonObject): ReplayHeatmap? {
        fun find(node: JsonElement): JsonObject? = when (node) {
            is JsonObject -> {
                val entity = node["macroMarkersListEntity"] as? JsonObject
                if (entity?.get("markersList").obj()?.get("markerType").str() ==
                    "MARKER_TYPE_HEATMAP") entity
                else node.values.firstNotNullOfOrNull(::find)
            }
            is JsonArray -> node.firstNotNullOfOrNull(::find)
            else -> null
        }
        val markers = find(answer)?.get("markersList").obj()?.get("markers") as? JsonArray
            ?: return null
        data class Marker(val start: Long, val duration: Long, val intensity: Float)
        val parsed = markers.mapNotNull { it.obj()?.let { obj ->
            val start = obj["startMillis"].str()?.toLongOrNull() ?: return@mapNotNull null
            val len = obj["durationMillis"].str()?.toLongOrNull() ?: return@mapNotNull null
            val intensity = obj["intensityScoreNormalized"].str()?.toFloatOrNull()
                ?: return@mapNotNull null
            if (start < 0L || len <= 0L || !intensity.isFinite()) null
            else Marker(start, len, intensity.coerceIn(0f, 1f))
        }}.sortedBy { it.start }
        if (parsed.size < MIN_MARKERS || parsed.size > 1000) return null
        val length = parsed.map { it.duration }.sorted()[parsed.size / 2]
        if (length <= 0L) return null
        return ReplayHeatmap(length, parsed.map { it.intensity })
    }

    /**
     * Favor a sustained rise above the local listening trend, rather than the
     * first (naturally popular) seconds or a single isolated hot sample.
     */
    fun peakMs(curve: ReplayHeatmap, durationMs: Long = 0L): Long? {
        val length = durationMs.takeIf { it > 0L } ?: curve.durationMs
        val n = curve.intensity.size
        if (length < MIN_DURATION_MS || n < MIN_MARKERS || curve.segmentMs <= 0) return null
        val half = (4_500.0 / curve.segmentMs).roundToInt().coerceAtLeast(1)
        val smooth = FloatArray(n) { i ->
            ((i - half).coerceAtLeast(0)..(i + half).coerceAtMost(n - 1))
                .map { curve.intensity[it] }.average().toFloat()
        }
        val wide = (45_000.0 / curve.segmentMs).roundToInt().coerceAtLeast(3)
        val trend = FloatArray(n) { i ->
            val window = ((i - wide).coerceAtLeast(1)..(i + wide).coerceAtMost(n - 1))
                .map { smooth[it] }.sorted()
            window[window.size / 2]
        }
        fun lift(i: Int) = smooth[i] - trend[i]
        fun rise(i: Int) = lift(i) / trend[i].coerceAtLeast(0.05f)
        val indices = (0 until n).filter {
            val ms = it * curve.segmentMs
            ms >= maxOf(SKIP_HEAD_MS, 2 * curve.segmentMs) &&
                ms <= length - SKIP_TAIL_MS
        }
        val i = indices.maxByOrNull(::rise) ?: return null
        if (rise(i) < 0.08f || lift(i) < 0.03f) return null
        val threshold = rise(i) * 0.5f
        var start = i
        while (start - 1 in indices && rise(start - 1) >= threshold) start--
        return start * curve.segmentMs
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull
}
