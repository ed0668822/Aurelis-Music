// Aurelis © 2026. GPL-3.0-or-later.
// Adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898; original contributors, GPLv3.
package com.music.aurelis.ui.player

import android.content.res.Resources
import android.view.Window
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

interface MiniPlayerPull {
    fun beginDrag() = Unit
    fun drag(delta: Float)
    fun release(velocity: Float)
    fun cancel() = Unit
}

private const val SLIDE_STIFFNESS = 195f
private const val SLIDE_SETTLED_PX = 0.5f
private const val SHEET_VELOCITY_FRESH_MS = 80f
private const val LAYOUT_WAIT_MS = 300L
private const val WINDOW_ENTER_MS = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class PlayerSheetMotion(
    private val scope: CoroutineScope,
    private val dock: PlayerDock,
    private val shown: () -> Boolean,
    private val setShown: (Boolean) -> Unit,
    private val inlineMode: Boolean = false,
    private val reduceMotion: () -> Boolean = { false },
) : MiniPlayerPull {
    var held by mutableFloatStateOf(Float.NaN)
        private set

    val holding: Boolean get() = !held.isNaN()
    var sheet: SheetState? = null
    private var window: Window? = null
    private var windowAnimations = 0
    private var windowRestore: Job? = null
    private var pullStartNanos = 0L
    private var pullStartAt = 0f
    private var sheetSampleAt = Float.NaN
    private var sheetSampleNanos = 0L
    private var sheetVelocity = 0f

    var laidOut by mutableStateOf(false)
    var flingVelocity = 0f
    private var settle: Job? = null
    private var closing = false
    private var pullStartedShown = false

    /**
     * Altura disponible para el sheet. Usa [PlayerDock.sheetTravel] si ya fue
     * medido; si no, cae a la altura de pantalla del sistema como fallback.
     * (`WindowInfo.containerSize` no existe en Compose 1.7.6; se reemplazó.)
     */
    private fun travel(): Float = dock.sheetTravel.takeIf { it.isFinite() && it > 0f }
        ?: Resources.getSystem().displayMetrics.heightPixels.coerceAtLeast(1).toFloat()

    private fun sheetOffset(): Float = try {
        sheet?.requireOffset() ?: Float.NaN
    } catch (_: IllegalStateException) {
        Float.NaN
    }

    fun position(): Float = if (holding) held else if (inlineMode) {
        if (shown()) 0f else travel()
    } else sheetOffset()

    /** For routes opened externally (restoration, Home shortcut or return from Queue). */
    fun reveal() {
        if (!inlineMode || !shown() || holding) return
        closing = false
        held = travel()
        settle?.cancel()
        settle = scope.launch {
            withTimeoutOrNull(LAYOUT_WAIT_MS) { snapshotFlow { laidOut }.first { it } }
            slide(held, 0f)
            held = Float.NaN
        }
    }

    fun contentOffsetPx(): Int {
        val target = held
        val sheetAt = sheetOffset().takeUnless { it.isNaN() } ?: 0f
        if (target.isNaN()) {
            sampleSheet(sheetAt)
            return 0
        }
        return target.toInt() - sheetAt.toInt()
    }

    private fun sampleSheet(at: Float) {
        val now = System.nanoTime()
        if (!sheetSampleAt.isNaN()) {
            val seconds = (now - sheetSampleNanos) / 1e9f
            if (seconds > 0f) {
                val instant = (at - sheetSampleAt) / seconds
                sheetVelocity = sheetVelocity * 0.4f + instant * 0.6f
            }
        }
        sheetSampleAt = at
        sheetSampleNanos = now
    }

    private fun recentSheetVelocity(): Float =
        if ((System.nanoTime() - sheetSampleNanos) / 1e6f < SHEET_VELOCITY_FRESH_MS) sheetVelocity else 0f

    fun open() {
        if (shown()) return
        settle?.cancel()
        closing = false
        val travel = travel()
        held = travel
        setShown(true)
        settle = scope.launch {
            withTimeoutOrNull(LAYOUT_WAIT_MS) { snapshotFlow { laidOut }.first { it } }
            slide(from = travel, to = 0f)
            held = Float.NaN
        }
    }

    fun attachWindow(window: Window?) {
        this.window = window ?: return
        windowAnimations = window.attributes.windowAnimations
        window.setWindowAnimations(0)
        windowRestore?.cancel()
        windowRestore = scope.launch {
            delay(WINDOW_ENTER_MS)
            window.setWindowAnimations(windowAnimations)
        }
    }

    private fun quietWindow() {
        windowRestore?.cancel()
        window?.setWindowAnimations(0)
    }

    fun close() {
        if (!shown() || closing) return
        closing = true
        quietWindow()
        settle?.cancel()
        val from = position().takeUnless { it.isNaN() } ?: run {
            dismissQuietly()
            return
        }
        val velocity = if (holding) 0f else recentSheetVelocity()
        held = from
        settle = scope.launch {
            slide(from = from, to = travel(), velocity = velocity.coerceAtLeast(0f))
            dismissQuietly()
        }
    }

    override fun beginDrag() {
        if (!inlineMode) return
        pullStartedShown = shown()
        pullStartAt = position()
        pullStartNanos = System.nanoTime()
        settle?.cancel()
        closing = false
        if (pullStartedShown && !holding) held = 0f
    }

    override fun drag(delta: Float) {
        if (!delta.isFinite()) return
        if (!holding) {
            if ((!shown() && delta >= 0f) || (shown() && !inlineMode)) return
            settle?.cancel()
            closing = false
            pullStartedShown = shown()
            held = if (pullStartedShown) 0f else travel()
            pullStartAt = held
            pullStartNanos = System.nanoTime()
            if (!pullStartedShown) setShown(true)
        }
        settle?.cancel()
        held = (held + delta).coerceIn(0f, travel())
    }

    override fun release(velocity: Float) {
        if (!holding) return
        val travel = travel()
        val seconds = (System.nanoTime() - pullStartNanos) / 1e9f
        val average = if (seconds > 0f) (held - pullStartAt) / seconds else 0f
        val supplied = velocity.takeIf { it.isFinite() } ?: 0f
        val speed = (if (abs(average) > abs(supplied)) average else supplied).takeIf { it.isFinite() } ?: 0f
        val flick = flingVelocity.isFinite() && flingVelocity > 0f && abs(speed) >= flingVelocity
        val open = if (flick) speed < 0f else held < travel / 2f
        if (!open) {
            closing = true
            quietWindow()
        }
        val from = held
        settle = scope.launch {
            slide(from = from, to = if (open) 0f else travel, velocity = speed)
            if (open) held = Float.NaN else dismissQuietly()
        }
    }

    override fun cancel() {
        if (!inlineMode || !holding) return
        settle?.cancel()
        val from = held
        settle = scope.launch {
            slide(from, if (pullStartedShown) 0f else travel())
            if (pullStartedShown) held = Float.NaN else dismissQuietly()
        }
    }

    fun onSheetGone() {
        closing = false
        sheetSampleAt = Float.NaN
        sheetVelocity = 0f
        settle?.cancel()
        windowRestore?.cancel()
        held = Float.NaN
        laidOut = false
        sheet = null
        window = null
    }

    private suspend fun slide(from: Float, to: Float, velocity: Float = 0f) {
        val travel = travel()
        if (reduceMotion()) { held = to.coerceIn(0f, travel); return }
        animate(
            initialValue = from,
            targetValue = to,
            initialVelocity = velocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = SLIDE_STIFFNESS,
                visibilityThreshold = SLIDE_SETTLED_PX,
            ),
        ) { value, _ ->
            held = value.coerceIn(0f, travel)
        }
    }

    private fun dismissQuietly() {
        quietWindow()
        setShown(false)
        // A pull can be cancelled before the first player frame was composed.
        // Cleanup must not depend on that frame ever installing onDispose.
        if (inlineMode) { held = Float.NaN; closing = false; laidOut = false }
    }
}
