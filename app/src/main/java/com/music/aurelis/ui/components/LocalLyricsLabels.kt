// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R
import com.music.aurelis.data.lyrics.LyricsSource

@Composable
internal fun lyricsSourceTitle(source: LyricsSource): String = when (source) {
    LyricsSource.LOCAL_FILE -> stringResource(R.string.lyrics_local_file_title)
    LyricsSource.EMBEDDED -> stringResource(R.string.lyrics_embedded_title)
    else -> source.label
}
