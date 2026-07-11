package dev.forcetower.unes.designsystem.theme

import androidx.compose.ui.graphics.Color

// Neutrals — light. Mirrors iOS `UNESColor.ink/ink2/.../surface/.../pageBg` light values.
internal val InkLight = Color(0xFF1A1420)
internal val Ink2Light = Color(0xFF3A2F42)
internal val Ink3Light = Color(0xFF6B5E70)
internal val Ink4Light = Color(0xFF9C8FA0)
internal val SurfaceLight = Color(0xFFFBF7F2)
internal val Surface2Light = Color(0xFFF3EDE4)
internal val Surface3Light = Color(0xFFE9E0D2)
internal val CardLight = Color(0xFFFFFFFF)
internal val PageBgLight = Color(0xFFEDE7DD)
internal val LineLight = Color(0x171A1420)
internal val CardLineLight = Color(0x0D000000)

// Neutrals — dark. Mirrors iOS dark values.
internal val InkDark = Color(0xFFF5EFE6)
internal val Ink2Dark = Color(0xFFD6CEC2)
internal val Ink3Dark = Color(0xFF9F9386)
internal val Ink4Dark = Color(0xFF6B6156)
internal val SurfaceDark = Color(0xFF15101A)
internal val Surface2Dark = Color(0xFF1E1824)
internal val Surface3Dark = Color(0xFF2A2232)
internal val CardDark = Color(0xFF1C1624)
internal val PageBgDark = Color(0xFF0C0810)
internal val LineDark = Color(0x1AF5EFE6)
internal val CardLineDark = Color(0x0FF5EFE6)

// Brand — identity-carrying, fixed across light/dark. Mirrors iOS brand colors.
// Exposed via `MaterialTheme.melon.brand.*`, not as top-level `val`s.
internal val BrandPlum = Color(0xFF2D1B4E)
internal val BrandMagenta = Color(0xFFB23A7A)
internal val BrandCoral = Color(0xFFE85D4E)
internal val BrandAmber = Color(0xFFF4A23C)
internal val BrandPeach = Color(0xFFFBD9A8)

// Always-dark surface used by splash/welcome/sync screens (iOS `darkBg`).
// Not adaptive — same in light and dark mode.
internal val AlwaysDarkBg = Color(0xFF1A0F28)

// Fixed semantic tokens (`MelonFixedColors`). These don't flip with light/dark
// — they're consumed by always-dark surfaces (the IdentityCard hero) or by
// destructive/success affordances that should stay the same color regardless
// of theme. Each mirrors a corresponding iOS `MeColors`/`UNESColor` constant.
internal val DestructiveFixed = Color(0xFFC94538)
internal val OkFixed = Color(0xFF4AA679)
internal val OkOnDarkFixed = Color(0xFF7FD4A2)

// Hero mesh card (Hoje redesign). The card is its own always-dark world:
// `heroNight` is the base plate under the mesh blobs, `heroVeil` is the color
// of the top/bottom legibility scrim (applied at varying alphas), `onHero` is
// the pure-white foreground the design specifies (deliberately not the cream
// `surfaceLight`). Same values in light and dark themes.
internal val HeroNightFixed = Color(0xFF201133)
internal val HeroVeilFixed = Color(0xFF180C28)
internal val OnHeroFixed = Color(0xFFFFFFFF)

// Live-session indicators on the hero card ("Agora" pulse dot + label,
// "Dia concluído" label). Tuned for the always-dark hero, fixed across themes.
internal val LiveFixed = Color(0xFF6BE29A)
internal val LiveTextFixed = Color(0xFFA8F0C4)

// Success green for completed-state affordances on light/dark cards (timeline
// check circles, frequência stat). Same hex as the "Verde" accent base.
internal val SuccessFixed = Color(0xFF2F9E5E)

