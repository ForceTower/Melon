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
    val fixed: MelonFixedColors,
)

// Tokens that don't flip with light/dark and aren't part of the brand identity
// — semantic colors used in fixed contexts (success indicators on dark cards,
// destructive actions, fixed light surfaces inside always-dark containers).
// Routed through `MaterialTheme.melon.fixed.*` so feature code never reaches
// for raw `Color(0x…)` literals (see CLAUDE.md design-system rules).
@Immutable
data class MelonFixedColors(
    // Always-light cream — same value as iOS `UNESColor.surfaceLight`. Use
    // for foregrounds inside always-dark containers (the IdentityCard hero,
    // pressed states on the dark splash buttons, etc.).
    val surfaceLight: Color,
    // Destructive accent for sign-out / wipe-data CTAs. Same hex iOS uses
    // (`MeColors.signOut`); kept fixed so it stays warning-red regardless of
    // theme.
    val destructive: Color,
    // "Online" / "ok" / "success" indicator dot. Lifted variant
    // (`successFg`) reads cleanly against `alwaysDarkBg`.
    val ok: Color,
    val okOnDark: Color,
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
    val rose: Color,
    val sky: Color,
    val emerald: Color,
    val indigo: Color,
    val mustard: Color,
)

private val MelonBrandDefaults = MelonBrandColors(
    plum = BrandPlum,
    magenta = BrandMagenta,
    coral = BrandCoral,
    amber = BrandAmber,
    peach = BrandPeach,
    alwaysDarkBg = AlwaysDarkBg,
)

private val MelonFixedDefaults = MelonFixedColors(
    surfaceLight = SurfaceLight,
    destructive = DestructiveFixed,
    ok = OkFixed,
    okOnDark = OkOnDarkFixed,
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
        rose = PaletteRoseLight,
        sky = PaletteSkyLight,
        emerald = PaletteEmeraldLight,
        indigo = PaletteIndigoLight,
        mustard = PaletteMustardLight,
    ),
    fixed = MelonFixedDefaults,
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
        rose = PaletteRoseDark,
        sky = PaletteSkyDark,
        emerald = PaletteEmeraldDark,
        indigo = PaletteIndigoDark,
        mustard = PaletteMustardDark,
    ),
    fixed = MelonFixedDefaults,
)

internal val LocalMelonColors = compositionLocalOf<MelonColors> {
    error("MelonColors not provided — wrap your content in MelonTheme { … }")
}

// `MaterialTheme.melon` — discoverable next to `colorScheme`, `typography`, `shapes`.
val MaterialTheme.melon: MelonColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMelonColors.current
