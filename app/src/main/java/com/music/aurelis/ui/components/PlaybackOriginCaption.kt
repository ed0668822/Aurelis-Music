// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
// F045: caption design adapted from BitChord PlayerControls.kt (GPLv3); see NOTICE.
package com.music.aurelis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun PlaybackOriginCaption(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 4f),
        ),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    )
}
