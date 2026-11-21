// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

/**
 * 1 means next track enters from the right; -1 means previous track enters
 * from the left. Repeat-all wraps preserve their real transport direction.
 */
internal fun songInfoSlideDirection(fromIndex: Int, toIndex: Int, queueSize: Int): Int {
    if (queueSize < 2 || fromIndex !in 0 until queueSize || toIndex !in 0 until queueSize) return 1
    if (fromIndex == queueSize - 1 && toIndex == 0) return 1
    if (fromIndex == 0 && toIndex == queueSize - 1) return -1
    return if (toIndex < fromIndex) -1 else 1
}
