package com.music.aurelis.data

import com.music.aurelis.AurelisApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = AurelisApplication::class)
class LocalSearchHistoryTest {
    private val context get() = RuntimeEnvironment.getApplication() as AurelisApplication
    @org.junit.After fun cleanup() {
        ProfileStorage(context).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
            .edit().remove("local_search_history_v1").commit()
    }
    @Test fun historyPersistsDeduplicatesAccentsAndCanBeRemovedOrCleared() {
        val history = LocalSearchHistory(context)
        history.clear()
        history.submit("Beyoncé"); history.submit("Beyonce")
        assertEquals(listOf("Beyonce"), LocalSearchHistory(context).entries)
        (1..25).forEach { history.submit("Query $it") }
        assertEquals(20, LocalSearchHistory(context).entries.size)
        history.remove("Query 25")
        assertFalse(LocalSearchHistory(context).entries.contains("Query 25"))
        history.clear()
        assertTrue(LocalSearchHistory(context).entries.isEmpty())
    }
    @Test fun malformedHistoryIsPreservedInsteadOfOverwritten() {
        val prefs = ProfileStorage(context).open(ProfileRepository.profileId, ProfileDataArea.PREFERENCES)
        prefs.edit().putString("local_search_history_v1", "broken").commit()
        val history = LocalSearchHistory(context)
        history.submit("Must not overwrite")
        assertEquals("broken", prefs.getString("local_search_history_v1", null))
    }
}
