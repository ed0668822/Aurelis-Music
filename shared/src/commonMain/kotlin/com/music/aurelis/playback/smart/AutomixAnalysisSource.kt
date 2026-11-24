// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.smart

/**
 * Keeps Automix analysis on the canonical YouTube Opus rendition.
 *
 * Playback may deliberately use a source substitute (including JioSaavn), but
 * its cache entry is not interchangeable with the YouTube recording that the
 * analysis fetcher owns. The plain video id is the cache key pinned to that
 * YouTube copy; suffixed keys (`#alt`, `#hifi`, ...) belong to substitutes or
 * upgrades.
 */
 object AutomixAnalysisSource {
    const val OPUS_QUERY_PARAMETER = "automix_opus"

    fun isCanonicalYouTubeRendition(videoId: String?, cacheKey: String): Boolean =
        videoId == null || cacheKey == videoId

    /**
     * A private playback URI for analysis-only reads.
     *
     * A normal `watch?v=` URI is allowed to reuse the source race already
     * pinned for audible playback. That is exactly the wrong thing for
     * Automix: its independently cached copy must always be YouTube Opus,
     * even while playback is JioSaavn or a lossless upgrade.
     */
    fun opusUri(videoId: String): String = "aurelis://watch?v=$videoId&$OPUS_QUERY_PARAMETER=1"

    fun requestsYouTubeOpus(marker: String?): Boolean = marker == "1"

    /**
     * Source-backed media IDs were previously analysed from a YouTube title
     * match. Those saved grids have different sample timelines and must never
     * be restored as if measured from the source's own bytes.
     *
     * The cache key identifies the exact source and track, independent of
     * changing title/artist query hints. YouTube keeps its existing store key.
     */
    fun storageKey(trackId: String, sourceCacheKey: String?): String =
        if (sourceCacheKey.isNullOrBlank()) trackId else "$trackId|source:$sourceCacheKey"

    /** Namespace exact playback cache entries away from legacy analysis-only entries. */
    fun playbackStorageKey(trackId: String, cacheKey: String): String =
        storageKey(trackId, "playback:$cacheKey")

    /** A local recording must not borrow the original video's YouTube analysis. */
    fun localStorageKey(trackId: String, localUri: String, revision: String? = null): String =
        storageKey(trackId, "local:${localUri.length}:$localUri:${revision.orEmpty()}")

    /**
     * A loop deck is safe when the playback rendition is complete and the
     * caller obtained its analysis under the *same* rendition-scoped key.
     * A substituted FLAC (#alt) may be analysed and mixed, but never with
     * an old canonical Opus result.
     */
    fun hasVerifiedLoopAudio(
        playingCacheKey: String,
        completeRenditionKeys: Collection<String>,
    ): Boolean = playingCacheKey in completeRenditionKeys
}
