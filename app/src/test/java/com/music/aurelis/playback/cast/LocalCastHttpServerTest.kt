// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URL

class LocalCastHttpServerTest {
    private val loopback = InetAddress.getByName("127.0.0.1") as Inet4Address
    private val bytes = "0123456789abcdef".toByteArray(Charsets.US_ASCII)

    private fun source() = LocalCastHttpServer.MediaResource(bytes.size.toLong(), "audio/mpeg") { offset ->
        ByteArrayInputStream(bytes).apply { skip(offset) }
    }

    private fun connection(url: String, method: String = "GET", range: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 3000
            readTimeout = 3000
            if (range != null) setRequestProperty("Range", range)
        }

    @Test
    fun servesFullMediaAndSupportsHead() {
        LocalCastHttpServer(loopback).use { server ->
            val url = requireNotNull(server.publish(source()))
            connection(url).let { response ->
                try {
                    assertEquals(200, response.responseCode)
                    assertEquals("bytes", response.getHeaderField("Accept-Ranges"))
                    assertEquals("audio/mpeg", response.contentType)
                    assertArrayEquals(bytes, response.inputStream.use { it.readBytes() })
                } finally {
                    response.disconnect()
                }
            }
            connection(url, "HEAD").let { response ->
                try {
                    assertEquals(200, response.responseCode)
                    assertEquals(bytes.size.toString(), response.getHeaderField("Content-Length"))
                } finally {
                    response.disconnect()
                }
            }
        }
    }

    @Test
    fun supportsExplicitOpenAndSuffixRanges() {
        LocalCastHttpServer(loopback).use { server ->
            val url = requireNotNull(server.publish(source()))
            val checks = listOf(
                "bytes=3-6" to "3456",
                "bytes=12-" to "cdef",
                "bytes=-3" to "def",
                "bytes=0-999" to "0123456789abcdef",
            )
            for ((range, expected) in checks) {
                connection(url, range = range).let { response ->
                    try {
                        assertEquals(206, response.responseCode)
                        assertEquals(expected, response.inputStream.use { it.readBytes().toString(Charsets.US_ASCII) })
                        assertTrue(response.getHeaderField("Content-Range").endsWith("/${bytes.size}"))
                    } finally {
                        response.disconnect()
                    }
                }
            }
        }
    }

    @Test
    fun rejectsInvalidRangesAndUnknownCapabilities() {
        LocalCastHttpServer(loopback).use { server ->
            val url = requireNotNull(server.publish(source()))
            for (range in listOf("bytes=999-", "bytes=5-2", "bytes=1-2,3-4", "bytes=-0")) {
                connection(url, range = range).let { response ->
                    try {
                        assertEquals(416, response.responseCode)
                        assertEquals("bytes */${bytes.size}", response.getHeaderField("Content-Range"))
                    } finally {
                        response.disconnect()
                    }
                }
            }
            connection(url + "forged").let { response ->
                try {
                    assertEquals(404, response.responseCode)
                } finally {
                    response.disconnect()
                }
            }
        }
    }

    @Test
    fun parsesByteRangesWithoutOverflowOrIllegalOffsets() {
        val parse = LocalCastHttpServer.ByteSpan.Companion::parse
        assertEquals(LocalCastHttpServer.ByteSpan(0, 15), parse("bytes=-1000", 16))
        assertEquals(LocalCastHttpServer.ByteSpan(0, 0), parse("bytes=0-0", 16))
        assertNull(parse("bytes=999999999999999999999999-", 16))
        assertNull(parse("items=0-1", 16))
        assertNull(parse("bytes=-", 16))
        assertNull(parse("bytes=0-", 0))
    }
}
