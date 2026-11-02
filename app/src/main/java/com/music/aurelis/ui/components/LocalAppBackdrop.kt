// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.music.aurelis.ui.components.backdrop.Backdrop

val LocalAppBackdrop = staticCompositionLocalOf<Backdrop> {
    error("LocalAppBackdrop no fue proporcionado")
}
