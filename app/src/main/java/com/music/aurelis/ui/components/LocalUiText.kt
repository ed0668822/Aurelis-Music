// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R
import com.music.aurelis.data.model.SortOption

/** Localize presentation without changing enum names saved in preferences. */
@Composable
internal fun SortOption.localizedLabel(): String = stringResource(when (this) {
    SortOption.TITLE_ASC -> R.string.local_sort_title_asc
    SortOption.TITLE_DESC -> R.string.local_sort_title_desc
    SortOption.ARTIST_ASC -> R.string.local_sort_artist_asc
    SortOption.ARTIST_DESC -> R.string.local_sort_artist_desc
    SortOption.ALBUM_ASC -> R.string.local_sort_album_asc
    SortOption.ALBUM_DESC -> R.string.local_sort_album_desc
    SortOption.DURATION_ASC -> R.string.local_sort_duration_asc
    SortOption.DURATION_DESC -> R.string.local_sort_duration_desc
})
