// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.music.aurelis.playback

import com.music.aurelis.data.sources.TrackMatcher

/**
 * The runtime a queued item states for itself, in milliseconds, or 0 when it
 * states none.
 *
 * Two places can carry it. A streamed track puts it on its playback URI as
 * `d=<seconds>`. A file on the device has no such parameter, so it used to read
 * as "unknown" until the player prepared it a few seconds before it played — and
 * Automix plans the whole pair around the incoming runtime. The queue row's own
 * "3:45" text is on the item either way, so it stands in when the URI says
 * nothing.
 */
internal fun queuedDurationMillis(uriSeconds: String?, durationText: String?): Long {
    uriSeconds?.trim()?.toLongOrNull()?.takeIf { it > 0L }?.let { return it * 1000L }
    return TrackMatcher.secondsOf(durationText)?.let { it * 1000L } ?: 0L
}
