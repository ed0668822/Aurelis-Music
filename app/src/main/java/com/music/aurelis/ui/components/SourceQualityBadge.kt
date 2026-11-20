// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.R

/**
 * BitChord-style source label: a small lossless-wave symbol plus text, without
 * a pill or dynamic claims about the actual Android output route.
 */
@Composable
fun SourceQualityBadge(
    qualityText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (qualityText.isBlank()) return
    val action = stringResource(R.string.mini_quality_action)
    val click = if (onClick != null) Modifier
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = "$action: $qualityText" }
        else Modifier
    Row(
        modifier = modifier
            .heightIn(min = 22.dp)
            .testTag("player-source-quality")
            .then(click),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (qualityText.endsWith("Lossless")) {
            Icon(
                painter = painterResource(R.drawable.ic_lossless),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(width = 15.dp, height = 8.dp),
            )
        }
        Text(
            text = qualityText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 14.sp,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
