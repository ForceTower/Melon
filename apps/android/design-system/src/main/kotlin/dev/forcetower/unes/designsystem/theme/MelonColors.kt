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
    val verdict: MelonVerdictColors,
    val vault: MelonVaultColors,
    val status: MelonStatusColors,
)

// Adaptive semantic status trio — pass/attention/fail signals on grades,
// absence bars, and warning banners. Unlike `fixed.success`/`fixed.destructive`
// these flip with the theme so they stay legible on dark surfaces.
@Immutable
data class MelonStatusColors(
    val ok: Color,
    val warn: Color,
    val bad: Color,
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
    // Ink foreground paired with `surfaceLight` — fixed dark regardless of
    // theme (light-pill CTAs sitting on the always-dark onboarding steps).
    val onSurfaceLight: Color,
    // Destructive accent for sign-out / wipe-data CTAs. Same hex iOS uses
    // (`MeColors.signOut`); kept fixed so it stays warning-red regardless of
    // theme.
    val destructive: Color,
    // "Online" / "ok" / "success" indicator dot. Lifted variant
    // (`successFg`) reads cleanly against `alwaysDarkBg`.
    val ok: Color,
    val okOnDark: Color,
    // Hero mesh card plate + scrim + foreground (Hoje redesign). The hero is
    // always dark regardless of theme; `onHero` is pure white by spec.
    val heroNight: Color,
    val heroVeil: Color,
    val onHero: Color,
    // Live-session indicators on the hero ("Agora" dot + label).
    val live: Color,
    val liveText: Color,
    // Onboarding dark steps (splash/welcome/sync): warm near-black plate under
    // the mesh + the scrim color layered over it at varying alphas.
    val night: Color,
    val nightVeil: Color,
    // Success green for done-state affordances (timeline checks). Matches the
    // "Verde" accent base so success reads consistently in both themes.
    val success: Color,
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
    // Lock-screen notification preview (dc `SettingsScreen`): `previewPlate`
    // is the mock lock-screen backdrop, `previewCard` the translucent
    // notification bubble on top. Adaptive, but dark-leaning in both themes.
    val previewPlate: Color,
    val previewCard: Color,
)

// Mesh palettes for the Final Countdown verdict hero (dc `FinalCountdownScreen`
// verdict map). Always painted dark regardless of system theme — the hero card
// is its own world. `night` is the card plate, `veil` the legibility scrim;
// each outcome family carries three mesh blob stops plus the `hue` used for
// the average ring / stat value / eyebrow chip.
@Immutable
data class MelonVerdictPalette(
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val hue: Color,
) {
    val blobs: List<Color> get() = listOf(blobA, blobB, blobC)
}

@Immutable
data class MelonVerdictColors(
    val night: Color,
    val veil: Color,
    // Aprovação direta (green).
    val passed: MelonVerdictPalette,
    // No caminho / comece a preencher (teal).
    val track: MelonVerdictPalette,
    // Dá pra passar (amber).
    val warn: MelonVerdictPalette,
    // Rumo à Prova Final / caminho difícil (coral-ember).
    val ember: MelonVerdictPalette,
    // Matemática perdida / reprovada (berry).
    val lost: MelonVerdictPalette,
)

// Credential vault mesh (dc `SettingsScreen` credenciais card). Always-dark
// like the verdict hero: `night` is the card plate, `veil` the legibility
// scrim color, and the three blob stops feed `Mesh(colors = …)`.
@Immutable
data class MelonVaultColors(
    val night: Color,
    val veil: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
) {
    val blobs: List<Color> get() = listOf(blobA, blobB, blobC)
}

// Discipline tinting palette. Adaptive: same semantic slot in light/dark, but
// dark values are lifted so chips/dots stay legible on `SurfaceDark`. The
// first ten slots mirror iOS `ColorFor` (Overview/OverviewViewModel.swift) —
// keep the two in sync. `violet` and `green` are Android additions for the Eu
// shortcut grid, and `jade` for the Mensagens category tinting; all three sit
// outside the discipline rotation.
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
    val violet: Color,
    val green: Color,
    val jade: Color,
    val orange: Color,
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
    onSurfaceLight = InkLight,
    destructive = DestructiveFixed,
    ok = OkFixed,
    okOnDark = OkOnDarkFixed,
    heroNight = HeroNightFixed,
    heroVeil = HeroVeilFixed,
    onHero = OnHeroFixed,
    live = LiveFixed,
    liveText = LiveTextFixed,
    night = NightFixed,
    nightVeil = NightVeilFixed,
    success = SuccessFixed,
)

private val MelonVaultDefaults = MelonVaultColors(
    night = VaultNight,
    veil = VaultVeil,
    blobA = VaultBlobA,
    blobB = VaultBlobB,
    blobC = VaultBlobC,
)

private val MelonVerdictDefaults = MelonVerdictColors(
    night = VerdictNight,
    veil = VerdictVeil,
    passed = MelonVerdictPalette(
        blobA = VerdictPassedBlobA,
        blobB = VerdictPassedBlobB,
        blobC = VerdictPassedBlobC,
        hue = VerdictPassedHue,
    ),
    track = MelonVerdictPalette(
        blobA = VerdictTrackBlobA,
        blobB = VerdictTrackBlobB,
        blobC = VerdictTrackBlobC,
        hue = VerdictTrackHue,
    ),
    warn = MelonVerdictPalette(
        blobA = VerdictWarnBlobA,
        blobB = VerdictWarnBlobB,
        blobC = VerdictWarnBlobC,
        hue = VerdictWarnHue,
    ),
    ember = MelonVerdictPalette(
        blobA = VerdictEmberBlobA,
        blobB = VerdictEmberBlobB,
        blobC = VerdictEmberBlobC,
        hue = VerdictEmberHue,
    ),
    lost = MelonVerdictPalette(
        blobA = VerdictLostBlobA,
        blobB = VerdictLostBlobB,
        blobC = VerdictLostBlobC,
        hue = VerdictLostHue,
    ),
)

internal fun melonColorsLight() = MelonColors(
    brand = MelonBrandDefaults,
    surface = MelonSurfaceColors(
        card = CardLight,
        cardLine = CardLineLight,
        line = LineLight,
        pressedAccent = AccentPressLight,
        previewPlate = PreviewPlateLight,
        previewCard = PreviewCardLight,
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
        violet = PaletteVioletLight,
        green = PaletteGreenLight,
        jade = PaletteJadeLight,
        orange = PaletteOrangeLight,
    ),
    fixed = MelonFixedDefaults,
    verdict = MelonVerdictDefaults,
    vault = MelonVaultDefaults,
    status = MelonStatusColors(
        ok = StatusOkLight,
        warn = StatusWarnLight,
        bad = StatusBadLight,
    ),
)

internal fun melonColorsDark() = MelonColors(
    brand = MelonBrandDefaults,
    surface = MelonSurfaceColors(
        card = CardDark,
        cardLine = CardLineDark,
        line = LineDark,
        pressedAccent = AccentPressDark,
        previewPlate = PreviewPlateDark,
        previewCard = PreviewCardDark,
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
        violet = PaletteVioletDark,
        green = PaletteGreenDark,
        jade = PaletteJadeDark,
        orange = PaletteOrangeDark,
    ),
    fixed = MelonFixedDefaults,
    verdict = MelonVerdictDefaults,
    vault = MelonVaultDefaults,
    status = MelonStatusColors(
        ok = StatusOkDark,
        warn = StatusWarnDark,
        bad = StatusBadDark,
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
