// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R
import com.music.aurelis.data.lyrics.LyricsFailure
import com.music.aurelis.data.lyrics.LyricsFailureKind

@Composable
internal fun lyricsFailureText(failure: LyricsFailure): String = when (failure.kind) {
    LyricsFailureKind.NETWORK -> stringResource(R.string.lyrics_error_network)
    LyricsFailureKind.TIMEOUT -> stringResource(R.string.lyrics_error_timeout)
    LyricsFailureKind.RATE_LIMIT -> failure.retryAfterSeconds?.let { stringResource(R.string.lyrics_error_rate_wait, it) }
        ?: stringResource(R.string.lyrics_error_rate)
    LyricsFailureKind.HTTP -> stringResource(R.string.lyrics_error_http, failure.httpCode ?: 0)
    LyricsFailureKind.INVALID_RESPONSE -> stringResource(R.string.lyrics_error_response)
    LyricsFailureKind.TOO_LARGE -> stringResource(R.string.lyrics_error_size)
    LyricsFailureKind.DISABLED -> stringResource(R.string.lyrics_error_disabled)
}
