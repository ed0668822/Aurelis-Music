// SPDX-FileCopyrightText: © 2026 Aurelis

package com.music.aurelis.ui.theme

import androidx.compose.runtime.Composable

private object NoOrientation : ScreenOrientation {
    override fun landscape() = Unit
    override fun portrait() = Unit
}

// A desktop window has no orientation to turn.
@Composable
actual fun rememberScreenOrientation(): ScreenOrientation = NoOrientation
