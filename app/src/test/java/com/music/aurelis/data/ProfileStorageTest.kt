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
class ProfileStorageTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun legacyProfileAndSettingsSurviveRepeatedInitialization() {
        val legacyProfile = context.getSharedPreferences("aurelis_profile", Context.MODE_PRIVATE)
        assertTrue(legacyProfile.edit().putString("user_name", "Mi nombre")
            .putString("user_photo", "content://avatars/42")
            .putString("unknown_key", "keep me").commit())
        val legacySettings = context.getSharedPreferences("aurelis_settings", Context.MODE_PRIVATE)
        assertTrue(legacySettings.edit().putInt("theme_mode", 2)
            .putBoolean("reduce_animation", true)
            .putStringSet("unknown_set", setOf("a", "b")).commit())
        val profileBefore = legacyProfile.all.toMap()
        val settingsBefore = legacySettings.all.toMap()

        repeat(2) {
            ProfileRepository.init(context)
            SettingsRepository.init(context)
            assertEquals(ProfileId.DEFAULT, ProfileRepository.profileId)
            assertEquals("Mi nombre", ProfileRepository.userName)
            assertEquals("content://avatars/42", ProfileRepository.userPhotoUri)
            assertEquals(2, SettingsRepository.themeMode.value)
            assertTrue(SettingsRepository.reduceAnimation.value)
        }

        assertEquals(profileBefore, legacyProfile.all)
        assertEquals(settingsBefore, legacySettings.all)
        ProfileRepository.userName = "Nuevo nombre"
        SettingsRepository.setThemeMode(1)
        assertEquals("Nuevo nombre", legacyProfile.getString("user_name", null))
        assertEquals(1, legacySettings.getInt("theme_mode", -1))
        assertEquals(ProfileId.DEFAULT, ProfileRepository.profileId)
    }

    @Test fun everyAreaIsIsolatedAcrossProfilesAndReopensWithoutLosingData() {
        val storage = ProfileStorage(context)
        val profiles = listOf(ProfileId.DEFAULT, ProfileId("alice"), ProfileId("bob"))
        for (profile in profiles) {
            for (area in ProfileDataArea.entries) {
                assertTrue(storage.open(profile, area).edit()
                    .putString("same_key", "${profile.value}:${area.name}").commit())
            }
        }
        val reopened = ProfileStorage(context)
        for (profile in profiles) {
            for (area in ProfileDataArea.entries) {
                assertEquals("${profile.value}:${area.name}",
                    reopened.open(profile, area).getString("same_key", null))
            }
        }
    }

    @Test fun aNewProfileDoesNotInheritLegacyPreferences() {
        val storage = ProfileStorage(context)
        assertTrue(storage.open(ProfileId.DEFAULT, ProfileDataArea.PREFERENCES)
            .edit().putString("private_preference", "legacy").commit())
        assertFalse(storage.open(ProfileId("new-profile"), ProfileDataArea.PREFERENCES)
            .contains("private_preference"))
    }

    @Test fun applicationInitializesProfileBeforeSettingsScreenCanWrite() {
        ProfileRepository.userName = "Local user"
        ProfileRepository.userPhotoUri = "content://avatars/7"
        ProfileRepository.init(context)
        assertEquals("Local user", ProfileRepository.userName)
        assertEquals("content://avatars/7", ProfileRepository.userPhotoUri)
        ProfileRepository.userPhotoUri = null
        assertNull(ProfileRepository.userPhotoUri)
    }

    @Test fun invalidProfileIdsCannotEscapeOrAliasStorageBoundaries() {
        for (id in listOf("", "../alice", "a.b", "Alice", " alice", "a/b", "a".repeat(65))) {
            assertThrows(IllegalArgumentException::class.java) { ProfileId(id) }
        }
        assertEquals("a".repeat(64), ProfileId("a".repeat(64)).value)
    }
}
