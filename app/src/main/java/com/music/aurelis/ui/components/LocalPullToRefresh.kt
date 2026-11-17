// Aurelis © 2026. GPL-3.0-or-later
// PullToRefresh adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898, GPLv3.
// Original contributors retained; local refresh ownership and accessibility adapted.
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.music.aurelis.R

/** The header renders the indicator outside the recorded page, as in the reference. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }
    val refreshLabel = stringResource(R.string.home_refresh)
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { if (!refreshing) onRefresh() },
        state = state,
        modifier = modifier.fillMaxSize().semantics {
            customActions = listOf(CustomAccessibilityAction(refreshLabel) {
                if (refreshing) false else { onRefresh(); true }
            })
        },
        indicator = {},
    ) { content() }
}
