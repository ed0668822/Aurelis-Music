// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.widget

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.music.aurelis.AurelisApplication
import com.music.aurelis.playback.RecordingPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MediaWidgetIntegrationTest {
    @Test fun providersHaveRealLayoutsAndActionsArePrivate() {
        val app = RuntimeEnvironment.getApplication()
        for (provider in listOf(MediaWidgetSquare::class.java, MediaWidgetWide::class.java, MediaWidgetPill::class.java)) {
            val info = app.packageManager.getReceiverInfo(ComponentName(app, provider), PackageManager.GET_META_DATA)
            assertFalse(info.exported)
            val metadata = requireNotNull(info.metaData).getInt("android.appwidget.provider")
            assertTrue(metadata != 0)
            app.resources.getXml(metadata).use { xml ->
                while (xml.eventType != org.xmlpull.v1.XmlPullParser.START_TAG) xml.next()
                val layout = xml.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "initialLayout", 0)
                assertTrue(layout != 0)
            }
        }
        assertFalse(app.packageManager.getReceiverInfo(ComponentName(app, MediaWidgetActions::class.java), 0).exported)
    }

    @Test fun aLateArtworkRenderCannotPublishAnOlderTrackOrPlaybackState() {
        val app = RuntimeEnvironment.getApplication()
        val old = MediaWidgetSnapshot.EMPTY.copy(mediaId = "old", title = "Previous", isPlaying = true)
        val current = old.copy(mediaId = "new", title = "Current", isPlaying = false)
        MediaWidgetSnapshot.save(app, old)
        MediaWidgetSnapshot.save(app, current)
        var publications = 0
        assertFalse(MediaWidgetSnapshot.publishIfCurrent(app, old) { publications++ })
        assertTrue(MediaWidgetSnapshot.publishIfCurrent(app, current) { publications++ })
        assertEquals(1, publications)
    }

    @Test fun widgetSnapshotUsesTheExistingPlayersMetadataAndActualPlaybackState() {
        val player = RecordingPlayer()
        try {
            assertFalse(MediaWidgetBinding.snapshot(player).hasTrack)
            player.setMediaItems(listOf(MediaItem.Builder().setMediaId("local-one").setUri("content://media/1")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Local title").setArtist("Local artist").build()).build()))
            player.prepare(); player.play()
            val playing = MediaWidgetBinding.snapshot(player)
            assertEquals("local-one", playing.mediaId)
            assertEquals("Local title", playing.title)
            assertTrue(playing.isPlaying)
            player.pause()
            assertFalse(MediaWidgetBinding.snapshot(player).isPlaying)
            MediaWidgetSnapshot.save(RuntimeEnvironment.getApplication(), playing)
            assertEquals(playing, MediaWidgetSnapshot.load(RuntimeEnvironment.getApplication()))
        } finally { player.release() }
    }
}
