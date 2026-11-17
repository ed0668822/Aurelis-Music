// Aurelis © 2026. GPL-3.0-or-later
// Presentation adapted from BitChord b690dc146009d4815825fa4a20815ef2e4ee8898 (GPLv3).
// Original BitChord contributors and terms retained; local profile/permission integration by Aurelis.
package com.music.aurelis.ui.onboarding

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.R
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.player.*
import com.music.aurelis.ui.theme.LocalReduceMotion
import com.music.aurelis.ui.theme.SystemBarIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_NAME = 0
private const val PAGE_PERMISSION = 1
private const val NAME_LIMIT = 60
internal const val LOCAL_ONBOARDING_COMPLETE = "local_onboarding_complete"

/** SharedPreferences can update memory even when its disk commit returns false. */
internal fun persistOnboardingCompletion(preferences: SharedPreferences): Boolean {
    try {
        val saved = preferences.edit().putBoolean(LOCAL_ONBOARDING_COMPLETE, true).commit()
        if (!saved) preferences.edit().remove(LOCAL_ONBOARDING_COMPLETE).commit()
        return saved
    } catch (error: Exception) {
        runCatching { preferences.edit().remove(LOCAL_ONBOARDING_COMPLETE).commit() }
        throw error
    }
}

/** Uses the existing installation's app_meta preferences, never another profile. */
@Composable
internal fun LocalOnboardingGate(
    preferences: SharedPreferences,
    initialName: String,
    profileReadOnly: Boolean,
    onNameChosen: (String) -> Unit,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    content: @Composable () -> Unit,
) {
    var completed by remember(preferences) {
        mutableStateOf(runCatching { preferences.getBoolean(LOCAL_ONBOARDING_COMPLETE, false) }.getOrDefault(false))
    }
    var finishing by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val failure = stringResource(R.string.onboarding_save_error)
    val scope = rememberCoroutineScope()
    val reduced = LocalReduceMotion.current
    val visible = remember { MutableTransitionState(!completed) }
    visible.targetState = !completed
    SystemBarIcons(dark = completed && MaterialTheme.colorScheme.background.luminance() > 0.5f)
    Box(Modifier.fillMaxSize()) {
        // No underlying navigation, accessibility actions or modal can steal input.
        if (completed) content()
        AnimatedVisibility(visibleState = visible, enter = EnterTransition.None,
            exit = if (reduced) ExitTransition.None else slideOutVertically(tween(560)) { it }) {
            OnboardingScreen(permissionGranted, onRequestPermission,
                onFinish = {
                    if (!finishing && !completed) {
                        finishing = true
                        error = null
                        scope.launch {
                            try {
                                val saved = withContext(Dispatchers.IO) {
                                    persistOnboardingCompletion(preferences)
                                }
                                if (saved) completed = true else error = failure
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                error = failure
                            } finally {
                                finishing = false
                            }
                        }
                    }
                }, initialName = initialName, onNameChosen = onNameChosen,
                profileReadOnly = profileReadOnly, finishing = finishing, errorMessage = error)
        }
    }
}

@Composable
fun OnboardingScreen(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    onNameChosen: (String) -> Unit = {},
    profileReadOnly: Boolean = false,
    finishing: Boolean = false,
    errorMessage: String? = null,
) {
    var page by rememberSaveable { mutableIntStateOf(PAGE_NAME) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    val failure = stringResource(R.string.onboarding_save_error)
    val focusManager = LocalFocusManager.current
    val reduced = LocalReduceMotion.current
    BackHandler(enabled = page == PAGE_PERMISSION) { if (!finishing) page = PAGE_NAME }
    val palette = cyclingMeshPalette(WelcomeMeshPalettes, reduceAnimation = reduced)
    Box(modifier.fillMaxSize().background(Color.Black).testTag("local-onboarding")
        .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }) {
        MeshGradientBackground(palette, trackKey = page, continuous = !reduced,
            driftMillis = 6_000, colorFadeMillis = MESH_CYCLE_MILLIS,
            animated = !reduced, reduceAnimation = reduced)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.05f), 0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.45f))))
        AnimatedContent(targetState = page, transitionSpec = {
            val forward = targetState > initialState
            val slide = tween<IntOffset>(420)
            if (reduced) EnterTransition.None togetherWith ExitTransition.None
            else (slideInHorizontally(slide) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320, 100)))
                .togetherWith(slideOutHorizontally(slide) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
        }, label = "onboardingPage", modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) { shown ->
            OnboardingPage {
                if (shown == PAGE_NAME) NamePage(name,
                    onNameChange = {
                        val clipped = it.take(NAME_LIMIT)
                        name = if (clipped.isNotEmpty() && Character.isHighSurrogate(clipped.last())) clipped.dropLast(1) else clipped
                        nameError = null
                    },
                    onNext = next@{
                        val chosen = name.trim()
                        if (chosen.isEmpty() || finishing) return@next
                        try {
                            if (!profileReadOnly) onNameChosen(chosen)
                            focusManager.clearFocus()
                            page = PAGE_PERMISSION
                        } catch (_: Exception) { nameError = failure }
                    }, readOnly = profileReadOnly, error = nameError)
                else PermissionPage({ page = PAGE_NAME }, permissionGranted,
                    onRequestPermission, onFinish, finishing, errorMessage)
            }
        }
    }
}

