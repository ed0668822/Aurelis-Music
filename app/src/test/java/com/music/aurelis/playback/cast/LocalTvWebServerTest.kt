package com.music.aurelis.playback.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class LocalTvWebServerTest {
    private val loopback = InetAddress.getByName("127.0.0.1") as Inet4Address

    private fun open(url: String, method: String = "GET", body: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 3000
            readTimeout = 3000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }

    @Test fun onlyServesSessionScopedPageAndState() {
        val receiver = LocalTvWebServer(
            loopback, "<h1>Aurelis TV</h1>".toByteArray(), { "{\"status\":\"ok\"}" },
            { "{\"lyrics\":[]}" }, {},
        )
        receiver.use {
            val address = requireNotNull(receiver.start())
            assertEquals(200, open(address).responseCode)
            val status = open(address + "state")
            assertEquals(200, status.responseCode)
            assertEquals("{\"status\":\"ok\"}", status.inputStream.bufferedReader().readText())
            val lyrics = open(address + "lyrics")
            assertEquals(200, lyrics.responseCode)
            assertTrue(lyrics.inputStream.bufferedReader().readText().contains("lyrics"))
            val invalid = open(address.substringBeforeLast('/') + "/untrusted")
            assertEquals(404, invalid.responseCode)
        }
    }

    @Test fun acceptsBoundedPairedEventsAndRejectsUnknownRoutes() {
        val received = AtomicReference<String?>(null)
        LocalTvWebServer(loopback, "tv".toByteArray(), { "{}" }, { "{}" },
            { received.set(it) }).use { receiver ->
            val address = requireNotNull(receiver.start())
            val body = "{\"action\":\"playing\",\"positionMs\":1000}"
            assertEquals(204, open(address + "event", "POST", body).responseCode)
            assertEquals(body, received.get())
            assertEquals(404, open(address + "missing").responseCode)
            assertFalse(address.contains("127.0.0.1:0/"))
        }
    }
}
