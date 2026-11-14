package com.music.aurelis.data

import android.content.Context
import com.music.aurelis.AurelisApplication
import com.music.aurelis.data.model.ProfileId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalProfilesTest {
    private val app get() = RuntimeEnvironment.getApplication() as AurelisApplication
    @Test fun activeLegacyScopeKeepsCollectionsSettingsHistoryQueueAndAvatarAfterRestart() {
        SingleProfileMigration(app).directory.deleteRecursively() // Fixture representing a pre-upgrade installation.
        val id = ProfileId("previous-active")
        val registry = app.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)
        val raw = """{"version":1,"ids":["default","previous-active"],"active":"previous-active"}"""
        registry.edit().putString("profiles_v1", raw).commit()
        ProfileStorage(app).open(id, ProfileDataArea.PROFILE).edit().putString("user_name", "Anterior").putString("user_photo", "content://legacy/photo").commit()
        ProfileStorage(app).open(id, ProfileDataArea.PREFERENCES).edit().putInt("theme_mode", 2).putFloat("eq_balance", .7f).commit()
        val library = LocalUserLibrary(app, id)
        library.toggleFavorite("one"); library.createPlaylist("Original", listOf("one"))
        library.recordListening(com.music.aurelis.data.model.LocalSong("one", "One", "A", null, "content://media/one", null, 60000, null, null, null), 1000, false); library.flush()
        val queue = SavedLocalQueue(listOf("one"), 0, 2345, 2, true)
        LocalQueueStore(app, id).save(queue)
        repeat(2) {
            app.onCreate()
            assertEquals(id, ProfileRepository.profileId); assertEquals(id, app.userLibrary.profileId)
            assertEquals("Anterior", ProfileRepository.userName); assertEquals("content://legacy/photo", ProfileRepository.userPhotoUri)
            assertEquals(2, SettingsRepository.themeMode.value); assertEquals(.7f, SettingsRepository.equalizerBalance.value, 0f)
            assertEquals(setOf("one"), app.userLibrary.state.value.favorites)
            assertEquals("Original", app.userLibrary.state.value.playlists.single().name)
            assertEquals(1000L, app.userLibrary.state.value.listening.getValue("one").listenedMs)
            assertEquals(queue, LocalQueueStore(app).read()); assertEquals(raw, registry.getString("profiles_v1", null))
        }
    }
    @Test fun editingSingleProfilePersistsWithoutChangingItsStorageIdentityOrLibrary() {
        val original = app.userLibrary; original.toggleFavorite("one")
        val id = ProfileRepository.profileId
        ProfileRepository.rename("  Nuevo nombre  ")
        ProfileRepository.userPhotoUri = "content://device/photo"
        ProfileRepository.init(app)
        assertEquals(id, ProfileRepository.profileId); assertEquals("Nuevo nombre", ProfileRepository.userName)
        assertEquals("content://device/photo", ProfileRepository.userPhotoUri)
        assertSame(original, app.userLibrary); assertEquals(setOf("one"), original.state.value.favorites)
    }
    @Test fun invalidNamesAndMalformedPreferencesDoNotDestroyExistingValues() {
        assertThrows(IllegalArgumentException::class.java) { ProfileRepository.rename(" ") }
        assertThrows(IllegalArgumentException::class.java) { ProfileRepository.rename("x".repeat(61)) }
        val prefs = ProfileStorage(app).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
        prefs.edit().putString("theme_mode", "wrong type").putString("eq_balance", "bad").commit()
        SettingsRepository.init(app)
        assertEquals(0, SettingsRepository.themeMode.value); assertEquals(0f, SettingsRepository.equalizerBalance.value, 0f)
        assertEquals("wrong type", prefs.getString("theme_mode", null))
    }
}
