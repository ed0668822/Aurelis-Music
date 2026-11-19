// SPDX-FileCopyrightText: 2026 Aurelis
// SPDX-License-Identifier: LGPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.aurelis.data.AppFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** F057 font source and size contract; no Apple font is bundled by this test. */
class AurelisTypographyF057Test {
    @Test fun systemAndVazirmatnUseTheSameReadableRoleMetrics() {
        val system = typographyFor(AppFont.SYSTEM)
        val alternate = typographyFor(AppFont.VAZIRMATN)
        val roles = listOf(
            system.displayLarge to alternate.displayLarge,
            system.displayMedium to alternate.displayMedium,
            system.displaySmall to alternate.displaySmall,
            system.headlineLarge to alternate.headlineLarge,
            system.headlineMedium to alternate.headlineMedium,
            system.headlineSmall to alternate.headlineSmall,
            system.titleLarge to alternate.titleLarge,
            system.titleMedium to alternate.titleMedium,
            system.titleSmall to alternate.titleSmall,
            system.bodyLarge to alternate.bodyLarge,
            system.bodyMedium to alternate.bodyMedium,
            system.bodySmall to alternate.bodySmall,
            system.labelLarge to alternate.labelLarge,
            system.labelMedium to alternate.labelMedium,
            system.labelSmall to alternate.labelSmall,
        )
        assertEquals(15, roles.size)
        roles.forEach { (main, second) ->
            assertEquals(main.fontSize, second.fontSize)
            assertEquals(main.lineHeight, second.lineHeight)
            assertEquals(main.letterSpacing, second.letterSpacing)
            assertEquals(main.fontWeight, second.fontWeight)
            assertTrue(main.lineHeight.isSp)
            assertTrue(main.lineHeight.value >= main.fontSize.value)
        }
        assertEquals(FontFamily.SansSerif, system.titleLarge.fontFamily)
        assertNotEquals(system.bodyLarge.fontFamily, alternate.bodyLarge.fontFamily)
    }

    @Test fun sourceMatchedKeyTextRolesAreStableWithoutSfPro() {
        val face = typographyFor(AppFont.SYSTEM)
        assertEquals(34.sp, face.displayLarge.fontSize)
        assertEquals(30.sp, face.headlineLarge.fontSize)
        assertEquals(22.sp, face.headlineMedium.fontSize)
        assertEquals(20.sp, face.titleLarge.fontSize)
        assertEquals(16.sp, face.titleMedium.fontSize)
        assertEquals(16.sp, face.bodyLarge.fontSize)
        assertEquals(14.sp, face.bodyMedium.fontSize)
        assertEquals(FontWeight.W700, face.titleLarge.fontWeight)
    }
}
