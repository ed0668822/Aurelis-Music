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
    @Test fun switchPersistsAndReloadsSettingsCollectionsAndRejectsStaleWrites() {
        val original = app.userLibrary
        original.toggleFavorite("one"); original.createPlaylist("Original", listOf("one"))
        SettingsRepository.setThemeMode(2); SettingsRepository.setEqualizerBalance(.7f)
        SettingsRepository.setTranslationService("https://one.example", "auto")
        val second = ProfileRepository.create("Segundo")
        ProfileRepository.switchTo(second)
        assertEquals(second, app.userLibrary.profileId)
        assertTrue(app.userLibrary.state.value.favorites.isEmpty())
        assertEquals(0, SettingsRepository.themeMode.value)
        assertEquals(0f, SettingsRepository.equalizerBalance.value, 0f)
        assertEquals("", SettingsRepository.translationEndpoint.value)
        assertThrows(IllegalStateException::class.java) { original.toggleFavorite("stale") }
        app.userLibrary.toggleFavorite("two"); SettingsRepository.setReduceAnimation(true)
        ProfileRepository.rename(second, "Renombrado")
        ProfileRepository.switchTo(ProfileId.DEFAULT)
        assertEquals(setOf("one"), app.userLibrary.state.value.favorites)
        assertEquals("Original", app.userLibrary.state.value.playlists.single().name)
        assertEquals(2, SettingsRepository.themeMode.value)
        assertEquals(.7f, SettingsRepository.equalizerBalance.value, 0f)
        ProfileRepository.switchTo(second)
        ProfileRepository.init(app); SettingsRepository.init(app)
        assertEquals(second, ProfileRepository.profileId)
        assertEquals("Renombrado", ProfileRepository.userName)
        assertTrue(SettingsRepository.reduceAnimation.value)
        assertEquals(setOf("two"), LocalUserLibrary(app).state.value.favorites)
    }
    @Test fun deletingActiveDefaultClearsOnlyItsPrivateDataAndDoesNotResurrectIt() {
        val store = ProfileStorage(app)
        ProfileDataArea.entries.forEach { store.open(ProfileId.DEFAULT, it).edit().putString("private", "erase").commit() }
        val media = java.io.File(app.filesDir, "music.mp3").apply { writeText("keep") }
        val cache = java.io.File(app.cacheDir, "lyrics-local").apply { writeText("keep") }
        val avatar = ProfileRepository.avatarDirectory(ProfileId.DEFAULT).apply { mkdirs() }.resolve("avatar.png").apply { writeText("private") }
        val other = ProfileRepository.create("Otro")
        store.open(other, ProfileDataArea.FAVORITES).edit().putString("private", "retain").commit()
        ProfileRepository.delete(ProfileId.DEFAULT)
        assertEquals(other, ProfileRepository.profileId)
        ProfileDataArea.entries.forEach { assertTrue(store.open(ProfileId.DEFAULT, it).all.isEmpty()) }
        assertFalse(avatar.exists()); assertTrue(media.exists()); assertTrue(cache.exists())
        assertEquals("retain", store.open(other, ProfileDataArea.FAVORITES).getString("private", null))
        ProfileRepository.init(app)
        assertEquals(listOf(other), ProfileRepository.state.value.profiles.map { it.id })
        assertThrows(IllegalStateException::class.java) { ProfileRepository.delete(other) }
    }
    @Test fun unknownOrCorruptRegistryStaysUntouchedAndBlocksAllProfileWrites() {
        val registry = app.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)
        for (raw in listOf("broken", "{\"version\":99}", "{\"version\":1,\"ids\":[\"default\"],\"active\":\"missing\"}")) {
            registry.edit().putString("profiles_v1", raw).commit()
            ProfileRepository.init(app)
            assertTrue(ProfileRepository.state.value.readOnly)
            assertThrows(IllegalStateException::class.java) { ProfileRepository.create("Lost") }
            assertThrows(IllegalStateException::class.java) { SettingsRepository.setThemeMode(1) }
            assertThrows(IllegalStateException::class.java) { LocalUserLibrary(app).toggleFavorite("x") }
            LocalQueueStore(app).save(SavedLocalQueue(listOf("x"), 0, 0, 0, false))
            assertEquals(raw, registry.getString("profiles_v1", null))
        }
    }
    @Test fun invalidNamesAndMalformedPreferencesDoNotDestroyExistingValues() {
        val id = ProfileRepository.create("Nombre")
        assertThrows(IllegalArgumentException::class.java) { ProfileRepository.create(" nombre ") }
        assertThrows(IllegalArgumentException::class.java) { ProfileRepository.rename(id, " ") }
        val prefs = ProfileStorage(app).open(id, ProfileDataArea.PREFERENCES)
        prefs.edit().putString("theme_mode", "wrong type").putString("eq_balance", "bad").commit()
        ProfileRepository.switchTo(id)
        assertEquals(0, SettingsRepository.themeMode.value)
        assertEquals(0f, SettingsRepository.equalizerBalance.value, 0f)
        assertEquals("wrong type", prefs.getString("theme_mode", null))
    }
    @Test fun deletionTombstoneRetriesCleanupOnOpen() {
        val removed = ProfileRepository.create("Temporal")
        val storage = ProfileStorage(app)
        storage.open(removed, ProfileDataArea.HISTORY).edit().putString("private", "pending").commit()
        val registry = app.getSharedPreferences("aurelis_profiles_registry", Context.MODE_PRIVATE)
        registry.edit().putString("profiles_v1", """{"version":1,"ids":["default"],"active":"default","deleting":["${removed.value}"]}""").commit()
        ProfileRepository.init(app)
        assertTrue(storage.open(removed, ProfileDataArea.HISTORY).all.isEmpty())
        assertFalse(registry.getString("profiles_v1", "")!!.contains(removed.value))
    }
}