@Composable
private fun OnboardingPage(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val viewportHeight = maxHeight
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).heightIn(min = viewportHeight)
            .padding(horizontal = 28.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.NamePage(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
    readOnly: Boolean,
    error: String?,
) {
    // With the keyboard up there is room for the title or the logo, not both;
    // the title is the one that says where you are.
    val keyboardUp = WindowInsets.isImeVisible
    val reduced = LocalReduceMotion.current
    AnimatedVisibility(
        visible = !keyboardUp,
        enter = if (reduced) EnterTransition.None else fadeIn() + expandVertically(),
        exit = if (reduced) ExitTransition.None else fadeOut() + shrinkVertically(),
    ) {
        Column {
            Spacer(Modifier.height(56.dp))
            Icon(AurelisIcons.MusicNote, null, tint = Color.White,
                modifier = Modifier.size(width = 46.dp, height = 30.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
    Title(stringResource(R.string.onboarding_welcome_title))

    Spacer(Modifier.weight(1f))

    Text(
        text = stringResource(R.string.onboarding_name_prompt),
        style = MaterialTheme.typography.titleMedium,
        fontSize = 17.sp,
        color = Color.White.copy(alpha = 0.78f),
        modifier = Modifier.padding(start = 4.dp),
    )
    Spacer(Modifier.height(12.dp))
    NameField(value = name, onValueChange = onNameChange, onDone = onNext, readOnly = readOnly)
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(if (readOnly) R.string.settings_profile_read_only_hint else R.string.onboarding_name_footer),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(28.dp))
    if (error != null) Text(error, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
    PillButton(
        label = stringResource(R.string.onboarding_next),
        onClick = onNext,
        enabled = name.isNotBlank(),
        primary = true,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, readOnly: Boolean) {
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
        color = Color.White,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.onboarding_name_hint),
                style = style,
                color = Color.White.copy(alpha = 0.38f),
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            readOnly = readOnly,
            textStyle = style,
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.PermissionPage(
    onBack: () -> Unit,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit,
    finishing: Boolean,
    error: String?,
) {
    Spacer(Modifier.height(16.dp))
    BackButton(onBack, enabled = !finishing)
    Spacer(Modifier.height(20.dp))
    Title(stringResource(R.string.onboarding_permission_title))
    Spacer(Modifier.height(14.dp))
    Text(
        text = stringResource(R.string.onboarding_permission_body),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Color.White.copy(alpha = 0.72f),
    )

    Spacer(Modifier.weight(1f))

    if (error != null) Text(error, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
    if (!permissionGranted) {
        PillButton(
            label = stringResource(R.string.onboarding_grant_permission),
            onClick = onRequestPermission,
            primary = true,
            enabled = !finishing,
        )
    } else {
        PillButton(
            label = stringResource(R.string.onboarding_start),
            onClick = onFinish,
            primary = true,
            enabled = !finishing,
        )
    }
    Spacer(Modifier.height(12.dp))
    PillButton(
        label = stringResource(R.string.onboarding_skip),
        onClick = onFinish,
        primary = false,
        enabled = !finishing,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1.2).sp,
        ),
        color = Color.White,
    )
}

@Composable
private fun PillButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, if (LocalReduceMotion.current) snap() else spring(stiffness = 900f), label = "pillScale")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, if (LocalReduceMotion.current) snap() else tween(180), label = "pillAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.W700,
            color = if (primary) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AurelisIcons.ChevronRight,
            contentDescription = stringResource(R.string.onboarding_back),
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = -1f },
        )
    }
}
