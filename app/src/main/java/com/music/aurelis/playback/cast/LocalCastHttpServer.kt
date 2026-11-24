// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * A short-lived HTTP endpoint for media already authorized for playback by the phone.
 *
 * This is not a general file server: callers register openers, and requests only
 * contain random capability identifiers. It binds to one LAN interface, not 0.0.0.0,
 * and is stopped with the Cast session. No MediaSession or second audio player.
 *
 * Only seekable, known-length media may be registered. The opener positions its
 * stream at the supplied byte offset, so HTTP Range seeks never require copying
 * the entire file through memory.
 */
internal class LocalCastHttpServer(private val bindAddress: Inet4Address) : Closeable {
    data class MediaResource(
        val length: Long,
        val mimeType: String,
        val openAt: (Long) -> InputStream,
    )

    data class ByteSpan(val start: Long, val endInclusive: Long) {
        val length: Long get() = endInclusive - start + 1

        companion object {
            fun parse(header: String, size: Long): ByteSpan? {
                if (size <= 0) return null
                val match = RANGE.matchEntire(header.trim()) ?: return null
                val startText = match.groupValues[1]
                val endText = match.groupValues[2]
                if (startText.isEmpty() && endText.isEmpty()) return null
                if (startText.isEmpty()) {
                    val suffix = endText.toLongOrNull()?.takeIf { it > 0 } ?: return null
                    return ByteSpan((size - suffix).coerceAtLeast(0), size - 1)
                }
                val start = startText.toLongOrNull()?.takeIf { it >= 0 && it < size } ?: return null
                val end = if (endText.isEmpty()) {
                    size - 1
                } else {
                    endText.toLongOrNull()?.takeIf { it >= start }?.coerceAtMost(size - 1)
                        ?: return null
                }
                return ByteSpan(start, end)
            }
        }
    }

    private val random = SecureRandom()
    private val resources = LinkedHashMap<String, MediaResource>()
    private val connections = ConcurrentHashMap.newKeySet<Socket>()
    private var listener: ServerSocket? = null
    private var workers: ThreadPoolExecutor? = null
    private var sessionKey = newKey()

    /** Registers one media source and returns an unguessable LAN URL. */
    @Synchronized
    fun publish(media: MediaResource): String? {
        if (media.length <= 0) return null
        val socket = startIfNeeded() ?: return null
        val key = newKey()
        resources[key] = media
        // Only the current and a few preloaded queue entries need to be held.
        while (resources.size > MAX_RESOURCES) {
            val oldest = resources.keys.first()
            resources.remove(oldest)
        }
        return "http://${bindAddress.hostAddress}:${socket.localPort}/$sessionKey/$key"
    }

