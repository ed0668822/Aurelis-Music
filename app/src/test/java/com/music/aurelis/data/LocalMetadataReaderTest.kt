// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.data

import android.media.MediaMetadataRetriever
import android.net.Uri
import java.util.concurrent.CancellationException
import com.music.aurelis.AurelisApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalMetadataReaderTest {
    private val uri = Uri.parse("content://media/external/audio/media/1")
    @Test fun successfulReadsAreCachedOnlyWhileFileIdentityIsCurrent() {
        var calls = 0
        val reader = LocalMetadataReader { calls++; LocalFileMetadata(genre = "Jazz") }
        repeat(2) { assertEquals("Jazz", reader.read(uri, 100, 200)?.genre) }
        assertEquals(1, calls)
        reader.read(uri, 100, 201); reader.read(uri, 101, 201)
        repeat(2) { reader.read(uri, 0, 0) }
        assertEquals(5, calls)
    }
    @Test fun transientFailuresAreNotCachedAndPermissionDenialIsPropagated() {
        var calls = 0
        val reader = LocalMetadataReader { if (++calls == 1) null else LocalFileMetadata(genre = "Rock") }
        assertNull(reader.read(uri, 1, 1)); assertEquals("Rock", reader.read(uri, 1, 1)?.genre)
        assertThrows(SecurityException::class.java) { LocalMetadataReader { throw SecurityException() }.read(uri, 1, 1) }
    }
    @Test fun androidRetrieverReadsOnlyActualTagValues() {
        val app = RuntimeEnvironment.getApplication()
        val source = DataSource.toDataSource(app, uri)
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_GENRE, "  Jazz  ")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, "2/12")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, "1/2")
        val tags = LocalMetadataReader(app).read(uri, 0, 0)!!
        assertEquals("Jazz", tags.genre); assertEquals(2, tags.track); assertEquals(1, tags.disc)
        assertNull(tags.year); assertNull(tags.title)
    }
    @Test fun emptyMetadataDoesNotBecomePermanentCacheHit() {
        var reads = 0
        val reader = LocalMetadataReader {
            reads++
            if (reads == 1) LocalFileMetadata() else LocalFileMetadata(genre = "Jazz")
        }
        assertFalse(reader.read(uri, 100, 200)!!.hasValues())
        assertEquals("Jazz", reader.read(uri, 100, 200)?.genre)
        assertEquals("Jazz", reader.read(uri, 100, 200)?.genre)
        assertEquals(2, reads)
    }

    @Test fun invalidIndicesAreRejectedAndSpacesInFractionsAreAccepted() {
        val app = RuntimeEnvironment.getApplication()
        val tagUri = Uri.parse("content://media/external/audio/media/777")
        val source = DataSource.toDataSource(app, tagUri)
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, " 3 / 12 ")
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER, "9999/10000")
        val tags = LocalMetadataReader(app).read(tagUri, 0, 0)!!
        assertEquals(3, tags.track)
        assertNull(tags.disc)
    }

    @Test fun cancellationAndPermissionFailureAreNeverCached() {
        assertThrows(CancellationException::class.java) {
            LocalMetadataReader { throw CancellationException("Cancel scan") }.read(uri, 100, 200)
        }
        var reads = 0
        val reader = LocalMetadataReader {
            if (++reads == 1) throw SecurityException("Permission revoked")
            LocalFileMetadata(genre = "Ambient")
        }
        assertThrows(SecurityException::class.java) { reader.read(uri, 100, 200) }
        assertEquals("Ambient", reader.read(uri, 100, 200)?.genre)
        assertEquals(2, reads)
    }

    @Test fun cacheHasBoundedNumberOfEntries() {
        var reads = 0
        val reader = LocalMetadataReader { reads++; LocalFileMetadata(title = "Track") }
        for (id in 1..2049) reader.read(Uri.parse("content://media/external/audio/media/$id"), 100, 200)
        assertEquals(2049, reads)
        reader.read(Uri.parse("content://media/external/audio/media/2049"), 100, 200)
        assertEquals(2049, reads)
        reader.read(Uri.parse("content://media/external/audio/media/1"), 100, 200)
        assertEquals(2050, reads)
    }

}
