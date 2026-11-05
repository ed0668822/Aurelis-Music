package com.music.aurelis.playback

import android.content.ContextWrapper
import android.Manifest
import androidx.activity.ComponentActivity
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.AudioPermission
import com.music.aurelis.data.model.LocalSong
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalMetadataTest {
    @Test fun media3RoundtripPreservesScannedMetadataAndDoesNotReplaceLocalUris() {
        val song = LocalSong("42", "Local", "Artist", "Album", "content://media/external/audio/media/42",
            "content://media/external/audio/albumart/9", 500, "/Music/moved.flac", 2, 2026,
            dateAddedMs = 123_000, sizeBytes = 456_789, albumId = "9", artistId = "7", discNumber = 1)
        assertEquals(song, song.toMediaItem().toLocalSong())
    }

    @Test fun permissionSettingsRequiresAnEarlierRequestAndAnActivityEvenThroughWrappers() {
        val app = RuntimeEnvironment.getApplication()
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val context = ContextWrapper(controller.get())
        shadowOf(app).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        assertFalse(AudioPermission.needsSettings(context, false))
        assertTrue(AudioPermission.needsSettings(context, true))
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        assertFalse(AudioPermission.needsSettings(context, true))
        controller.pause().stop().destroy()
    }
}
