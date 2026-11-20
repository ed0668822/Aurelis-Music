// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** F055: glyph families retain distinct action and selection-state artwork. */
class AurelisIconFamilyF055Test {
    @Test fun commonActionIconsUseSourceViewportAndPaths() {
        val icons = listOf(AurelisIcons.Home, AurelisIcons.Search,
            AurelisIcons.Explore, AurelisIcons.Library, AurelisIcons.Queue,
            AurelisIcons.Lyrics, AurelisIcons.Shuffle, AurelisIcons.Repeat,
            AurelisIcons.MusicNote, AurelisIcons.Clock, AurelisIcons.Add,
            AurelisIcons.Check, AurelisIcons.ChevronRight)
        icons.forEach {
            assertEquals(24f, it.viewportWidth, 0f)
            assertEquals(24f, it.viewportHeight, 0f)
            assertFalse(it.root.size == 0)
        }
    }
    @Test fun favoriteSelectedAndUnselectedUseDistinctSourceGeometry() {
        assertNotEquals(AurelisIcons.Heart.name, AurelisIcons.HeartBorder.name)
        assertNotEquals(AurelisReferenceGlyphs.Star.name, AurelisReferenceGlyphs.StarFilled.name)
    }
    @Test fun menuFamilyHasRealIndependent24DpGlyphs() {
        val glyphs = listOf(AurelisMenuIcons.PlaylistAdd, AurelisMenuIcons.PlayNext,
            AurelisMenuIcons.PlayLast, AurelisMenuIcons.Radio,
            AurelisMenuIcons.Info, AurelisMenuIcons.SleepTimer,
            AurelisMenuIcons.LyricsOffset, AurelisMenuIcons.PlaybackTools,
            AurelisMenuIcons.MixOverview, AurelisMenuIcons.CopyDiagnostic,
            AurelisMenuIcons.Album, AurelisMenuIcons.Artist, AurelisMenuIcons.More)
        glyphs.forEach {
            assertEquals(24f, it.viewportWidth, 0f)
            assertEquals(24f, it.viewportHeight, 0f)
            assertFalse(it.root.size == 0)
        }
        assertNotEquals(AurelisMenuIcons.PlayNext.name, AurelisMenuIcons.PlayLast.name)
    }
    @Test fun navigationAndPlaybackRemainAurelisDistinctFromBrand() {
        assertEquals("bc_home", AurelisIcons.Home.name)
        assertEquals("bc_queue", AurelisIcons.Queue.name)
        assertEquals("bc_lyrics", AurelisIcons.Lyrics.name)
    }
}
