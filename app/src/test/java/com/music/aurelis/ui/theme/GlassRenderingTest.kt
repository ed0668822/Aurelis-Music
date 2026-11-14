package com.music.aurelis.ui.theme

import android.graphics.RuntimeShader
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// API 35 selects Robolectric's Android V native runtime. Its older S runtime
// cannot parse child.eval(), even when emulating API 33; test exact source here.
class GlassRenderingTest {
    @Test fun everyRefractionAndHighlightProgramCompilesInTheNativeRenderer() {
        assertEquals(4, glassShaderPrograms.size)
        glassShaderPrograms.forEachIndexed { index, source ->
            try { RuntimeShader(source) } catch (e: IllegalArgumentException) {
                throw AssertionError("Program $index: ${e.message}", e)
            }
        }
        assertTrue(glassShadersAvailable())
    }
    @Test fun unsupportedRenderersAndReducedEffectsNeverRequestRefraction() {
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(true,false,false,true,true,true))
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(true,true,true,true,true,true))
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(false,false,true,true,true,true))
        assertEquals(GlassRenderMode.BLUR, glassRenderMode(true,false,true,true,false,true))
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(true,false,true,false,false,false))
        assertEquals(GlassRenderMode.REFRACTION, glassRenderMode(true,false,true,true,true,true))
    }
    @Test fun pageAndLensSamplingStopsWhenEffectsAreDisabledOrReduced() {
        assertTrue(shouldSampleGlass(true, false, true))
        assertFalse(shouldSampleGlass(false, false, true))
        assertFalse(shouldSampleGlass(true, true, true))
        assertFalse(shouldSampleGlass(true, false, false))
        // Retained backdrop/shader availability must not override the user's toggle.
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(false, false, true, true, true, false))
        assertEquals(GlassRenderMode.SOLID, glassRenderMode(true, true, true, true, true, false))
    }
}
