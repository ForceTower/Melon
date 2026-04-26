package dev.forcetower.unes.ui.feature.me

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.forcetower.unes.designsystem.theme.melon

// Resolve a `ShortcutTone` to background + foreground via theme tokens.
// Mirrors the `TONES` lookup in `screens-me.jsx`. The brand colors are fixed
// across light/dark; teal uses the discipline palette since it has no brand
// slot, and the foreground for `Plum` reaches for `BrandPeach` to keep the
// hot accent fill against a deep cool background.
@Composable
@ReadOnlyComposable
internal fun resolveTone(tone: ShortcutTone): ResolvedTone {
    val brand = MaterialTheme.melon.brand
    val palette = MaterialTheme.melon.palette
    val onBrand = MaterialTheme.melon.fixed.surfaceLight
    return when (tone) {
        ShortcutTone.Plum -> ResolvedTone(brand.plum, brand.peach)
        ShortcutTone.Magenta -> ResolvedTone(brand.magenta, onBrand)
        ShortcutTone.Teal -> ResolvedTone(palette.teal, onBrand)
        ShortcutTone.Coral -> ResolvedTone(brand.coral, onBrand)
        ShortcutTone.Amber -> ResolvedTone(brand.amber, brand.plum)
    }
}
