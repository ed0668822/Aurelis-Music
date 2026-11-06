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
        for ((index, body) in listOf("{}", "not json", """{"translatedText":["only one"]}""", """{"translatedText":[42,{}]}""", """{"translatedText":["", " "]}""").withIndex()) {
            assertEquals(LyricsTranslation.Result.Failed(LyricsFailure(LyricsFailureKind.INVALID_RESPONSE)), LyricsTranslation.translate("bad-$index", lines(), "es", endpoint, "en", httpClient = client { body }))
        }
        assertEquals(LyricsTranslation.Result.Failed(LyricsFailure(LyricsFailureKind.NETWORK)), LyricsTranslation.translate("offline", lines(), "es", endpoint, "en", httpClient = client { throw IOException("offline fixture") }))
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
    @Test fun rateLimitTimeoutAndHttpErrorsAreExplicitAndNeverCached() = runTest {
        for ((name, code) in listOf("rate" to 429, "http" to 503)) {
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                    .header("Retry-After", "9").body("{}".toResponseBody("application/json".toMediaType())).build()
            }.build()
            val result = LyricsTranslation.translate("b08-$name", lines(), "es", endpoint, "en", httpClient = http) as LyricsTranslation.Result.Failed
            assertEquals(code, result.failure.httpCode)
            assertEquals(if (code == 429) LyricsFailureKind.RATE_LIMIT else LyricsFailureKind.HTTP, result.failure.kind)
            if (code == 429) assertEquals(9L, result.failure.retryAfterSeconds)
            assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("b08-$name", lines(), "es", endpoint, "en", false))
        }
        val timeout = LyricsTranslation.translate("b08-timeout", lines(), "es", endpoint, "en",
            httpClient = client { throw java.net.SocketTimeoutException("fixture") }) as LyricsTranslation.Result.Failed
        assertEquals(LyricsFailureKind.TIMEOUT, timeout.failure.kind)
    }
    @Test fun inputAndResponseSizeLimitsFailWithoutPartialTranslations() = runTest {
        var calls = 0
        val noSend = client { calls++; "{}" }
        val excessive = List(501) { LyricLine(it * 1000L, "Fixture") }
        val tooLong = listOf(LyricLine(0, "x".repeat(3501)))
        for ((id, value) in listOf("b08-count" to excessive, "b08-length" to tooLong)) {
            val result = LyricsTranslation.translate(id, value, "es", endpoint, "en", httpClient = noSend) as LyricsTranslation.Result.Failed
            assertEquals(LyricsFailureKind.TOO_LARGE, result.failure.kind)
        }
        assertEquals(0, calls)
        val result = LyricsTranslation.translate("b08-response-size", lines(), "es", endpoint, "en",
            httpClient = client { "x".repeat(524_289) }) as LyricsTranslation.Result.Failed
        assertEquals(LyricsFailureKind.TOO_LARGE, result.failure.kind)
    }
    @Test fun translationKeepsBackgroundVoicesTimingsAndOriginalText() = runTest {
        val original = listOf(LyricLine(1000, "Lead", listOf(LyricWord(1000, 2500, "Lead")),
            background = LyricLine(1500, "Backing", listOf(LyricWord(1500, 2800, "Backing")))))
        val result = LyricsTranslation.translate("b08-voices", original, "es", endpoint, "en", httpClient = client {
            """{"translatedText":["Voz principal","Coro"]}"""
        }) as LyricsTranslation.Result.Translated
        assertEquals("Lead", original.single().text); assertEquals("Backing", original.single().background!!.text)
        val translated = result.lines.single()
        assertEquals("Voz principal", translated.text); assertEquals("Coro", translated.background!!.text)
        assertEquals(1000L, translated.timeMs); assertEquals(1500L, translated.background!!.timeMs)
        assertEquals(original.single(), translated.timingSource)
        assertEquals(original.single().background, translated.background!!.timingSource)
        assertEquals(2500L, translated.words.single().endMs)
    }
    @Test fun batchesUseAtMostTwoConcurrentRequestsAndKeepOrder() = runTest {
        val entered = java.util.concurrent.CountDownLatch(2)
        val active = java.util.concurrent.atomic.AtomicInteger()
        val peak = java.util.concurrent.atomic.AtomicInteger()
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val original = List(9) { LyricLine(it * 1000L, "$it:" + "x".repeat(998)) }
        val http = client { request ->
            val now = active.incrementAndGet(); peak.updateAndGet { maxOf(it, now) }; calls.incrementAndGet()
            try {
                entered.countDown(); assertTrue("First two batches should overlap", entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
                val buffer = okio.Buffer(); request.body!!.writeTo(buffer)
                val inputs = JSONObject(buffer.readUtf8()).getJSONArray("q")
                val output = org.json.JSONArray()
                repeat(inputs.length()) { output.put("Traducida " + inputs.getString(it).substringBefore(':')) }
                JSONObject().put("translatedText", output).toString()
            } finally { active.decrementAndGet() }
        }
        val result = LyricsTranslation.translate("b08-parallel", original, "es", endpoint, "en", httpClient = http) as LyricsTranslation.Result.Translated
        assertEquals(2, peak.get()); assertEquals(3, calls.get())
        assertEquals(List(9) { "Traducida $it" }, result.lines.map { it.text })
    }
    @Test fun aSuccessfulTranslationSurvivesUnwritableDiskCache() = runTest {
        val previous = LyricsTranslation.cacheDir
        val file = java.io.File(RuntimeEnvironment.getApplication().cacheDir, "b08-not-a-directory").apply { writeText("fixture") }
        try {
            LyricsTranslation.cacheDir = file
            val result = LyricsTranslation.translate("b08-cache-failure", lines(), "es", endpoint, "en", httpClient = client {
                """{"translatedText":["Frase uno","Frase dos"]}"""
            }) as LyricsTranslation.Result.Translated
            assertFalse(result.fromCache); assertEquals(listOf("Frase uno", "Frase dos"), result.lines.map { it.text })
        } finally { LyricsTranslation.cacheDir = previous; file.delete() }
    }

    @Test fun failingOneBatchDoesNotStoreTheSuccessfulPartialBatch() = runTest {
        val original = List(6) { LyricLine(it * 1000L, "$it:" + "x".repeat(998)) }
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
            val inputs = JSONObject(buffer.readUtf8()).getJSONArray("q")
            val failing = inputs.getString(0).startsWith("3:")
            val output = org.json.JSONArray()
            repeat(inputs.length()) { output.put("Fixture " + inputs.getString(it).substringBefore(':')) }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if (failing) 429 else 200).message("fixture")
                .body(JSONObject().put("translatedText", output).toString().toResponseBody("application/json".toMediaType())).build()
        }.build()
        val result = LyricsTranslation.translate("b08-partial", original, "es", endpoint, "en", httpClient = http) as LyricsTranslation.Result.Failed
        assertEquals(LyricsFailureKind.RATE_LIMIT, result.failure.kind)
        assertEquals(LyricsTranslation.Result.Unavailable, LyricsTranslation.translate("b08-partial", original, "es", endpoint, "en", false))
    }

}
