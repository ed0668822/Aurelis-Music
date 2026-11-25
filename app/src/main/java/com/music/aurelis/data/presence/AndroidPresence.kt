// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.data.presence

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.music.aurelis.data.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.random.Random

internal object AndroidPresence {
    private const val ENDPOINT = "wss://aurelis-presence.ed0668822.workers.dev/v1/presence"
    private const val INITIAL_RETRY_MS = 5_000L
    private const val MAX_RETRY_MS = 120_000L

    // This client deliberately has no account cookies or auth interceptors.
    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(false)
            .pingInterval(60, TimeUnit.SECONDS)
            .build()
    }

    fun install() {
        val owner = ProcessLifecycleOwner.get()
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppSettings.shareLivePresence.collectLatest { enabled ->
                    if (!enabled) return@collectLatest

                    var retryMs = INITIAL_RETRY_MS
                    while (isActive) {
                        val startedAt = SystemClock.elapsedRealtime()
                        val retryAfterMs = keepConnectionOpen()
                        if (SystemClock.elapsedRealtime() - startedAt >= MAX_RETRY_MS) {
                            retryMs = INITIAL_RETRY_MS
                        }
                        delay(maxOf(retryMs, retryAfterMs) + Random.nextLong(1_000, 5_001))
                        retryMs = (retryMs * 2).coerceAtMost(MAX_RETRY_MS)
                    }
                }
            }
        }
    }

    private suspend fun keepConnectionOpen(): Long = suspendCancellableCoroutine { continuation ->
        val opened = AtomicBoolean()
        val socket = client.newWebSocket(
            Request.Builder().url(ENDPOINT).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    opened.set(true)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    continuation.resume(0L)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val retryAfterSeconds = response?.header("Retry-After")?.toLongOrNull() ?: 0L
                    continuation.resume(retryAfterSeconds.coerceIn(0L, 300L) * 1_000L)
                }
            },
        )
        continuation.invokeOnCancellation {
            if (!opened.get() || !socket.close(1000, null)) socket.cancel()
        }
    }
}
