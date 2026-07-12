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

// Onboarding dark steps (dc `UNES Onboarding - Android`). `night` is the warm
// near-black plate under the mesh blobs; `nightVeil` is the legibility scrim
// color layered on top at varying alphas. Fixed across themes — the splash,
// welcome and sync steps are always dark.
internal val NightFixed = Color(0xFF12100E)
internal val NightVeilFixed = Color(0xFF0A0810)

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
// Jade is the Mensagens "Disciplinas" category hue (dc `UNES Mensagens -
// Android` hue map) — greener than the discipline-rotation teal, also outside
// the rotation.
internal val PaletteJadeLight = Color(0xFF1F9E93)
internal val PaletteJadeDark = Color(0xFF3FC3B5)
// Orange is the onboarding intro "Notas" slide accent (dc `UNES Onboarding -
// Android`) — softer than brand coral so it reads on `PageBgLight`; outside
// the discipline rotation.
internal val PaletteOrangeLight = Color(0xFFE8894E)
internal val PaletteOrangeDark = Color(0xFFF2A26B)

// Verdict hero mesh palettes (`MelonVerdictColors`). The Final Countdown
// verdict card is an always-dark world: `night` is the base plate, `veil` the
// legibility scrim over the blobs, and each outcome family gets three mesh
// blob stops plus a `hue` used for the average ring, the stat value, and the
// eyebrow chip. Values come straight from the dc `FinalCountdownScreen`
// verdict map (`V[kind].mesh` / `V[kind].hue`); the plate/scrim pair shares
// the onboarding night tones.
internal val VerdictNight = NightFixed
internal val VerdictVeil = NightVeilFixed

internal val VerdictPassedBlobA = Color(0xFF2F9E5E)
internal val VerdictPassedBlobB = Color(0xFF4FD69C)
internal val VerdictPassedBlobC = Color(0xFF3B9EAE)
internal val VerdictPassedHue = Color(0xFF4FD69C)

internal val VerdictTrackBlobA = Color(0xFF2AA5B8)
internal val VerdictTrackBlobB = Color(0xFF3B9EAE)
internal val VerdictTrackBlobC = Color(0xFF5AD1E0)
internal val VerdictTrackHue = Color(0xFF5AD1E0)

internal val VerdictWarnBlobA = Color(0xFFF4A23C)
internal val VerdictWarnBlobB = Color(0xFFE8894E)
internal val VerdictWarnBlobC = Color(0xFFF6B03C)
internal val VerdictWarnHue = Color(0xFFF4B54C)

internal val VerdictEmberBlobA = Color(0xFFE85D4E)
internal val VerdictEmberBlobB = Color(0xFFF4A23C)
internal val VerdictEmberBlobC = Color(0xFFB23A7A)
internal val VerdictEmberHue = Color(0xFFF0805E)

internal val VerdictLostBlobA = Color(0xFFB23A7A)
internal val VerdictLostBlobB = Color(0xFF9B5AD0)
internal val VerdictLostBlobC = Color(0xFFE85D4E)
internal val VerdictLostHue = Color(0xFFC97BD6)

// Credential vault mesh (`MelonVaultColors`, dc `SettingsScreen` credenciais
// card). Like the verdict hero, the vault is an always-dark world: a deep
// green plate under three green/teal blob stops, with `veil` as the
// legibility scrim color. Fixed across themes.
internal val VaultNight = Color(0xFF14231E)
internal val VaultVeil = Color(0xFF081410)
internal val VaultBlobA = Color(0xFF1F7A5E)
internal val VaultBlobB = Color(0xFF2F9E6E)
internal val VaultBlobC = Color(0xFF1B5C6E)

// Lock-screen notification preview (dc `SettingsScreen` "Notas" section).
// The plate mocks a phone lock screen so it stays near-dark in both themes,
// but recedes further in dark mode; the inner card is a translucent
// notification bubble over it (white wash on light, lifted plum on dark).
internal val PreviewPlateLight = Color(0xFF2A2233)
internal val PreviewPlateDark = Color(0xFF181020)
internal val PreviewCardLight = Color(0x24FFFFFF)
internal val PreviewCardDark = Color(0xD93C3448)
