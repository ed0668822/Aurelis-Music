// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Original 24dp stroked icons for the local BitChord-style song menu.
 * These do not embed proprietary SF Symbols paths.
 */
internal object AurelisMenuIcons {
    private val ink = SolidColor(Color.Black)
    private fun glyph(name: String, outline: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(outline), stroke = ink,
                strokeLineWidth = 2.1f, strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round)
        }.build()

    val Star get() = AurelisPlayerIcons.Star
    val StarFilled get() = AurelisPlayerIcons.StarFilled
    val PlaylistAdd by lazy { glyph("menu_playlist_add",
        "M3 5 H14 M3 10 H14 M3 15 H11 M17 12 V21 M13 16.5 H21") }
    val PlayNext by lazy { glyph("menu_play_next",
        "M9 6 H21 M9 12 H21 M9 18 H21 M3 5 L7 8 L3 11 Z") }
    val PlayLast by lazy { glyph("menu_play_last",
        "M9 6 H21 M9 12 H21 M9 18 H21 M3 13 L7 16 L3 19 Z") }
    val Radio by lazy { glyph("menu_radio",
        "M12 10.5 A1.5 1.5 0 1 1 12 13.5 A1.5 1.5 0 1 1 12 10.5 Z M8.5 8.5 Q5.5 12 8.5 15.5 M15.5 8.5 Q18.5 12 15.5 15.5 M5.5 5 Q0 12 5.5 19 M18.5 5 Q24 12 18.5 19") }
    val Album by lazy { glyph("menu_album", "M7 3 H20 V16 H7 Z M4 7 V20 H17") }
    val Artist by lazy { glyph("menu_artist",
        "M12 3 Q9.5 3 9.5 6 V12 Q9.5 14.5 12 14.5 Q14.5 14.5 14.5 12 V6 Q14.5 3 12 3 Z M6.5 10.5 Q6.5 18 12 18 Q17.5 18 17.5 10.5 M12 18 V21 M8 21 H16") }
    val Info by lazy { glyph("menu_info",
        "M12 3 A9 9 0 1 1 12 21 A9 9 0 1 1 12 3 Z M12 10.5 V16.5 M12 7.6 V7.7") }
    val SleepTimer by lazy { glyph("menu_sleep",
        "M15.5 3.5 Q10 7.5 12.5 13 Q14.5 18 20 17.5 Q17.5 21 12 21 Q4 21 4 13 Q4 7 9 4.5 M17 3 H22 L17 8 H22") }
    val LyricsOffset by lazy { glyph("menu_offset",
        "M3 6 H21 M3 12 H21 M3 18 H21 M8 3 V9 M16 9 V15 M10 15 V21") }
    val PlaybackTools by lazy { glyph("menu_tools",
        "M4 6 H20 M4 12 H20 M4 18 H20 M8 4 V8 M15 10 V14 M10 16 V20") }
    val MixOverview by lazy { glyph("menu_mix",
        "M3 10 V14 M7 6 V18 M11 3 V21 M15 8 V16 M19 5 V19 M22 10 V14") }
    val CopyDiagnostic by lazy { glyph("menu_diagnostic",
        "M7 4 H17 V20 H7 Z M10 4 V2 H14 V4 M10 9 H14 M10 13 H14 M10 17 H14") }
    val More by lazy { glyph("menu_more", "M5 12 H5.1 M12 12 H12.1 M19 12 H19.1") }
}
