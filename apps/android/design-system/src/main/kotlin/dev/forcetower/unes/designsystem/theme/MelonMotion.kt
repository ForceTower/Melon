package dev.forcetower.unes.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// Mirrors iOS `UNESMotion`. Use these instead of declaring ad-hoc `tween()` /
// `spring()` values in feature code so motion stays consistent across screens.
object MelonMotion {
    fun <T> spring(): AnimationSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    fun <T> pop(): AnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)

    fun <T> ease(): AnimationSpec<T> =
        tween(durationMillis = 350, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f))

    fun <T> easeSlow(): AnimationSpec<T> =
        tween(durationMillis = 700, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f))

    fun <T> easeEmphasized(): AnimationSpec<T> =
        tween(durationMillis = 600, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))

    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val PopEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

    // iOS `UINavigationController` push/pop keyframes — fast acceleration,
    // gentle landing. Used by the nav-stack transitions in
    // `app/ui/navigation/NavTransitions.kt`.
    val NavPushEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val NavPushDurationMillis: Int = 350

    fun <T> navPush(): AnimationSpec<T> =
        tween(durationMillis = NavPushDurationMillis, easing = NavPushEasing)
}
