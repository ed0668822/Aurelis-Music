// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

object AurelisIcons {
    val MusicNote: ImageVector get() = AurelisReferenceGlyphs.MusicNote
    val Clock: ImageVector get() = AurelisReferenceGlyphs.Clock
    // Original geometric glyphs; no SF Symbols paths or restricted dependency.
    val Explore: ImageVector get() = AurelisReferenceGlyphs.Explore
    val Library: ImageVector get() = AurelisReferenceGlyphs.Library
    val Sort: ImageVector get() = Icons.Rounded.Sort
    val Refresh: ImageVector get() = Icons.Rounded.Refresh
    val Appearance: ImageVector get() = Icons.Rounded.Palette
    val Performance: ImageVector get() = Icons.Rounded.Speed
    val Storage: ImageVector get() = Icons.Rounded.Storage
    val Language: ImageVector get() = Icons.Rounded.Language
    val Info: ImageVector get() = Icons.Rounded.Info
    val Home: ImageVector get() = AurelisReferenceGlyphs.Home
    val Search: ImageVector get() = AurelisReferenceGlyphs.Search
    val Settings: ImageVector get() = Icons.Rounded.Settings
    val Play: ImageVector get() = AurelisReferenceGlyphs.Play
    val Pause: ImageVector get() = Icons.Rounded.Pause
    val SkipNext: ImageVector get() = Icons.Rounded.SkipNext
    val SkipPrevious: ImageVector get() = Icons.Rounded.SkipPrevious
    val Shuffle: ImageVector get() = AurelisReferenceGlyphs.Shuffle
    val Repeat: ImageVector get() = AurelisReferenceGlyphs.Repeat
    val Heart: ImageVector get() = AurelisReferenceGlyphs.HeartFilled
    val HeartBorder: ImageVector get() = AurelisReferenceGlyphs.Heart
    val Queue: ImageVector get() = AurelisReferenceGlyphs.Queue
    val Lyrics: ImageVector get() = AurelisReferenceGlyphs.Lyrics
    val Equalizer: ImageVector get() = Icons.Rounded.Equalizer
    val Volume: ImageVector get() = Icons.AutoMirrored.Filled.VolumeUp
    val Folder: ImageVector get() = Icons.Rounded.Folder
    val Album: ImageVector get() = Icons.Rounded.Album
    val Artist: ImageVector get() = Icons.Rounded.Person
    val Playlist: ImageVector get() = Icons.AutoMirrored.Filled.PlaylistPlay
    val MoreVert: ImageVector get() = Icons.Rounded.MoreVert
    val ArrowBack: ImageVector by lazy { glyph("M15,4 L7,12 L15,20", line = true, mirror = true) }
    val ChevronRight: ImageVector get() = AurelisReferenceGlyphs.ChevronRight
    val ChevronLeft: ImageVector get() = Icons.Rounded.ChevronLeft
    val Close: ImageVector get() = Icons.Rounded.Close
    val Check: ImageVector get() = AurelisReferenceGlyphs.Check
    val Add: ImageVector get() = AurelisReferenceGlyphs.Plus
    val Delete: ImageVector get() = Icons.Rounded.Delete
    val Share: ImageVector get() = Icons.Rounded.Share
    private fun glyph(data: String, line: Boolean = false, mirror: Boolean = false): ImageVector =
        ImageVector.Builder("AurelisGlyph", 24.dp, 24.dp, 24f, 24f, autoMirror = mirror)
            .addPath(PathParser().parsePathString(data).toNodes(),
                fill = if (line) null else SolidColor(Color.Black),
                stroke = if (line) SolidColor(Color.Black) else null,
                strokeLineWidth = if (line) 3f else 0f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()
}
