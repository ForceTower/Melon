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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalInspectionMode
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
// SHADOWS: an alpha below 1f composites the subtree into an offscreen buffer
// sized to the layer, and anything the content draws outside those bounds is
// clipped — so a card that draws its own elevation shadow *inside* one of
// these modifiers keeps that shadow cut at its own edges for the whole
// reveal. Pass a [RevealShadow] instead of calling `Modifier.shadow` on the
// content: the shadow then rides on an outer layer that never fades (its
// elevation tracks the reveal progress instead), leaving only shadow-less
// content inside the alpha buffer.
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

/** `null` (no host) means the reveal always plays. */
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
 * window (unhosted screens) or the window is still open. A `@Preview` renders
 * a single frame and never advances the animation, so it skips straight to
 * the settled state instead of drawing frame zero — invisible content.
 */
@Composable
private fun revealOnAppear(): Boolean {
    if (LocalInspectionMode.current) return false
    val window = LocalRevealWindow.current
    return remember { window?.isOpen != false }
}

/**
 * The elevation shadow a revealed card would otherwise draw itself. Handed to
 * a reveal modifier so it can live outside the fading layer — see SHADOWS.
 */
data class RevealShadow(
    val elevation: Dp,
    val shape: Shape,
    val spotColor: Color = DefaultShadowColor,
    val ambientColor: Color = DefaultShadowColor,
)

/**
 * Shadow layer for a reveal: elevation *and* shadow opacity grow with
 * [progress] instead of sitting at full strength under still-invisible
 * content. Both matter — a shadow is drawn filled under its caster and only
 * hidden because the caster is opaque, so a still-transparent card shows the
 * whole shape through itself as a grey plate. The curve holds the shadow at
 * nothing for the transparent part of the fade and eases it in over the rest,
 * once the card is solid enough to hide its own fill. [geometry] carries the
 * reveal's translation/scale so the shadow travels with the card.
 */
private fun Modifier.revealShadowLayer(
    shadow: RevealShadow,
    progress: () -> Float,
    geometry: GraphicsLayerScope.() -> Unit,
): Modifier = graphicsLayer {
    geometry()
    val grown = ((progress() - ShadowHoldFraction) / (1f - ShadowHoldFraction))
        .coerceIn(0f, 1f)
        .let { it * it }
    shadowElevation = shadow.elevation.toPx() * grown
    shape = shadow.shape
    spotShadowColor = shadow.spotColor.fadedBy(grown)
    ambientShadowColor = shadow.ambientColor.fadedBy(grown)
}

/** Fraction of the reveal a shadow sits out before it starts growing. */
private const val ShadowHoldFraction = 0.4f

private fun Color.fadedBy(factor: Float): Color =
    if (factor >= 1f) this else copy(alpha = alpha * factor)

/** Static stand-in for the [shadow] a card gave up, when the reveal is skipped. */
private fun Modifier.staticShadowLayer(shadow: RevealShadow?): Modifier =
    if (shadow == null) this else revealShadowLayer(shadow, progress = { 1f }, geometry = {})

/** Slide up from `fromOffset` while fading in. Defaults match iOS `fadeUpOnAppear`. */
fun Modifier.fadeUpOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 600,
    fromOffset: Dp = 12.dp,
    shadow: RevealShadow? = null,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier.staticShadowLayer(shadow)
    val alpha = remember { Animatable(0f) }
    val translation = remember { Animatable(fromOffset.value) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
            launch { translation.animateTo(0f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
        }
    }
    // `translation.value` is a dp scalar (kept unitless on Animatable);
    // multiply by `density` to convert to pixels inside the layer.
    val slide: GraphicsLayerScope.() -> Unit = { translationY = translation.value * density }
    if (shadow == null) {
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            slide()
        }
    } else {
        Modifier
            .revealShadowLayer(shadow, progress = { alpha.value }, geometry = slide)
            .graphicsLayer { this.alpha = alpha.value }
    }
}

/** Fade in only — no translation. */
fun Modifier.fadeInOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 600,
    shadow: RevealShadow? = null,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier.staticShadowLayer(shadow)
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing))
    }
    if (shadow == null) {
        Modifier.graphicsLayer { this.alpha = alpha.value }
    } else {
        Modifier
            .revealShadowLayer(shadow, progress = { alpha.value }, geometry = {})
            .graphicsLayer { this.alpha = alpha.value }
    }
}

/** Scale in from `fromScale` while fading in. Mirrors iOS `scaleInOnAppear`. */
fun Modifier.scaleInOnAppear(
    delayMs: Int = 0,
    durationMs: Int = 500,
    fromScale: Float = 0.92f,
    shadow: RevealShadow? = null,
): Modifier = composed {
    if (!revealOnAppear()) return@composed Modifier.staticShadowLayer(shadow)
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(fromScale) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMs, easing = MelonMotion.EmphasizedEasing)) }
            launch { scale.animateTo(1f, tween(durationMs, easing = MelonMotion.PopEasing)) }
        }
    }
    val zoom: GraphicsLayerScope.() -> Unit = {
        scaleX = scale.value
        scaleY = scale.value
    }
    if (shadow == null) {
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            zoom()
        }
    } else {
        Modifier
            .revealShadowLayer(shadow, progress = { alpha.value }, geometry = zoom)
            .graphicsLayer { this.alpha = alpha.value }
    }
}
