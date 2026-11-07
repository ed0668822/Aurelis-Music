package com.music.aurelis.data

import android.graphics.Bitmap
import android.net.Uri
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.ProfileId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class ProfileAvatarsTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    private val source = Uri.parse("content://test/avatar")
    @Test fun avatarIsPrivateCopiedAndRemovalNeverDeletesExternalLegacyImage() = runBlocking {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        bitmap.recycle()
        shadowOf(app.contentResolver).registerInputStream(source, ByteArrayInputStream(bytes))
        ProfileAvatars.import(app, ProfileId.DEFAULT, source)
        val file = File(Uri.parse(ProfileRepository.userPhotoUri).path!!)
        assertTrue(file.exists()); assertEquals(ProfileRepository.avatarDirectory(ProfileId.DEFAULT), file.parentFile)
        ProfileAvatars.remove(ProfileId.DEFAULT)
        assertNull(ProfileRepository.userPhotoUri); assertFalse(file.exists())
        val external = File(app.filesDir, "legacy.png").apply { writeText("preserve") }
        ProfileRepository.userPhotoUri = Uri.fromFile(external).toString()
        ProfileAvatars.remove(ProfileId.DEFAULT)
        assertTrue(external.exists())
    }
    @Test fun invalidOrOversizeImagePreservesExistingAvatarAndLeavesNoFiles() = runBlocking {
        ProfileRepository.userPhotoUri = "content://legacy/photo"
        for (bytes in listOf(byteArrayOf(1, 2, 3), ByteArray(2 * 1024 * 1024 + 1))) {
            shadowOf(app.contentResolver).registerInputStream(source, ByteArrayInputStream(bytes))
            try { ProfileAvatars.import(app, ProfileId.DEFAULT, source); fail("invalid image accepted") }
            catch (_: IllegalArgumentException) { }
            assertEquals("content://legacy/photo", ProfileRepository.userPhotoUri)
            assertTrue(ProfileRepository.avatarDirectory(ProfileId.DEFAULT).listFiles().isNullOrEmpty())
        }
    }
}
