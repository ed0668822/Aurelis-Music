// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * F060: the old single static loading block now uses the same BitChord-style
 * low-cost shimmer as the real song, shelf and detail placeholders.
 * It is only shown while its consumer is loading. ShimmerBox automatically
 * becomes a motionless solid placeholder under reduced-motion settings.
 */
@Composable
fun SkeletonLoader(modifier: Modifier = Modifier) {
    ShimmerBox(
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(12.dp),
    )
}
