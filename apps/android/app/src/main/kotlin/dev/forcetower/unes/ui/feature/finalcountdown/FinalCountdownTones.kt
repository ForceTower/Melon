package dev.forcetower.unes.ui.feature.finalcountdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon

// Resolve an `FCTone` to its background and on-background foreground via theme
// tokens — same recipe as iOS `FCTone.bg` / `.fg`. Brand identity colors
// route through `melon.brand.*`; teal lifts from the discipline palette since
// it has no brand slot; green uses the fixed `ok` token.
@Composable
@ReadOnlyComposable
internal fun fcToneBackground(tone: FCTone): Color {
    val brand = MaterialTheme.melon.brand
    val palette = MaterialTheme.melon.palette
    val fixed = MaterialTheme.melon.fixed
    return when (tone) {
        FCTone.Plum -> brand.plum
        FCTone.Magenta -> brand.magenta
        FCTone.Teal -> palette.teal
        FCTone.Coral -> brand.coral
        FCTone.Amber -> brand.amber
        FCTone.Green -> fixed.ok
    }
}

// Foreground color used on the tone-tinted chip square. Mirrors iOS:
// plum → peach, amber → plum, others → cream.
@Composable
@ReadOnlyComposable
internal fun fcToneForeground(tone: FCTone): Color {
    val brand = MaterialTheme.melon.brand
    val fixed = MaterialTheme.melon.fixed
    return when (tone) {
        FCTone.Plum -> brand.peach
        FCTone.Amber -> brand.plum
        else -> fixed.surfaceLight
    }
}

// Soft tint used as a rule-pill background. Opacity matches the iOS `soft`
// curve (amber 0.14, green 0.12, others 0.10).
@Composable
@ReadOnlyComposable
internal fun fcToneSoft(tone: FCTone): Color {
    val alpha = when (tone) {
        FCTone.Amber -> 0.14f
        FCTone.Green -> 0.12f
        else -> 0.10f
    }
    return fcToneBackground(tone).copy(alpha = alpha)
}
