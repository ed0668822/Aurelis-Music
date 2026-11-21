// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis

import com.music.aurelis.data.pruneEmbeddedArtworkCache
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEmbeddedArtworkCacheTest {
    @Test fun oldestFilesAreEvictedWithoutRemovingTheActiveCover() {
        val dir = createTempDirectory("aurelis-cache").toFile()
        try {
            val old = File(dir, "old.jpg").apply { writeBytes(ByteArray(80)); setLastModified(1000L) }
            val newer = File(dir, "new.jpg").apply { writeBytes(ByteArray(80)); setLastModified(2000L) }
            val active = File(dir, "active.jpg").apply { writeBytes(ByteArray(80)); setLastModified(500L) }
            pruneEmbeddedArtworkCache(dir, 160L, active)
            assertFalse(old.exists())
            assertTrue(newer.exists())
            assertTrue(active.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun temporaryFilesAreNotDeletedByEviction() {
        val dir = createTempDirectory("aurelis-temp-cache").toFile()
        try {
            val old = File(dir, "old.jpg").apply { writeBytes(ByteArray(90)) }
            val temp = File(dir, "cover.tmp").apply { writeBytes(ByteArray(90)) }
            pruneEmbeddedArtworkCache(dir, 10L)
            assertFalse(old.exists())
            assertTrue(temp.exists())
        } finally { dir.deleteRecursively() }
    }
}
