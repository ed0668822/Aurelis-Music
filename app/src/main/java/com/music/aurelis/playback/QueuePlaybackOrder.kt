// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline

/** Read the actual one-pass Timeline order; null means it cannot be verified. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun queuePlaybackOrder(player: Player): List<Int>? {
    val count = player.mediaItemCount
    if (!player.shuffleModeEnabled) return (0 until count).toList()
    if (!player.isCommandAvailable(Player.COMMAND_GET_TIMELINE)) return null
    val timeline = player.currentTimeline
    if (timeline.windowCount != count) return null
    return timelinePlaybackOrder(timeline, count)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun timelinePlaybackOrder(timeline: Timeline, count: Int): List<Int>? {
    if (count < 0 || timeline.windowCount != count) return null
    val order = ArrayList<Int>(count)
    val seen = BooleanArray(count)
    var index = timeline.getFirstWindowIndex(true)
    while (index != C.INDEX_UNSET && order.size < count) {
        if (index !in 0 until count || seen[index]) return null
        seen[index] = true
        order += index
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
    }
    return order.takeIf { it.size == count && index == C.INDEX_UNSET }
}
