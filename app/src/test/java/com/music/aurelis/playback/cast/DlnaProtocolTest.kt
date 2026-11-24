// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.playback.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.URL

class DlnaProtocolTest {
    private val deviceLocation = URL("http://192.168.1.30:8200/root/desc.xml")

    private val description = """
        <root xmlns="urn:schemas-upnp-org:device-1-0">
          <device>
            <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
            <friendlyName>Living Room TV</friendlyName>
            <UDN>uuid:renderer-123</UDN>
            <serviceList>
              <service>
                <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                <controlURL>/upnp/control/transport</controlURL>
              </service>
              <service>
                <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>
                <controlURL>/upnp/control/connection</controlURL>
              </service>
            </serviceList>
          </device>
        </root>
    """.trimIndent()

    @Test fun resolvesSafeRendererAndServices() {
        val target = DlnaProtocol.parseDescription(description, deviceLocation)
        assertNotNull(target)
        assertEquals("Living Room TV", target!!.name)
        assertEquals("uuid:renderer-123", target.id)
        assertEquals("http://192.168.1.30:8200/upnp/control/transport", target.transportUrl)
        assertEquals("http://192.168.1.30:8200/upnp/control/connection", target.connectionUrl)
        assertNull(target.renderingUrl)
    }

    @Test fun rejectsNonRendererAndExternalControlHost() {
        assertNull(DlnaProtocol.parseDescription(description.replace("MediaRenderer", "MediaServer"), deviceLocation))
        val evil = description.replace("/upnp/control/transport", "http://example.org/remote")
        assertNull(DlnaProtocol.parseDescription(evil, deviceLocation))
    }

    @Test fun onlyTrustsSsdpLocationAdvertisedByRespondingAddress() {
        val peer = InetAddress.getByName("192.168.1.30")
        assertEquals(deviceLocation, DlnaProtocol.location(
            "HTTP/1.1 200 OK\r\nLOCATION: ${deviceLocation}\r\n\r\n", peer))
        assertNull(DlnaProtocol.location("LOCATION: http://example.org/payload", peer))
        assertNull(DlnaProtocol.location("LOCATION: http://192.168.1.99/payload", peer))
    }

    @Test fun emitsEscapedSoapAndValidatesProtocolInfo() {
        val xml = DlnaProtocol.soap("urn:schemas-upnp-org:service:AVTransport:1",
            "SetAVTransportURI", mapOf("InstanceID" to "0", "CurrentURI" to "http://tv/?a=1&b=2"))
        assertTrue(xml.contains("a=1&amp;b=2"))
        assertEquals("01:01:01", DlnaProtocol.relativeTime(3_661_999))
        assertEquals(3_661_000L, DlnaProtocol.parseTime("01:01:01"))
        assertTrue(DlnaProtocol.supports("audio/mpeg", "http-get:*:audio/mpeg:*,http-get:*:audio/wav:*"))
        assertFalse(DlnaProtocol.supports("audio/flac", "http-get:*:audio/mpeg:*"))
    }
}
