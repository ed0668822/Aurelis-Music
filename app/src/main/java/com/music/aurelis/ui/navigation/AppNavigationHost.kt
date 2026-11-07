package com.music.aurelis.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

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
    playbackOverlays: @Composable BoxScope.() -> Unit = {},
) {
    val stateHolder = rememberSaveableStateHolder()
    val route = navigator.visibleRoute()
    val playbackPage = route == AppRoute.NowPlaying || route == AppRoute.Queue
    val pageRoute = if (playbackPage) navigator.entries.asReversed().firstOrNull {
        it.route != AppRoute.NowPlaying && it.route != AppRoute.Queue
    }?.route else route
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    Box(modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize().then(pageModifier).then(if (playbackPage) Modifier.clearAndSetSemantics { } else Modifier), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (pageRoute == null) {
                    stateHolder.SaveableStateProvider("tab.${navigator.selectedTab.name}") {
                        tabContent(navigator.selectedTab)
                    }
                } else {
                    val key = requireNotNull(pageRoute.navigationKey) { "Destination is not wired in the local graph" }
                    stateHolder.SaveableStateProvider("page.$key") { routeContent(pageRoute) }
                }
            }
        }
        if (route == null || (route == AppRoute.NowPlaying && pageRoute == null)) {
            Box(Modifier.fillMaxSize().then(if (playbackPage) Modifier.clearAndSetSemantics { } else Modifier)) { bottomBar() }
        }
        playbackOverlays()
    }
}
