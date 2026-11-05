// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.music.aurelis.data.model.LocalShelf

sealed interface AppRoute {
    data object Settings : AppRoute
    data object Profile : AppRoute
    data object Equalizer : AppRoute
    data object Replay : AppRoute
    data object History : AppRoute
    data object LocalMusic : AppRoute
    data object NowPlaying : AppRoute
    data object Queue : AppRoute

    data class LibraryShowAll(val shelf: LocalShelf) : AppRoute
    data class ArtistShelf(val artistId: String, val shelf: LocalShelf) : AppRoute
    data class SettingsSection(val category: SettingsCategory) : AppRoute
    data class SettingsChoice(val choice: SettingsChoiceKind) : AppRoute
    data object EqualizerPresets : AppRoute
}

data class RouteEntry(val id: Long, val route: AppRoute, val detailDepth: Int)

enum class AppTab { HOME, EXPLORE, LIBRARY, SEARCH }

/** Only implemented, parameter-free destinations are restored in the current graph. */
internal val AppRoute.navigationKey: String?
    get() = when (this) {
        AppRoute.Settings -> "settings"
        AppRoute.Equalizer -> "equalizer"
        AppRoute.Replay -> "replay"
        AppRoute.LocalMusic -> "local-music"
        AppRoute.NowPlaying -> "now-playing"
        AppRoute.Queue -> "queue"
        else -> null
    }

private fun routeForKey(key: String): AppRoute? = when (key) {
    "settings" -> AppRoute.Settings
    "equalizer" -> AppRoute.Equalizer
    "replay" -> AppRoute.Replay
    "local-music" -> AppRoute.LocalMusic
    "now-playing" -> AppRoute.NowPlaying
    "queue" -> AppRoute.Queue
    else -> null
}

@Composable
fun rememberAppNavigator(): AppNavigator =
    rememberSaveable(saver = AppNavigator.StateSaver) { AppNavigator() }

@Stable
class AppNavigator(
    private val detailDepth: () -> Int = { 0 },
    private val popDetail: () -> Boolean = { false },
    private val clearDetails: () -> Unit = {},
) {
    private val stack = mutableStateListOf<RouteEntry>()
    private var nextId = System.currentTimeMillis()

    val entries: List<RouteEntry> get() = stack

    var selectedTab by mutableStateOf(AppTab.HOME)
        private set

    val canGoBack: Boolean
        get() = stack.isNotEmpty() || detailDepth() > 0 || selectedTab != AppTab.HOME

    fun selectTab(tab: AppTab) {
        clear()
        selectedTab = tab
    }

    /** Pop pages first; a root tab returns to Home, whose Back belongs to Android. */
    fun back(): Boolean {
        if (pop()) return true
        if (selectedTab == AppTab.HOME) return false
        selectedTab = AppTab.HOME
        return true
    }

    fun visibleEntry(depth: Int = detailDepth()): RouteEntry? =
        stack.lastOrNull()?.takeIf { depth <= it.detailDepth }

    fun visibleRoute(depth: Int = detailDepth()): AppRoute? = visibleEntry(depth)?.route

    operator fun contains(route: AppRoute): Boolean = stack.any { it.route == route }

    fun push(route: AppRoute) {
        val depth = detailDepth()
        if (visibleRoute(depth) == route) return
        stack += RouteEntry(nextId++, route, depth)
    }

    fun pushAll(vararg routes: AppRoute) {
        if (routes.isEmpty() || visibleRoute() == routes.last()) return
        routes.forEach(::push)
    }

    fun pop(): Boolean {
        val top = stack.lastOrNull()
        if (detailDepth() > (top?.detailDepth ?: 0)) return popDetail()
        if (top == null) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun clear() {
        stack.clear()
        clearDetails()
    }

    fun reset(vararg routes: AppRoute) {
        clear()
        routes.forEach(::push)
    }

    fun onDetailStackShrunk(depth: Int) {
        for (i in stack.indices) {
            val entry = stack[i]
            if (entry.detailDepth > depth) stack[i] = entry.copy(detailDepth = depth)
        }
    }

    internal fun saveState(): List<String>? {
        // Never put track lists, artwork or callbacks into the Activity's saved Bundle.
        if (detailDepth() != 0 || stack.any { it.detailDepth != 0 }) return null
        val routes = stack.map { it.route.navigationKey ?: return null }
        return listOf("1", selectedTab.name) + routes
    }

    companion object {
        internal fun restoreState(saved: List<String>): AppNavigator? {
            if (saved.size < 2 || saved[0] != "1") return null
            val tab = AppTab.entries.firstOrNull { it.name == saved[1] } ?: return null
            val routes = saved.drop(2).map { routeForKey(it) ?: return null }
            return AppNavigator().apply {
                selectTab(tab)
                routes.forEach(::push)
            }
        }

        internal val StateSaver = listSaver<AppNavigator, String>(
            save = { it.saveState() ?: emptyList() },
            restore = { restoreState(it) },
        )
    }
}

sealed interface AppDestination {
    val key: String

    data class Tab(val index: Int) : AppDestination {
        override val key: String get() = "tab:$index"
    }

    data class Route(val entry: RouteEntry) : AppDestination {
        override val key: String get() = "route:${entry.id}"
    }

    data class Release(val id: String) : AppDestination {
        override val key: String get() = id
    }
}
