// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components.floatingtabbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FloatingTabBar(
    selectedTabKey: Int,
    modifier: Modifier = Modifier,
    scrollConnection: FloatingTabBarScrollConnection? = null,
    tabBarContentModifier: Modifier = Modifier,
    inlineAccessory: (@Composable (Modifier, Any?) -> Unit)? = null,
    expandedAccessory: (@Composable (Modifier, Any?) -> Unit)? = null,
    colors: FloatingTabBarDefaults.Colors = FloatingTabBarDefaults.colors(),
    elevations: FloatingTabBarDefaults.Elevations = FloatingTabBarDefaults.elevations(),
    sizes: FloatingTabBarDefaults.Sizes = FloatingTabBarDefaults.sizes(),
    contentKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val transition = updateTransition(selectedTabKey, label = "FloatingTabBar transition")

    // FIX: transitionSpec en Transition.animateFloat es
    // @Composable Transition.Segment<S>.() -> FiniteAnimationSpec<Float>
    // (receiver), NO (from, to) -> spec.
    val expanded by transition.animateFloat(
        transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
        label = "expanded",
    ) { state -> if (state == selectedTabKey) 1f else 0f }

    Column(modifier = modifier.fillMaxWidth()) {
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

        inlineAccessory?.let {
            AnimatedVisibility(
                visible = expanded < 0.5f,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)),
            ) {
                it(Modifier.fillMaxWidth().padding(top = 8.dp), null)
            }
        }

        expandedAccessory?.let {
            AnimatedVisibility(
                visible = expanded >= 0.5f,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)),
            ) {
                it(Modifier.fillMaxWidth().padding(top = 8.dp), null)
            }
        }
    }
}

@Composable
fun FloatingTabItem(
    key: Int,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
