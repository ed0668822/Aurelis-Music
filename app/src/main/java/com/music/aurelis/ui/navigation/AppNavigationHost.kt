// Aurelis © 2026. GPL-3.0-or-later
// Entry-keyed page lifecycle adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898,
// GPLv3; original contributors and source paths retained in NOTICE.
package com.music.aurelis.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val RouteStateKeysSaver = listSaver<MutableSet<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableSet() },
)

/**
 * One visible page, saved state per tab/page, and playback overlays kept composed
 * so existing enter/exit animations run. Hidden pages are not interactive or
 * exposed to accessibility. Only small UI state belongs in the saved Bundle.
 */
@Composable
fun AppNavigationHost(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    tabContent: @Composable BoxScope.(AppTab) -> Unit,
    routeContent: @Composable (AppRoute) -> Unit,
    bottomBar: @Composable BoxScope.() -> Unit,
    pageModifier: Modifier = Modifier,
    pageBottomPadding: Dp = 0.dp,
    topBar: @Composable BoxScope.() -> Unit = {},
    playbackOverlays: @Composable BoxScope.() -> Unit = {},
) {
    val stateHolder = rememberSaveableStateHolder()
    val visibleEntry = navigator.visibleEntry()
    val route = visibleEntry?.route
    val playbackPage = route == AppRoute.NowPlaying || route == AppRoute.Queue
    val pageEntry = if (playbackPage) navigator.entries.asReversed().firstOrNull {
        it.route != AppRoute.NowPlaying && it.route != AppRoute.Queue
    } else visibleEntry
    val livePageKeys = navigator.entries.map { AppDestination.Route(it).key }.toSet()
    val retainedPageKeys = rememberSaveable(saver = RouteStateKeysSaver) { mutableSetOf<String>() }
    SideEffect {
        retainedPageKeys.filterNot { it in livePageKeys }.forEach(stateHolder::removeState)
        retainedPageKeys.retainAll(livePageKeys)
        retainedPageKeys.addAll(livePageKeys)
    }
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    Box(modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize().then(pageModifier).then(if (playbackPage) Modifier.clearAndSetSemantics { } else Modifier), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (pageEntry == null) {
                    stateHolder.SaveableStateProvider("tab.${navigator.selectedTab.name}") {
                        tabContent(navigator.selectedTab)
                    }
                } else {
                    requireNotNull(pageEntry.route.navigationKey) { "Destination is not wired in the local graph" }
                    Box(Modifier.fillMaxSize().padding(bottom = pageBottomPadding)
                        .consumeWindowInsets(PaddingValues(bottom = pageBottomPadding))) {
                        stateHolder.SaveableStateProvider(AppDestination.Route(pageEntry).key) { routeContent(pageEntry.route) }
                    }
                }
            }
        }
        // Chrome belongs to the app, including pushed library/settings pages.
        // Keep it composed behind the player for the dock; hide its semantics there.
        Box(Modifier.fillMaxSize().then(if (playbackPage) Modifier.clearAndSetSemantics { }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            } else Modifier)) {
            topBar()
            bottomBar()
        }
        playbackOverlays()
    }
}
