// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.replay

import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.ui.player.bestPartChorusStart

/** Replay and the local player's seek pill share one conservative algorithm. */
internal object ReplayBestPart {
    fun chorusStart(lines: List<LyricLine>, durationMs: Long): Long? =
        bestPartChorusStart(lines, durationMs)
}
