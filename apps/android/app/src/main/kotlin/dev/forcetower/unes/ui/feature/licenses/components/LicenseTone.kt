package dev.forcetower.unes.ui.feature.licenses.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseFamily

// One accent color per license family — feeds the group icon tint, the row
// dot, the distribution-bar segment, the copyright-notice wash, and the
// repository link button. Matches the dc `LicensesScreen` palette: MIT rides
// the adaptive accent (`--mit`), Apache-2.0 the jade token (`--apache`); the
// remaining families (rare in our dependency set, but real) reuse the brand /
// palette tokens so every group still reads distinctly.
@Composable
@ReadOnlyComposable
internal fun LicenseFamily.toneBackground(): Color = when (this) {
    LicenseFamily.Mit -> MaterialTheme.colorScheme.primary
    LicenseFamily.Apache2 -> MaterialTheme.melon.palette.jade
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
