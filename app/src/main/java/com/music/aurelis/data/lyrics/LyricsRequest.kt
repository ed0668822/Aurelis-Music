// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.lyrics

import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Diagnostics contain no lyric text, URLs, credentials or server response bodies. */
enum class LyricsFailureKind { NETWORK, TIMEOUT, RATE_LIMIT, HTTP, INVALID_RESPONSE, TOO_LARGE, DISABLED }

data class LyricsFailure(
    val kind: LyricsFailureKind,
    val httpCode: Int? = null,
    val retryAfterSeconds: Long? = null,
)

internal class LyricsRequestException(val failure: LyricsFailure) : IOException(failure.kind.name)

internal suspend fun requestLyricsBody(
    request: Request,
    client: OkHttpClient,
    maxBytes: Int = 1_048_576,
): String = suspendCancellableCoroutine { continuation ->
    val call = client.newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(
                if (e is LyricsRequestException) e else LyricsRequestException(LyricsFailure(
                    if (e is InterruptedIOException) LyricsFailureKind.TIMEOUT else LyricsFailureKind.NETWORK)))
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                val body = response.use {
                    if (!it.isSuccessful) throw LyricsRequestException(LyricsFailure(
                        if (it.code == 429) LyricsFailureKind.RATE_LIMIT else LyricsFailureKind.HTTP,
                        httpCode = it.code,
                        retryAfterSeconds = it.header("Retry-After")?.toLongOrNull()?.takeIf { value -> value >= 0 }?.coerceAtMost(86_400),
                    ))
                    val bytes = it.body?.byteStream()?.use { stream -> stream.readNBytes(maxBytes + 1) }
                        ?: throw LyricsRequestException(LyricsFailure(LyricsFailureKind.INVALID_RESPONSE))
                    if (bytes.size > maxBytes) throw LyricsRequestException(LyricsFailure(LyricsFailureKind.TOO_LARGE))
                    bytes.toString(Charsets.UTF_8)
                }
                if (continuation.isActive) continuation.resume(body)
            } catch (e: IOException) { onFailure(call, e) }
        }
    })
}
