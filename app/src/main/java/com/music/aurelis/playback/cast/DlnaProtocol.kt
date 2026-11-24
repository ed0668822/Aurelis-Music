// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** A UPnP AVTransport renderer whose control URL came from an SSDP-advertised device. */
internal data class DlnaRenderer(
    val id: String,
    val name: String,
    val transportUrl: String,
    val transportType: String,
    val connectionUrl: String? = null,
    val connectionType: String? = null,
    val renderingUrl: String? = null,
    val renderingType: String? = null,
)

internal object DlnaProtocol {
    private const val MAX_DOCUMENT = 256 * 1024
    private const val SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/"

    fun location(ssdp: String, sender: InetAddress): URL? {
        if (sender !is Inet4Address || sender.isLoopbackAddress || sender.isAnyLocalAddress ||
            sender.isMulticastAddress || sender.isLinkLocalAddress || !sender.isSiteLocalAddress
        ) return null
        val value = ssdp.lineSequence().firstOrNull {
            it.substringBefore(':').equals("LOCATION", ignoreCase = true)
        }?.substringAfter(':')?.trim() ?: return null
        val url = runCatching { URL(value) }.getOrNull() ?: return null
        // Never follow an SSDP response to another host or a public server.
        if (url.protocol != "http" || url.host != sender.hostAddress ||
            url.port !in -1..65535 || url.port == 0 || url.userInfo != null
        ) return null
        return url
    }

    fun parseDescription(xml: String, location: URL): DlnaRenderer? = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream(Charsets.UTF_8))
        val device = doc.getElementsByTagNameNS("*", "device").item(0) as? Element ?: return null
        val type = device.childText("deviceType")
        if (!type.contains("MediaRenderer")) return null
        val id = device.childText("UDN").takeIf { it.isNotBlank() } ?: return null
        val name = device.childText("friendlyName").takeIf { it.isNotBlank() } ?: id
        val services = device.getElementsByTagNameNS("*", "service")
        fun service(kind: String): Pair<String, String>? {
            for (i in 0 until services.length) {
                val node = services.item(i) as? Element ?: continue
                val serviceType = node.childText("serviceType")
                if (!serviceType.contains(kind)) continue
                val path = node.childText("controlURL")
                val url = runCatching { URL(location, path) }.getOrNull() ?: continue
                if (url.protocol == "http" && url.host == location.host &&
                    url.port == location.port && url.userInfo == null
                ) return url.toExternalForm() to serviceType
            }
            return null
        }
        val transport = service(":AVTransport:") ?: return null
        val connection = service(":ConnectionManager:")
        val rendering = service(":RenderingControl:")
        DlnaRenderer(id, name.take(100), transport.first, transport.second,
            connection?.first, connection?.second, rendering?.first, rendering?.second)
    }.getOrNull()

    private fun Element.childText(name: String): String {
        val nodes = getElementsByTagNameNS("*", name)
        return nodes.item(0)?.textContent?.trim().orEmpty()
    }

    fun xmlEscape(text: String): String = buildString {
        for (char in text) append(when (char) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&apos;"
            else -> char.toString()
        })
    }

    fun soap(service: String, action: String, arguments: Map<String, String>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        append("<s:Envelope xmlns:s=\"").append(SOAP_NS).append("\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">")
        append("<s:Body><u:").append(action).append(" xmlns:u=\"").append(xmlEscape(service)).append("\">")
        for ((name, value) in arguments) {
            require(name.matches(Regex("[A-Za-z][A-Za-z0-9]*")))
            append('<').append(name).append('>').append(xmlEscape(value)).append("</").append(name).append('>')
        }
        append("</u:").append(action).append("></s:Body></s:Envelope>")
    }

    fun xmlValue(xml: String, field: String): String? = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream(Charsets.UTF_8))
        doc.getElementsByTagNameNS("*", field).item(0)?.textContent?.trim()
    }.getOrNull()

    fun relativeTime(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return "%02d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }

    fun parseTime(value: String?): Long? {
        val parts = value?.split(':') ?: return null
        if (parts.size != 3) return null
        val h = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull() ?: return null
        val seconds = parts[2].substringBefore('.').toLongOrNull() ?: return null
        if (h < 0 || m !in 0..59 || seconds !in 0..59) return null
        return (h * 3600 + m * 60 + seconds) * 1000
    }

    fun supports(mime: String, sink: String?): Boolean {
        if (sink.isNullOrBlank()) return true // unreported capability, probe at load
        val types = sink.split(',').mapNotNull { entry ->
            entry.split(':').takeIf { it.size >= 3 && it[0] == "http-get" }?.get(2)
        }
        return types.any { it == "*" || it.equals(mime, true) || (it.endsWith("/*") && mime.startsWith(it.dropLast(1))) }
    }

    /**
     * A bounded, no-redirect HTTP client for trusted SSDP-advertised LAN hosts.
     * Raw sockets avoid Android's outbound cleartext HTTP policy preventing the
     * mandatory UPnP HTTP/SOAP protocol. Call ONLY with validated renderer URLs.
     */
    fun request(url: URL, method: String, payload: String? = null, action: String? = null): String? {
        if (url.protocol != "http" || url.userInfo != null || url.port == 0) return null
        val host = runCatching { InetAddress.getByName(url.host) }.getOrNull() as? Inet4Address ?: return null
        if (host.isAnyLocalAddress || host.isLoopbackAddress || host.isMulticastAddress ||
            host.isLinkLocalAddress || !host.isSiteLocalAddress) return null
        val port = if (url.port > 0) url.port else 80
        val body = payload?.toByteArray(Charsets.UTF_8) ?: byteArrayOf()
        if (body.size > 8192) return null
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1800)
                socket.soTimeout = 2500
                val target = (url.file.ifEmpty { "/" }).replace("\r", "").replace("\n", "")
                val header = buildString {
                    append(method).append(' ').append(target).append(" HTTP/1.1\r\n")
                    append("Host: ").append(url.host).append(':').append(port).append("\r\n")
                    append("Connection: close\r\n")
                    if (payload != null) {
                        append("Content-Type: text/xml; charset=\"utf-8\"\r\n")
                        action?.let { append("SOAPACTION: \"").append(it).append("\"\r\n") }
                        append("Content-Length: ").append(body.size).append("\r\n")
                    }
                    append("\r\n")
                }
                socket.getOutputStream().write(header.toByteArray(Charsets.US_ASCII))
                if (body.isNotEmpty()) socket.getOutputStream().write(body)
                socket.getOutputStream().flush()
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= MAX_DOCUMENT + 16384) {
                    val count = socket.getInputStream().read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val response = output.toString("UTF-8")
                if (!response.startsWith("HTTP/1.") ||
                    !Regex("^HTTP/1\\.[01] 2\\d\\d").containsMatchIn(response) ||
                    response.length > MAX_DOCUMENT + 16384
                ) return null
                response.substringAfter("\r\n\r\n", "")
            }
        }.getOrNull()
    }

    fun call(renderer: DlnaRenderer, action: String, params: Map<String, String>): String? {
        val url = runCatching { URL(renderer.transportUrl) }.getOrNull() ?: return null
        return request(url, "POST", soap(renderer.transportType, action, params),
            renderer.transportType + "#" + action)
    }
}
