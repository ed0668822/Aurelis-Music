// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

/** Active local installation state; no remote/download state for on-device music. */
interface AppUiHost {
    val libraries: StateFlow<com.music.aurelis.data.LocalUserLibrary>
}

object AppUi {
    @Volatile
    private var installed: AppUiHost? = null

    fun install(host: AppUiHost) {
        installed = host
    }

    val host: AppUiHost
        get() = checkNotNull(installed) { "AppUi.install was not called" }
}

val LocalPullToRefreshEnabled = staticCompositionLocalOf { true }
