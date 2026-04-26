package dev.forcetower.unes.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import dev.forcetower.unes.designsystem.theme.MelonMotion

// iOS-style horizontal navigation transitions, mirroring
// `UINavigationController` push/pop keyframes:
//   • duration 350ms, ease ≈ cubic-bezier(0.32, 0.72, 0, 1)
//   • underlying view tracks at one-third parallax
//   • no scale, no fade
//
// Exposed as extension functions on `AnimatedContentTransitionScope` so
// callers can compose them with their own logic (e.g. ConnectedScreen
// short-circuits these when the top-entry change is actually a tab swap).

private val SlideTween: FiniteAnimationSpec<IntOffset> = tween(
    durationMillis = MelonMotion.NavPushDurationMillis,
    easing = MelonMotion.NavPushEasing,
)

// Outgoing view drifts left at parallax speed while incoming slides in from
// the right edge. Denominator picked to match iOS's observed ~33% parallax.
private const val ParallaxDenominator = 3

internal fun AnimatedContentTransitionScope<Scene<NavKey>>.iosPush(): ContentTransform =
    slideInHorizontally(SlideTween) { width -> width } togetherWith
        slideOutHorizontally(SlideTween) { width -> -width / ParallaxDenominator }

internal fun AnimatedContentTransitionScope<Scene<NavKey>>.iosPop(): ContentTransform =
    slideInHorizontally(SlideTween) { width -> -width / ParallaxDenominator } togetherWith
        slideOutHorizontally(SlideTween) { width -> width }
