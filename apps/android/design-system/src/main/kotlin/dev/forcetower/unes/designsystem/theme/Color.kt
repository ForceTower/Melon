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
internal val LineLight = Color(0x14000000)
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
internal val LineDark = Color(0x17F5EFE6)
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
