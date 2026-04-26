package dev.forcetower.unes.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Melon-specific palette that lives alongside Material 3's ColorScheme.
//
// `brand` carries identity colors that don't flip with light/dark — they're
// theme-routed (not top-level `val`s) so all color access goes through one
// CompositionLocal entry point.
// `surface` carries iOS tokens that *are* adaptive but don't have a Material 3
// ColorScheme slot (card, hairlines, pressed-accent).
//
// Material slots (background, primary, surface, onSurface, surfaceContainer*,
// outline, …) live on `MaterialTheme.colorScheme` — use those for any standard
// Compose component so the iOS-derived neutrals come through automatically.
@Immutable
data class MelonColors(
    val brand: MelonBrandColors,
    val surface: MelonSurfaceColors,
    val palette: MelonPaletteColors,
)

@Immutable
data class MelonBrandColors(
    val plum: Color,
    val magenta: Color,
    val coral: Color,
    val amber: Color,
    val peach: Color,
    // Always-dark — same value in light and dark mode (splash/welcome/sync).
    val alwaysDarkBg: Color,
)

@Immutable
data class MelonSurfaceColors(
    val card: Color,
    val cardLine: Color,
    val line: Color,
    val pressedAccent: Color,
)

// Discipline tinting palette. Adaptive: same semantic slot in light/dark, but
// dark values are lifted so chips/dots stay legible on `SurfaceDark`. Mirrors
// iOS `ColorFor` (Overview/OverviewViewModel.swift) — keep the two in sync.
@Immutable
data class MelonPaletteColors(
    val coral: Color,
    val amber: Color,
    val magenta: Color,
    val teal: Color,
    val plum: Color,
)

private val MelonBrandDefaults = MelonBrandColors(
    plum = BrandPlum,
    magenta = BrandMagenta,
    coral = BrandCoral,
    amber = BrandAmber,
    peach = BrandPeach,
    alwaysDarkBg = AlwaysDarkBg,
)

internal fun melonColorsLight() = MelonColors(
    brand = MelonBrandDefaults,
    surface = MelonSurfaceColors(
        card = CardLight,
        cardLine = CardLineLight,
        line = LineLight,
        pressedAccent = AccentPressLight,
    ),
    palette = MelonPaletteColors(
        coral = PaletteCoralLight,
        amber = PaletteAmberLight,
        magenta = PaletteMagentaLight,
        teal = PaletteTealLight,
        plum = PalettePlumLight,
    ),
)

internal fun melonColorsDark() = MelonColors(
    brand = MelonBrandDefaults,
    surface = MelonSurfaceColors(
        card = CardDark,
        cardLine = CardLineDark,
        line = LineDark,
        pressedAccent = AccentPressDark,
    ),
    palette = MelonPaletteColors(
        coral = PaletteCoralDark,
        amber = PaletteAmberDark,
        magenta = PaletteMagentaDark,
        teal = PaletteTealDark,
        plum = PalettePlumDark,
    ),
)

internal val LocalMelonColors = compositionLocalOf<MelonColors> {
    error("MelonColors not provided — wrap your content in MelonTheme { … }")
}

// `MaterialTheme.melon` — discoverable next to `colorScheme`, `typography`, `shapes`.
val MaterialTheme.melon: MelonColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMelonColors.current
