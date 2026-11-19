// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import android.content.Context
import android.media.MediaRouter2
import android.os.Build

/**
 * F047: delegate selection to Android's real media routing UI.
 * Returning false means nothing was selected; never mutate AudioRouting.selectedId
 * or make a device look active before the platform confirms a route.
 */
internal fun showAndroidOutputSwitcher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return false
    return runCatching {
        MediaRouter2.getInstance(context).showSystemOutputSwitcher()
    }.getOrDefault(false)
}
