package dev.forcetower.unes.ui.feature.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.settings.SettingsTone

internal data class ResolvedSettingsTone(val background: Color, val foreground: Color)

// Routes a SettingsTone through the theme — the background is a brand color
// (or the teal palette slot, which doesn't have a brand entry on iOS either)
// and the foreground picks the readable swatch on top. Mirrors iOS
// `SettingsTone.{background, foreground}`.
@Composable
@ReadOnlyComposable
internal fun resolveTone(tone: SettingsTone): ResolvedSettingsTone {
    val brand = MaterialTheme.melon.brand
    val palette = MaterialTheme.melon.palette
    val onLight = MaterialTheme.melon.fixed.surfaceLight
    return when (tone) {
        SettingsTone.Plum -> ResolvedSettingsTone(brand.plum, brand.peach)
        SettingsTone.Magenta -> ResolvedSettingsTone(brand.magenta, onLight)
        SettingsTone.Teal -> ResolvedSettingsTone(palette.teal, onLight)
        SettingsTone.Coral -> ResolvedSettingsTone(brand.coral, onLight)
        SettingsTone.Amber -> ResolvedSettingsTone(brand.amber, brand.plum)
    }
}
