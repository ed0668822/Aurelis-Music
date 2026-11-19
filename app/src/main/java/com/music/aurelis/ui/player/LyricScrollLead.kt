// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import com.music.aurelis.data.lyrics.LyricLine

/** BitChord's anticipatory window applies only to scroll focus, never to the lyric sweep. */
internal fun lyricScrollLeadMs(lines: List<LyricLine>, positionMs: Long): Long {
    val current = lines.indexOfLast { it.timeMs <= positionMs }
    if (current < 0) return 350L
    val next = lines.getOrNull(current + 1) ?: return 350L
    return (next.timeMs - lines[current].endMs).coerceIn(350L, 500L)
}
