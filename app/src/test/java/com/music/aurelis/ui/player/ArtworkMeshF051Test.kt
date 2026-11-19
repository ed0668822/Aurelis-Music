// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMeshF051Test {
    @Test fun seamUsesTheBottomEdgeOfTheActualCover() {
        val top = 0xFFDC2424.toInt()
        val bottom = 0xFF1855CE.toInt()
        val pixels = IntArray(36) { index -> if (index / 6 == 5) bottom else top }
        val grid = meshCellAverages(pixels, width = 6, height = 6, cols = 6, rows = 6)
        assertEquals(bottom, grid[0])
        assertEquals(bottom, grid[5])
        assertEquals(top, grid[30])
        assertEquals(top, grid[35])
    }

    @Test fun mostlyBlackCoverDoesNotBecomeAWholeScreenAccent() {
        val black = 0xFF080808.toInt()
        val red = 0xFFEE2020.toInt()
        val pixels = IntArray(36) { index -> if (index / 6 == 0) red else black }
        val grid = meshCellAverages(pixels, width = 6, height = 6, cols = 6, rows = 6)
        assertTrue(grid.count { it == black } >= 30)
        assertEquals(red, grid[30])
    }

    @Test fun uniformCoverStaysUniformBeforeTheTinyBlur() {
        val blue = 0xFF415889.toInt()
        val grid = meshCellAverages(IntArray(120 * 120) { blue }, 120, 120, 6, 6)
        assertTrue(grid.all { it == blue })
    }
}
