package dev.forcetower.unes.designsystem.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
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

/** Slide up from `fromOffset` while fading in. Defaults match iOS `fadeUpOnAppear`. */
fun Modifier.fadeUpOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 600,
    fromOffset: Dp = 12.dp,
): Modifier = composed {
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
