package com.music.aurelis.ui.components.backdrop

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.components.backdrop.effects.lens
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = AurelisApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropSizeRegressionTest {
    private fun scope(shape: Shape = RoundedCornerShape(20.dp)) = object : BackdropEffectScopeImpl() {
        override val shape = shape
    }
    private val invalidSizes = listOf(Size.Unspecified, Size.Zero, Size(0f, 40f),
        Size(40f, -1f), Size(Float.POSITIVE_INFINITY, 40f), Size(40f, Float.NaN))

    @Test fun attachingBeforeMeasurementDoesNotEvaluateEffects() {
        val scope = scope()
        var calls = 0
        // The same entry point invoked by DrawBackdropNode.onAttach -> updateEffects.
        scope.apply { calls++; lens(12f, 8f, chromaticAberration = true) }
        assertEquals(0, calls)
        assertNull(scope.renderEffect)
        assertEquals(0f, scope.padding)
    }

    @Test fun firstDrawAndResizeKeepRealRefractionEnabled() {
        val scope = scope()
        val canvas = Canvas(ImageBitmap(240, 120))
        val drawScope = CanvasDrawScope()
        val measured = mutableListOf<Size>()
        for ((size, scale) in listOf(Size(120f, 60f) to 1f, Size(240f, 120f) to 0.5f,
            Size(200f, 80f) to 1f)) {
            drawScope.draw(Density(2f), LayoutDirection.Rtl, canvas, size) {
                scope.update(this, scale)
                scope.apply { measured += this.size; lens(12f, 8f, depthEffect = true, chromaticAberration = true) }
            }
            assertNotNull("Measured lens must produce a real RenderEffect", scope.renderEffect)
            assertEquals(2f, scope.density)
            assertEquals(LayoutDirection.Rtl, scope.layoutDirection)
        }
        assertEquals(listOf(Size(120f,60f), Size(120f,60f), Size(200f,80f)), measured)
    }

    @Test fun invalidDimensionsClearPreviousEffectsWithoutEvaluatingCallbacks() {
        for (invalid in invalidSizes) {
            val scope = scope()
            scope.size = Size(120f, 60f)
            scope.apply { lens(12f, 8f); padding = 4f }
            assertNotNull(scope.renderEffect)
            scope.size = invalid
            var calls = 0
            scope.apply { calls++; lens(12f, 8f) }
            assertEquals("Invalid size: $invalid", 0, calls)
            assertNull(scope.renderEffect)
            assertEquals(0f, scope.padding)
        }
    }

    @Test fun detachResetAndReattachWaitForNewMeasurements() {
        val scope = scope()
        scope.size = Size(120f, 60f)
        scope.apply { lens(12f, 8f) }
        assertNotNull(scope.renderEffect)
        scope.reset()
        scope.apply { lens(12f, 8f) }
        assertNull(scope.renderEffect)
        scope.size = Size(80f, 160f)
        scope.apply { lens(12f, 8f) }
        assertNotNull(scope.renderEffect)
    }

    @Test fun directLensCallsAlsoRejectInvalidDimensionsForBothCornerShapes() {
        for (shape in listOf(RoundedCornerShape(50), AbsoluteRoundedCornerShape(20.dp))) {
            val scope = scope(shape)
            for (invalid in invalidSizes) {
                scope.size = invalid
                scope.lens(12f, 8f)
                assertNull(scope.renderEffect)
            }
        }
    }

    @Test fun unrelatedErrorsForValidDimensionsRemainVisible() {
        val scope = scope()
        scope.size = Size(120f, 60f)
        val failure = assertThrows(IllegalStateException::class.java) {
            scope.apply { error("unexpected effect failure") }
        }
        assertEquals("unexpected effect failure", failure.message)
    }
}