// Adaptive semantic status trio (`MelonStatusColors`) — approval/warning/fail
// signals on grades, absences, and attention banners (Disciplinas redesign).
// Dark values are lifted so the same semantic reads on `SurfaceDark`; warn
// dark intentionally lands on the amber accent, matching the dc spec.
internal val StatusOkLight = Color(0xFF2F9E5E)
internal val StatusOkDark = Color(0xFF4AB878)
internal val StatusWarnLight = Color(0xFFC77F16)
internal val StatusWarnDark = BrandAmber
internal val StatusBadLight = Color(0xFFD64A3C)
internal val StatusBadDark = Color(0xFFF26D5B)

// Accent — adaptive: coral in light, amber in dark (matches iOS `UNESColor.accent`).
internal val AccentLight = BrandCoral
internal val AccentPressLight = Color(0xFFC94538)
internal val AccentDark = BrandAmber
internal val AccentPressDark = Color(0xFFE88A1D)

// Discipline palette — adaptive accents used to tint discipline cards, "AGORA"
// chips, schedule blocks, and timeline dots. Light values match the brand
// hexes; dark values are lifted so the same semantic ("CALC is teal") still
// reads on `SurfaceDark`. Mirrors iOS `ColorFor.{coral, amber, magenta, teal,
// plum}` from `OverviewViewModel.swift`.
internal val PaletteCoralLight = BrandCoral
internal val PaletteCoralDark = Color(0xFFF27E6E)
internal val PaletteAmberLight = BrandAmber
internal val PaletteAmberDark = BrandAmber
internal val PaletteMagentaLight = BrandMagenta
internal val PaletteMagentaDark = Color(0xFFD46299)
internal val PaletteTealLight = Color(0xFF3B9EAE)
internal val PaletteTealDark = Color(0xFF5BB8C6)
internal val PalettePlumLight = BrandPlum
internal val PalettePlumDark = Color(0xFFB39DDB)
internal val PaletteRoseLight = Color(0xFFC64A6D)
internal val PaletteRoseDark = Color(0xFFE88AA5)
internal val PaletteSkyLight = Color(0xFF3C7DC9)
internal val PaletteSkyDark = Color(0xFF79AEE8)
internal val PaletteEmeraldLight = Color(0xFF2E8B5C)
internal val PaletteEmeraldDark = Color(0xFF5FC48E)
internal val PaletteIndigoLight = Color(0xFF4A5FB8)
internal val PaletteIndigoDark = Color(0xFF8A9EE8)
internal val PaletteMustardLight = Color(0xFFA0741F)
internal val PaletteMustardDark = Color(0xFFD4A84C)
// Violet + green joined the palette for the Eu shortcut grid (dc `UNES Eu -
// Android` hue map); they are not part of the 10-hue discipline rotation in
// `ColorFor`.
internal val PaletteVioletLight = Color(0xFF7C4DD6)
internal val PaletteVioletDark = Color(0xFFB08CF0)
internal val PaletteGreenLight = Color(0xFF2F9E5E)
internal val PaletteGreenDark = Color(0xFF4AB878)

// Verdict hero gradient pairs (`MelonVerdictColors`). Hand-tuned dark stops
// for each verdict outcome family in the Final Countdown screen — passed
// reads as a deep emerald wash, failed/impossible as plum, final as a brick
// red, borderline as warm amber-brown, and ontrack/empty as a teal slate.
// Mirrors iOS `backgroundGradient` (FCVerdictHero.swift). These are dark in
// every theme because the hero card is its own world.
internal val VerdictPassedTop = Color(0xFF1A3A28)
internal val VerdictPassedBottom = Color(0xFF0F2418)
internal val VerdictFailedTop = Color(0xFF2A1624)
internal val VerdictFailedBottom = Color(0xFF180D1A)
internal val VerdictFinalTop = Color(0xFF3A1E1A)
internal val VerdictFinalBottom = Color(0xFF201110)
internal val VerdictBorderlineTop = Color(0xFF3A2A12)
internal val VerdictBorderlineBottom = Color(0xFF201608)
internal val VerdictNeutralTop = Color(0xFF1A2A2F)
internal val VerdictNeutralBottom = Color(0xFF0E1618)
