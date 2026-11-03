// Aurelis © 2026. GPL-3.0-or-later
/*
 * Floating tab bar with inline/expanded behaviour, adapted from BitChord's
 * FloatingTabBar. The bar collapses inline as the page scrolls down and
 * expands back as it scrolls up, driven by [FloatingTabBarScrollConnection].
 */
package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.floatAnimatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.music.aurelis.ui.components.glassContentColor

/**
 * A floating tab bar that can collapse inline (tabs fold to the selected one)
 * or expand (full tab pill) as the page scrolls.
 *
 * @param selectedTabKey currently selected tab index
 * @param scrollConnection connection to the page's scroll (optional)
 * @param modifier modifier for the bar
 * @param tabBarContentModifier modifier applied to the tab pill surface
 * @param inlineAccessory content shown in the collapsed row (e.g. now playing)
 * @param expandedAccessory content shown in the expanded row
 * @param colors background, accessory background and indicator colours
 * @param elevations inline and expanded elevations
 * @param sizes padding and spacing sizes
 * @param contentKey key for remembering state across recompositions
 * @param content tab content slot
 */
@Composable
fun FloatingTabBar(
    selectedTabKey: Int,
    scrollConnection: FloatingTabBarScrollConnection? = null,
    modifier: Modifier = Modifier,
    tabBarContentModifier: Modifier = Modifier,
    inlineAccessory: (@Composable (Modifier, Any?) -> Unit)? = null,
    expandedAccessory: (@Composable (Modifier, Any?) -> Unit)? = null,
    colors: FloatingTabBarDefaults.Colors = FloatingTabBarDefaults.colors(),
    elevations: FloatingTabBarDefaults.Elevations = FloatingTabBarDefaults.elevations(),
    sizes: FloatingTabBarDefaults.Sizes = FloatingTabBarDefaults.sizes(),
    contentKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val contentColor = glassContentColor()
    val collapsed by remember { floatAnimatable(0f) }
    val transition = updateTransition(selectedTabKey, label = "FloatingTabBar transition")
    val expanded by transition.animateFloat(
        transitionSpec = { _, _ -> tween(220, easing = FastOutSlowInEasing) },
        label = "expanded",
    ) { if (it == selectedTabKey) 1f else 0f }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(sizes.tabBarContentPadding)
                .then(tabBarContentModifier)
                .background(colors.backgroundColor, shape = RoundedCornerShape(percent = 50))
                .clip(RoundedCornerShape(percent = 50)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                content()
            }
        }

        // Inline accessory (shown when collapsed)
        inlineAccessory?.let {
            AnimatedVisibility(
                visible = expanded < 0.5f,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)),
            ) {
                it(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    null,
                )
            }
        }

        // Expanded accessory (shown when expanded)
        expandedAccessory?.let {
            AnimatedVisibility(
                visible = expanded >= 0.5f,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)),
            ) {
                it(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    null,
                )
            }
        }
    }
}

/** A single tab item within [FloatingTabBar]. */
@Composable
fun FloatingTabItem(
    key: Int,
    icon: androidx.compose.ui.graphics.ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = androidx.compose.ui.Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

private fun Dp.toPx(density: Density): Float = with(density) { this.toPx() }
