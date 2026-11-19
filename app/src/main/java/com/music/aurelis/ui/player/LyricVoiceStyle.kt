// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection

/** Detect the lyrics' script independently of Android's interface locale. */
internal fun lyricTextIsRtl(text: String): Boolean {
    var rtl = 0
    var ltr = 0
    for (ch in text) {
        if (ch.code in 0xFB1D..0xFB4F) return true
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> rtl++
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> ltr++
        }
    }
    return rtl > ltr
}

/** The duet owns a physical side, while each lyric retains its own bidi text shaping. */
internal fun lyricVoiceStyle(
    base: TextStyle,
    text: String,
    alignEnd: Boolean,
    laneLocked: Boolean,
): TextStyle {
    val rtl = lyricTextIsRtl(text)
    val right = if (laneLocked) alignEnd else rtl
    return base.copy(
        textAlign = if (right) TextAlign.Right else TextAlign.Left,
        textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr,
    )
}