    @Synchronized
    private fun startIfNeeded(): ServerSocket? {
        listener?.let { return it }
        val server = try {
            ServerSocket(0, 8, bindAddress)
        } catch (_: Exception) {
            return null
        }
        val pool = ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8),
            { action -> Thread(action, "AurelisLocalCastHttp").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy(),
        )
        sessionKey = newKey()
        listener = server
        workers = pool
        Thread({
            while (!server.isClosed) {
                val peer = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                connections.add(peer)
                try {
                    pool.execute {
                        try {
                            serve(peer)
                        } finally {
                            connections.remove(peer)
                            runCatching { peer.close() }
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    connections.remove(peer)
                    runCatching { peer.close() }
                }
            }
        }, "AurelisLocalCastAccept").apply { isDaemon = true; start() }
        return server
    }

    private fun serve(socket: Socket) {
        try {
            socket.soTimeout = SOCKET_TIMEOUT_MS
            val input = socket.getInputStream()
            val output = BufferedOutputStream(socket.getOutputStream())
            val first = readLine(input) ?: return
            val request = first.split(' ')
            if (request.size != 3 || !request[2].startsWith("HTTP/1.")) {
                respond(output, "400 Bad Request")
                return
            }
            var range: String? = null
            var completeHeaders = false
            for (index in 0 until MAX_HEADERS) {
                val header = readLine(input) ?: return
                if (header.isEmpty()) {
                    completeHeaders = true
                    break
                }
                if (header.startsWith("Range:", ignoreCase = true)) {
                    if (range != null) {
                        respond(output, "400 Bad Request")
                        return
                    }
                    range = header.substringAfter(':').trim()
                }
            }
            if (!completeHeaders) {
                respond(output, "431 Request Header Fields Too Large")
                return
            }
            val method = request[0]
            if (method != "GET" && method != "HEAD") {
                respond(output, "405 Method Not Allowed", listOf("Allow" to "GET, HEAD"))
                return
            }
            val segments = request[1].split('/')
            val media = synchronized(this) {
                if (segments.size == 3 && segments[0].isEmpty() &&
                    segments[1] == sessionKey) resources[segments[2]] else null
            }
            if (media == null) {
                respond(output, "404 Not Found")
                return
            }
            val span = if (range == null) {
                ByteSpan(0, media.length - 1)
            } else {
                ByteSpan.parse(range!!, media.length) ?: run {
                    respond(output, "416 Range Not Satisfiable", listOf("Content-Range" to "bytes */${media.length}"))
                    return
                }
            }
            val mime = media.mimeType.takeIf { SAFE_MIME.matches(it) } ?: "application/octet-stream"
            val headers = mutableListOf(
                "Content-Type" to mime,
                "Accept-Ranges" to "bytes",
                "Content-Length" to span.length.toString(),
            )
            if (range != null) headers += "Content-Range" to "bytes ${span.start}-${span.endInclusive}/${media.length}"
            if (method == "HEAD") {
                respond(output, if (range == null) "200 OK" else "206 Partial Content", headers)
                return
            }
            media.openAt(span.start).use { stream ->
                respond(output, if (range == null) "200 OK" else "206 Partial Content", headers, flush = false)
                val buffer = ByteArray(16 * 1024)
                var remaining = span.length
                while (remaining > 0) {
                    val read = stream.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    remaining -= read
                }
                output.flush()
            }
        } catch (_: Exception) {
            // A disconnected receiver or unreadable source must not crash the service.
        }
    }

    private fun respond(
        output: BufferedOutputStream,
        status: String,
        headers: List<Pair<String, String>> = emptyList(),
        flush: Boolean = true,
    ) {
        val message = buildString {
            append("HTTP/1.1 ").append(status).append("\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            for ((name, value) in headers) append(name).append(": ").append(value).append("\r\n")
            if (headers.none { it.first == "Content-Length" }) append("Content-Length: 0\r\n")
            append("\r\n")
        }
        output.write(message.toByteArray(StandardCharsets.US_ASCII))
        if (flush) output.flush()
    }

    private fun readLine(input: InputStream): String? {
        val bytes = ByteArrayOutputStream(128)
        while (bytes.size() < MAX_LINE_BYTES) {
            val c = input.read()
            if (c < 0) return null
            if (c == 10) return bytes.toString(StandardCharsets.US_ASCII.name()).trimEnd('\r')
            bytes.write(c)
        }
        throw IllegalArgumentException("Oversized HTTP request line")
    }

    private fun newKey(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    @Synchronized
    override fun close() {
        runCatching { listener?.close() }
        listener = null
        connections.forEach { runCatching { it.close() } }
        connections.clear()
        workers?.shutdownNow()
        workers = null
        resources.clear()
        sessionKey = newKey()
    }

    private companion object {
        val RANGE = Regex("^bytes=(\\d*)-(\\d*)$")
        val SAFE_MIME = Regex("^[A-Za-z0-9.+_-]+/[A-Za-z0-9.+_-]+$")
        const val MAX_RESOURCES = 48
        const val MAX_HEADERS = 32
        const val MAX_LINE_BYTES = 4096
        const val SOCKET_TIMEOUT_MS = 15_000
    }
}
