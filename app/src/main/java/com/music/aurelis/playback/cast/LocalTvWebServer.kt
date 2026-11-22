package com.music.aurelis.playback.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
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

/** Locates a Wi-Fi/Ethernet address usable by a television on the same LAN. */
internal object TvLan {
    fun address(context: Context): Inet4Address? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val networks = (listOfNotNull(manager.activeNetwork) + manager.allNetworks).distinct()
        for (network in networks) {
            val capabilities = manager.getNetworkCapabilities(network) ?: continue
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) continue
            val links = manager.getLinkProperties(network) ?: continue
            val ipv4 = links.linkAddresses.map { it.address }.filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isAnyLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress }
            if (ipv4 != null) return ipv4
        }
        return null
    }
}

/**
 * Browser TV pairing endpoint. A random session URL contains every authority
 * needed to read a track or submit a control event. Never expose this over WAN.
 * Responses and request bodies are capped; no arbitrary file paths are accepted.
 */
internal class LocalTvWebServer(
    private val address: Inet4Address,
    private val page: ByteArray,
    private val status: () -> String,
    private val lyrics: () -> String,
    private val onEvent: (String) -> Unit,
) : Closeable {
    private val random = SecureRandom()
    private val token = ByteArray(24).also(random::nextBytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private val connections = ConcurrentHashMap.newKeySet<Socket>()
    private var listener: ServerSocket? = null
    private var pool: ThreadPoolExecutor? = null

    @Synchronized
    fun start(): String? {
        listener?.let { return "http://${address.hostAddress}:${it.localPort}/$token/" }
        val server = runCatching { ServerSocket(0, 8, address) }.getOrNull() ?: return null
        val executor = ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(8),
            { thread -> Thread(thread, "AurelisWebTv").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy())
        listener = server
        pool = executor
        Thread({
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: Exception) { break }
                connections.add(socket)
                try {
                    executor.execute {
                        try { serve(socket) } finally {
                            connections.remove(socket)
                            runCatching { socket.close() }
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    connections.remove(socket)
                    runCatching { socket.close() }
                }
            }
        }, "AurelisWebTvAccept").apply { isDaemon = true; start() }
        return "http://${address.hostAddress}:${server.localPort}/$token/"
    }

    private fun serve(socket: Socket) {
        try {
            socket.soTimeout = 8000
            val input = socket.getInputStream()
            val output = BufferedOutputStream(socket.getOutputStream())
            val request = readLine(input)?.split(' ') ?: return
            if (request.size != 3 || !request[2].startsWith("HTTP/1.")) {
                respond(output, "400 Bad Request"); return
            }
            var length = 0
            var complete = false
            var origin: String? = null
            for (i in 0 until 32) {
                val header = readLine(input) ?: return
                if (header.isEmpty()) { complete = true; break }
                if (header.startsWith("Content-Length:", true)) {
                    length = header.substringAfter(':').trim().toIntOrNull() ?: -1
                }
                if (header.startsWith("Origin:", true)) origin = header.substringAfter(':').trim()
            }
            if (!complete || length !in 0..2048) { respond(output, "400 Bad Request"); return }
            val uri = request[1]
            val prefix = "/$token/"
            if (!uri.startsWith(prefix) || uri.contains("..")) { respond(output, "404 Not Found"); return }
            val path = uri.removePrefix(prefix).substringBefore('?')
            val method = request[0]
            if (method == "GET" && path.isEmpty()) {
                respond(output, "200 OK", "text/html; charset=utf-8", page)
            } else if (method == "GET" && path == "state") {
                respond(output, "200 OK", "application/json; charset=utf-8",
                    status().toByteArray(StandardCharsets.UTF_8))
            } else if (method == "GET" && path == "lyrics") {
                respond(output, "200 OK", "application/json; charset=utf-8",
                    lyrics().toByteArray(StandardCharsets.UTF_8))
            } else if (method == "POST" && path == "event") {
                val expectedOrigin = "http://${address.hostAddress}:${listener?.localPort}"
                if (origin != null && origin != expectedOrigin) {
                    respond(output, "403 Forbidden"); return
                }
                val bytes = ByteArray(length)
                var done = 0
                while (done < length) {
                    val n = input.read(bytes, done, length - done)
                    if (n <= 0) return
                    done += n
                }
                onEvent(String(bytes, StandardCharsets.UTF_8))
                respond(output, "204 No Content")
            } else {
                respond(output, "404 Not Found")
            }
        } catch (e: Exception) {
            Log.d("AurelisWebTv", "TV browser client disconnected: ${e.javaClass.simpleName}")
        }
    }

    private fun readLine(input: java.io.InputStream): String? {
        val line = ByteArrayOutputStream()
        while (line.size() < 4096) {
            val b = input.read()
            if (b < 0) return null
            if (b == 10) return line.toString("UTF-8").trimEnd('\r')
            line.write(b)
        }
        return null
    }

    private fun respond(
        output: BufferedOutputStream, code: String,
        mime: String = "text/plain; charset=utf-8", body: ByteArray = byteArrayOf(),
    ) {
        val header = "HTTP/1.1 $code\r\nContent-Type: $mime\r\n" +
            "Content-Length: ${body.size}\r\nCache-Control: no-store\r\n" +
            "X-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\n" +
            "Content-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src http: https: data:; media-src http: https: blob:; connect-src 'self'\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    @Synchronized
    override fun close() {
        runCatching { listener?.close() }
        listener = null
        connections.forEach { runCatching { it.close() } }
        connections.clear()
        pool?.shutdownNow()
        pool = null
    }
}
