package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseFamily

// Maps a license family onto the existing brand/palette/fixed tokens. Same
// six-tone palette iOS draws on (`LicenseTone` in `LicensesView.swift`); each
// family is paired with a foreground that reads at >= 4.5:1 against its
// background, so the chip stays legible against either the cream or plum
// surfaces without retuning per appearance.
@Composable
@ReadOnlyComposable
internal fun LicenseFamily.toneBackground(): Color = when (this) {
    LicenseFamily.Mit -> MaterialTheme.melon.brand.amber
    LicenseFamily.Apache2 -> MaterialTheme.melon.palette.teal
    LicenseFamily.Bsd3,
    LicenseFamily.Bsd2,
    LicenseFamily.Other -> MaterialTheme.melon.brand.plum
    LicenseFamily.Isc,
    LicenseFamily.Cc0,
    LicenseFamily.Unlicense -> MaterialTheme.melon.fixed.ok
    LicenseFamily.Mpl2,
    LicenseFamily.Epl1 -> MaterialTheme.melon.brand.magenta
    LicenseFamily.CcBy4 -> MaterialTheme.melon.brand.coral
}

@Composable
@ReadOnlyComposable
internal fun LicenseFamily.toneForeground(): Color = when (this) {
    // Amber bg + dark plum text — the only inverted pair, since amber is the
    // brightest tone in the palette.
    LicenseFamily.Mit -> MaterialTheme.melon.brand.plum
    // Plum bg + warm peach text — lifts from the deep purple background.
    LicenseFamily.Bsd3,
    LicenseFamily.Bsd2,
    LicenseFamily.Other -> MaterialTheme.melon.brand.peach
    else -> MaterialTheme.melon.fixed.surfaceLight
}
