// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LyricsProvidersTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private fun song() = LocalSong("b08", "Luna & Sol", "Artista", "Álbum", "content://fixture/b08", null, 60_000, null, 1, 2026)
    private fun client(code: Int = 200, body: String = "[]", handler: (Interceptor.Chain) -> Unit = {}) =
        OkHttpClient.Builder().addInterceptor { chain ->
            handler(chain)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .header("Retry-After", "12").body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
    private fun hit(synced: String? = "[00:00.000]Letra propia", plain: String? = null) = JSONArray().put(JSONObject()
        .put("trackName", "Luna & Sol").put("artistName", "Artista").put("duration", 60)
        .put("syncedLyrics", synced).put("plainLyrics", plain)).toString()
    private suspend fun failure(block: suspend () -> Unit): LyricsFailure = try {
        block(); error("Expected a typed lookup failure")
    } catch (error: LyricsRequestException) { error.failure }

    @Test fun registryOffersOnlyApprovedSourcesAndSavedPreferencesCannotReactivateOthers() {
        assertEquals(listOf(LyricsSource.LRCLIB), LyricsSource.offered)
        assertEquals(listOf(LyricsSource.LRCLIB), LyricsSource.ordered(LyricsSource.entries.reversed()))
        val previous = SettingsRepository.lyricsSources.value
        try {
            SettingsRepository.setLyricsSources(LyricsSource.entries.toSet())
            SettingsRepository.init(app)
            assertEquals(setOf(LyricsSource.LRCLIB), SettingsRepository.lyricsSources.value)
            SettingsRepository.setLyricsSources(emptySet())
            SettingsRepository.init(app)
            assertTrue(SettingsRepository.lyricsSources.value.isEmpty())
        } finally { SettingsRepository.setLyricsSources(previous) }
    }
    @Test fun disabledProvidersAreRejectedEvenWhenExplicitlyRequested() = runTest {
        for (source in LyricsSource.entries.filterNot { it == LyricsSource.LRCLIB }) {
            assertEquals(LyricsFailureKind.DISABLED, failure { LyricsProviderRegistry.lookup(song(), source) }.kind)
        }
        assertNull(LyricsRepository.lyrics("Fixture", "Artist", 60_000,
            sources = setOf(LyricsSource.BETTER_LYRICS, LyricsSource.BINI_LYRICS, LyricsSource.LRC_RED)))
    }
    @Test fun explicitLrclibRequestEncodesMetadataAndKeepsSyncAtZero() = runTest {
        val found = LrcLib.lookup(song().title, song().artist, song().durationMs, client(body = hit()) { chain ->
            val request = chain.request()
            assertEquals("https", request.url.scheme); assertEquals("lrclib.net", request.url.host)
            assertEquals("Luna & Sol", request.url.queryParameter("track_name"))
            assertEquals("Artista", request.url.queryParameter("artist_name"))
            assertEquals(setOf("track_name", "artist_name"), request.url.queryParameterNames)
            assertTrue(request.header("User-Agent")!!.contains("Aurelis"))
        })!!
        assertTrue(found.isSynced); assertEquals(0L, found.lines.single().timeMs)
        assertEquals("Letra propia", found.lines.single().text)
    }
    @Test fun plainLyricsRemainUntimedAndWrongRecordingIsNotAccepted() = runTest {
        val found = LrcLib.lookup("Luna & Sol", "Artista", 60_000, client(body = hit(null, "Texto propio\nOtra frase")))!!
        assertFalse(found.isSynced); assertTrue(found.lines.all { it.timeMs == 0L })
        assertNull(LrcLib.lookup("Luna & Sol", "Otro", 60_000, client(body = hit())))
        assertNull(LrcLib.lookup("Luna & Sol", "Artista", 90_000, client(body = hit())))
    }
    @Test fun notFoundIsDifferentFromHttpNetworkTimeoutAndRateLimit() = runTest {
        assertNull(LrcLib.lookup("Fixture", "Artista", 60_000, client(404)))
        assertNull(LrcLib.lookup("Fixture", "Artista", 60_000, client(body = "[]")))
        val rate = failure { LrcLib.lookup("Fixture", "Artista", 60_000, client(429)) }
        assertEquals(LyricsFailureKind.RATE_LIMIT, rate.kind); assertEquals(12L, rate.retryAfterSeconds)
        assertEquals(503, failure { LrcLib.lookup("Fixture", "Artista", 60_000, client(503)) }.httpCode)
        assertEquals(LyricsFailureKind.NETWORK, failure { LrcLib.lookup("Fixture", "Artista", 60_000, client { throw IOException("offline fixture") }) }.kind)
        assertEquals(LyricsFailureKind.TIMEOUT, failure { LrcLib.lookup("Fixture", "Artista", 60_000, client { throw SocketTimeoutException("fixture") }) }.kind)
    }
    @Test fun malformedAndOversizedResponsesFailBeforeBeingStored() = runTest {
        assertEquals(LyricsFailureKind.INVALID_RESPONSE, failure { LrcLib.lookup("Fixture", "Artist", 60_000, client(body = "not JSON")) }.kind)
        assertEquals(LyricsFailureKind.TOO_LARGE, failure { LrcLib.lookup("Fixture", "Artist", 60_000, client(body = "x".repeat(1_048_577))) }.kind)
    }
    @Test fun cancellationCancelsHttpCallAndNeverBecomesNotFound() = runTest {
        val entered = CompletableDeferred<Call>(); val release = CompletableDeferred<Unit>()
        val http = client(body = hit()) { chain -> entered.complete(chain.call()); runBlocking { release.await() } }
        var completed = false
        val job = launch { LrcLib.lookup("Luna & Sol", "Artista", 60_000, http); completed = true }
        try {
            val call = entered.await(); job.cancelAndJoin()
            assertTrue(call.isCanceled()); assertFalse(completed)
        } finally { release.complete(Unit) }
    }
    @Test fun remoteErrorDoesNotPreventLocalCacheOrOverwriteIt() = runTest {
        val requests = AtomicInteger()
        val repo = LocalLyricsRepository(app) {
            requests.incrementAndGet(); LyricsRepository.Result(LyricsSource.LRCLIB, listOf(LyricLine(1000, "Fixture local")))
        }
        val unique = song().copy(id = "b08-cache", contentUri = "content://fixture/b08-cache")
        assertNull(repo.find(unique)); assertEquals(0, requests.get())
        assertFalse(repo.find(unique, true)!!.cached)
        val offline = LocalLyricsRepository(app) { error("Cache must precede any network lookup") }
        val found = offline.find(unique, true)!!
        assertTrue(found.cached); assertEquals("Fixture local", found.lines.single().text)
        assertEquals(1, requests.get())
    }
}
