// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioOutputRouteTest {
    @Test fun noRouteDoesNotBecomePhoneByDefault() {
        assertNull(confirmedMediaRouteId(emptySet()))
        assertNull(confirmedOutput(emptyList()))
    }

    @Test fun ambiguousReportCannotClaimOneActiveDevice() {
        assertNull(confirmedMediaRouteId(setOf(1, 3)))
        val multiple = listOf(
            AudioOutputDevice(1, "Phone", AudioOutputKind.PHONE, isActive = true),
            AudioOutputDevice(3, "USB", AudioOutputKind.USB, isActive = true),
        )
        assertNull(confirmedOutput(multiple))
    }

    @Test fun twoBuiltInDevicesAreDisplayedAsOnePhone() {
        val earpiece = AudioOutputDevice(1, "Phone", AudioOutputKind.PHONE)
        val speaker = AudioOutputDevice(2, "Phone", AudioOutputKind.PHONE, isActive = true)
        val bluetooth = AudioOutputDevice(3, "Headphones", AudioOutputKind.BLUETOOTH)
        val normalized = collapseBuiltInPhoneOutputs(listOf(earpiece, speaker, bluetooth))
        assertEquals(listOf(speaker, bluetooth), normalized)
        assertEquals(speaker, confirmedOutput(normalized))
    }

    @Test fun collapsingPhoneNeverRemovesRealExternalDestinations() {
        val headset = AudioOutputDevice(4, "Wired", AudioOutputKind.WIRED, isActive = true)
        val phone = AudioOutputDevice(1, "Phone", AudioOutputKind.PHONE)
        val usb = AudioOutputDevice(5, "USB DAC", AudioOutputKind.USB)
        assertEquals(
            listOf(headset, phone, usb),
            collapseBuiltInPhoneOutputs(listOf(headset, phone, usb, phone.copy(id = 2))),
        )
        assertEquals(listOf(headset, usb), collapseBuiltInPhoneOutputs(listOf(headset, usb)))
    }

    @Test fun uniqueAndroidMediaDeviceIsReportedAsCandidate() {
        assertEquals(9, confirmedMediaRouteId(setOf(9)))
        val active = AudioOutputDevice(9, "Headphones", AudioOutputKind.BLUETOOTH, isActive = true)
        assertEquals(active, confirmedOutput(listOf(active)))
    }
}
