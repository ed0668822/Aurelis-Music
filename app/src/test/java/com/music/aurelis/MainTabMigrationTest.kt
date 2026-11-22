// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis

import com.music.aurelis.data.settings.normalizeMainTabIndex
import org.junit.Assert.assertEquals
import org.junit.Test

/** UI-04: upgrade compatibility for the removed fifth Radio tab. */
class MainTabMigrationTest {
    @Test fun fourRemainingTabIndexesRemainStable() {
        for (tab in 0..3) assertEquals(tab, normalizeMainTabIndex(tab))
    }

    @Test fun previouslySavedRadioReturnsToHome() {
        assertEquals(0, normalizeMainTabIndex(4))
    }

    @Test fun corruptOrFutureValuesCannotIndexPastNavigation() {
        assertEquals(0, normalizeMainTabIndex(-1))
        assertEquals(0, normalizeMainTabIndex(17))
        assertEquals(0, normalizeMainTabIndex(Int.MAX_VALUE))
    }
}
