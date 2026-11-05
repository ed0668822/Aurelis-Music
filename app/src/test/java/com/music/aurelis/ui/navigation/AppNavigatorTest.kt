package com.music.aurelis.ui.navigation

import com.music.aurelis.data.model.LocalShelf
import org.junit.Assert.*
import org.junit.Test

class AppNavigatorTest {
    @Test fun collectionRoutesRestoreSmallIdsIncludingUnicodeAndSeparators() {
        for (route in listOf(AppRoute.Collection(LocalCollectionKind.FAVORITES),
            AppRoute.Collection(LocalCollectionKind.PLAYLISTS),
            AppRoute.Collection(LocalCollectionKind.PLAYLIST, "list-1"),
            AppRoute.Collection(LocalCollectionKind.ALBUM, "Álbum: A + B / 100%"),
            AppRoute.Collection(LocalCollectionKind.ARTIST, "Artista & banda"))) {
            val navigator = AppNavigator().apply { push(route) }
            assertEquals(route, AppNavigator.restoreState(requireNotNull(navigator.saveState()))?.visibleRoute())
        }
    }
    @Test fun nestedPagesReturnToTheSourceTabBeforeHome() {
        val navigator = AppNavigator()
        navigator.selectTab(AppTab.LIBRARY)
        navigator.push(AppRoute.Settings)
        navigator.push(AppRoute.Settings)
        assertEquals(1, navigator.entries.size)
        navigator.push(AppRoute.Equalizer)
        assertEquals(AppRoute.Equalizer, navigator.visibleRoute())
        assertTrue(navigator.back())
        assertEquals(AppRoute.Settings, navigator.visibleRoute())
        assertTrue(navigator.back())
        assertNull(navigator.visibleRoute())
        assertEquals(AppTab.LIBRARY, navigator.selectedTab)
        assertTrue(navigator.back())
        assertEquals(AppTab.HOME, navigator.selectedTab)
        assertFalse(navigator.canGoBack)
        assertFalse(navigator.back())
    }

    @Test fun queueReturnsToPlayerThenTheOriginalTab() {
        val navigator = AppNavigator()
        navigator.selectTab(AppTab.SEARCH)
        navigator.pushAll(AppRoute.NowPlaying, AppRoute.Queue)
        assertTrue(navigator.pop())
        assertEquals(AppRoute.NowPlaying, navigator.visibleRoute())
        assertTrue(navigator.pop())
        assertNull(navigator.visibleRoute())
        assertEquals(AppTab.SEARCH, navigator.selectedTab)
    }

    @Test fun selectingOrReselectingATabDoesNotGrowHistory() {
        val navigator = AppNavigator()
        for (tab in AppTab.entries) {
            navigator.push(AppRoute.Settings)
            navigator.selectTab(tab)
            navigator.selectTab(tab)
            assertEquals(tab, navigator.selectedTab)
            assertTrue(navigator.entries.isEmpty())
            assertEquals(tab != AppTab.HOME, navigator.canGoBack)
        }
    }

    @Test fun savedNavigationRestoresAllWiredPagesAndCanBePoppedInOrder() {
        val navigator = AppNavigator()
        navigator.selectTab(AppTab.LIBRARY)
        val routes = listOf(AppRoute.LocalMusic, AppRoute.Settings, AppRoute.Equalizer,
            AppRoute.Replay, AppRoute.NowPlaying, AppRoute.Queue)
        routes.forEach(navigator::push)
        val restored = requireNotNull(AppNavigator.restoreState(requireNotNull(navigator.saveState())))
        assertEquals(AppTab.LIBRARY, restored.selectedTab)
        assertEquals(routes, restored.entries.map { it.route })
        assertEquals(restored.entries.size, restored.entries.map { it.id }.distinct().size)
        for (route in routes.reversed()) {
            assertEquals(route, restored.visibleRoute())
            assertTrue(restored.pop())
        }
        assertNull(restored.visibleRoute())
    }

    @Test fun malformedOrOutdatedSavedStateIsRejectedWithoutPartialNavigation() {
        for (saved in listOf(emptyList(), listOf("1"), listOf("2", "HOME"),
            listOf("1", "REMOVED_TAB"), listOf("1", "HOME", "settings", "removed-page"))) {
            assertNull(AppNavigator.restoreState(saved))
        }
        for (tab in AppTab.entries) {
            val navigator = AppNavigator().apply { selectTab(tab) }
            assertEquals(tab, AppNavigator.restoreState(requireNotNull(navigator.saveState()))?.selectedTab)
        }
    }

    @Test fun existingDetailAndRouteOrderingIsPreserved() {
        val details = mutableListOf<String>()
        val navigator = AppNavigator(
            detailDepth = { details.size },
            popDetail = { if (details.isEmpty()) false else { details.removeAt(details.lastIndex); true } },
            clearDetails = { details.clear() },
        )
        navigator.push(AppRoute.Settings)
        details += "album"
        assertNull(navigator.visibleRoute())
        navigator.push(AppRoute.Equalizer)
        details += "artist"
        assertTrue(navigator.pop())
        assertEquals(listOf("album"), details)
        assertEquals(AppRoute.Equalizer, navigator.visibleRoute())
        assertTrue(navigator.pop())
        assertNull(navigator.visibleRoute())
        assertTrue(navigator.pop())
        assertEquals(AppRoute.Settings, navigator.visibleRoute())
        assertTrue(navigator.pop())
        assertFalse(navigator.pop())
        details += "album"
        navigator.selectTab(AppTab.SEARCH)
        assertTrue(details.isEmpty())
    }

    @Test fun removedDetailsDoNotLeaveInvisiblePages() {
        var depth = 2
        val navigator = AppNavigator(detailDepth = { depth })
        navigator.push(AppRoute.Settings)
        depth = 0
        navigator.onDetailStackShrunk(depth)
        assertEquals(0, navigator.entries.single().detailDepth)
        assertEquals(AppRoute.Settings, navigator.visibleRoute())
    }

    @Test fun deferredShelfDataAndExternalDetailCallbacksAreNotSerialized() {
        val navigator = AppNavigator()
        navigator.push(AppRoute.LibraryShowAll(LocalShelf("Local", emptyList())))
        assertNull(navigator.saveState())
        assertNull(AppNavigator(detailDepth = { 1 }).saveState())
    }
}
