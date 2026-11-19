// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

/**
 * Match BitChord's automatic following policy without resuming while paused,
 * during direct interaction, or when the user explicitly disabled following.
 */
internal fun lyricFollowResumeDelay(
    playing: Boolean,
    following: Boolean,
    manuallyPaused: Boolean,
    dragging: Boolean,
    scrolling: Boolean,
    picking: Boolean,
    activeOnScreen: Boolean,
): Long? {
    if (!playing || following || manuallyPaused || dragging || scrolling || picking) return null
    return if (activeOnScreen) 600L else 5_000L
}
