package dev.forcetower.unes.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// On-appear reveal modifiers — Compose equivalents of iOS
// `.fadeUpOnAppear(delay:)` / `.fadeInOnAppear(delay:)` / `.scaleInOnAppear()`.
//
// These animate via `graphicsLayer` (alpha + translation/scale only) instead
// of `AnimatedVisibility`, which would clip slide-in motion to the post-
// transition bounds and make the entrance look like content "materializing"
// rather than rising from below. graphicsLayer never affects layout, so the
// content keeps its final slot from frame zero and the motion is purely
// visual — exactly the iOS feel.
//
// PLACEMENT: put these modifiers BEFORE any `background`, `border`, `clip`
// in the chain. graphicsLayer only wraps the modifiers + content that come
// AFTER it, so trailing-position usage animates only the inner children
// while the container's background/border stay fixed. Layout modifiers
// (`offset`, `size`, `weight`) can sit on either side.
//
// SCROLL CONTAINERS: lazy items compose when scrolled into view, which would
// replay the entrance (and hide rows for their stagger delay) on every
// scroll. Screens hosted under a `RevealWindowHost` avoid this: the reveal
// only plays for content composed before the user's first scroll; anything
// composed after it appears in place instantly.

/**
 * Per-screen gate for the on-appear modifiers. Open while the screen is
 * playing its entrance; closed forever by the first user scroll, so content
 * composed by scrolling (in either direction) skips the reveal.
 */
class RevealWindow internal constructor() {
    var isOpen: Boolean = true
        private set

    fun close() {
        isOpen = false
    }
}

/** `null` (no host) means the reveal always plays — e.g. previews. */
val LocalRevealWindow = staticCompositionLocalOf<RevealWindow?> { null }

/**
 * Scopes a [RevealWindow] to [content] and closes it on the first user
 * scroll anywhere inside (observed via a non-consuming [NestedScrollConnection]).
 * Wrap each navigation destination once; the on-appear modifiers pick the
 * window up through [LocalRevealWindow].
 */
@Composable
fun RevealWindowHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val window = remember { RevealWindow() }
    val closeOnScroll = remember(window) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) window.close()
                return Offset.Zero
            }
        }
    }
    CompositionLocalProvider(LocalRevealWindow provides window) {
        Box(
            modifier = modifier.nestedScroll(closeOnScroll),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

/**
 * Decided once per call site, at first composition: reveal if there is no
 * window (previews, unhosted screens) or the window is still open.
 */
@Composable
private fun revealOnAppear(): Boolean {
    val window = LocalRevealWindow.current
    return remember { window?.isOpen != false }
}

/** Slide up from `fromOffset` while fading in. Defaults match iOS `fadeUpOnAppear`. */
fun Modifier.fadeUpOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 600,
    fromOffset: Dp = 12.dp,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier
    val alpha = remember { Animatable(0f) }
    val translation = remember { Animatable(fromOffset.value) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
            launch { translation.animateTo(0f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        // `translation.value` is a dp scalar (kept unitless on Animatable);
        // multiply by `density` to convert to pixels inside the layer.
        translationY = translation.value * density
    }
}

/** Fade in only — no translation. */
fun Modifier.fadeInOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 600,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing))
    }
    graphicsLayer { this.alpha = alpha.value }
}

/** Scale in from `fromScale` while fading in. Mirrors iOS `scaleInOnAppear`. */
fun Modifier.scaleInOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 500,
    fromScale: Float = 0.92f,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(fromScale) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
            launch { scale.animateTo(1f, tween(durationMs, easing = MelonMotion.PopEasing)) }
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        scaleX = scale.value
        scaleY = scale.value
    }
}
