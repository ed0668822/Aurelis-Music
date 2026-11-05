// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

/** Compatibility entry point backed by the shared accessible search field. */
@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    placeholder: String = "Buscar canciones, artistas, álbumes…",
) {
    SearchField(
        query = query,
        onQueryChange = onQueryChanged,
        onSubmit = onSubmit,
        modifier = modifier,
        focusRequester = focusRequester,
        placeholder = placeholder,
    )
}
