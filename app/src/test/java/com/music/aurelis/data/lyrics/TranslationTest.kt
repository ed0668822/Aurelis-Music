package com.music.aurelis.data.lyrics

import com.music.aurelis.AurelisApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class TranslationTest {
    private val endpoint = "https://translation.example/service"
    private fun client(handler: (Request) -> String) = OkHttpClient.Builder().addInterceptor { chain ->
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
            .body(handler(chain.request()).toResponseBody("application/json".toMediaType())).build()
    }.build()
    private fun lines() = listOf(LyricLine(0, "Test one"), LyricLine(1000, "Test two"))
    @Test fun configuredApiKeepsLineOrderTimingsAndCacheWorksAfterMemoryEviction() = runTest {
        LyricsTranslation.cacheDir = RuntimeEnvironment.getApplication().cacheDir
        var calls = 0
        val client = client { request ->
            calls++
            assertEquals("/service/translate", request.url.encodedPath)
            val buffer = okio.Buffer(); request.body!!.writeTo(buffer)
            val data = JSONObject(buffer.readUtf8())
            assertEquals("auto", data.getString("source")); assertEquals("es", data.getString("target"))
            assertEquals("Test one", data.getJSONArray("q").getString(0))
            """{"translatedText":["Prueba uno","Prueba dos"],"detectedLanguage":["en","en"]}"""
        }
        val result = LyricsTranslation.translate("disk-first", lines(), "es", endpoint, "auto", httpClient = client) as LyricsTranslation.Result.Translated
        assertEquals(listOf("Prueba uno", "Prueba dos"), result.lines.map { it.text })
        assertEquals(listOf(0L, 1000L), result.lines.map { it.timeMs }); assertFalse(result.fromCache)
        repeat(13) { LyricsTranslation.translate("other-$it", lines(), "es", endpoint, "auto", httpClient = client) }
        val cached = LyricsTranslation.translate("disk-first", lines(), "es", endpoint, "auto", allowNetwork = false, httpClient = client) as LyricsTranslation.Result.Translated
        assertTrue(cached.fromCache); assertEquals(14, calls)
    }
    @Test fun contentLanguageAndServerChangesCannotReuseAnotherTranslation() = runTest {
        val client = client { """{"translatedText":["Prueba uno","Prueba dos"]}""" }
        LyricsTranslation.translate("versioned", lines(), "es", endpoint, "en", httpClient = client)
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("versioned", lines().map { it.copy(text = it.text + " changed") }, "es", endpoint, "en", false, client))
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("versioned", lines(), "fr", endpoint, "en", false, client))
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("versioned", lines(), "es", "https://other.example", "en", false, client))
    }
    @Test fun partialMalformedAndUnavailableResponsesNeverBecomeTranslations() = runTest {
        for ((index, body) in listOf("{}", "not json", """{"translatedText":["only one"]}""").withIndex()) {
            assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("bad-$index", lines(), "es", endpoint, "en", httpClient = client { body }))
        }
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("offline", lines(), "es", endpoint, "en", httpClient = client { throw IOException("offline fixture") }))
    }
    @Test fun sameLanguageIsExplicitAndDoesNotSendLyrics() = runTest {
        val client = client { fail("Same-language text must not be sent"); "" }
        assertEquals(LyricsTranslation.Result.SameLanguage("es"), LyricsTranslation.translate("same", lines(), "es", endpoint, "es", httpClient = client))
    }
    @Test fun unsafeEndpointCredentialsAndUnsupportedTargetsAreRejected() = runTest {
        for (url in listOf("http://example.org", "https://user:secret@example.org", "https://example.org?key=secret", "not a URL")) assertNull(LyricsTranslation.normaliseEndpoint(url))
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("bad-target", lines(), "xx", endpoint, "auto"))
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("no-server", lines(), "es", "", "auto"))
    }
    @Test fun cancellationPropagatesAndDoesNotPersistPartialResults() = runTest {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val client = client { entered.complete(Unit); runBlocking { release.await() }; """{"translatedText":["Fixture A","Fixture B"]}""" }
        val job = launch { LyricsTranslation.translate("cancelled", lines(), "es", endpoint, "en", httpClient = client) }
        try { entered.await(); job.cancelAndJoin(); assertTrue(job.isCancelled) }
        finally { release.complete(Unit) }
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("cancelled", lines(), "es", endpoint, "en", allowNetwork = false))
    }
}
