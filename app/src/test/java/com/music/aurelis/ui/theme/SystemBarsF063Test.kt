// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Activity
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SystemBarsF063Test {
    @Test fun activityWindowIsFoundThroughContextWrapper() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = android.view.ContextThemeWrapper(activity, android.R.style.Theme_Material)
        assertSame(activity.window, findSystemBarWindow(View(wrapped)))
        assertSame(activity.window, findSystemBarWindow(View(activity)))
    }
    @Test fun scrimUsesCoverTopLuminanceWithoutExtrapolating() {
        assertEquals(0.16f, topBandScrimAlpha(null), 0.0001f)
        assertEquals(0.405f, topBandScrimAlpha(0.5f), 0.0001f)
        assertEquals(0.65f, topBandScrimAlpha(1f), 0.0001f)
        assertEquals(0.65f, topBandScrimAlpha(3f), 0.0001f)
    }
}