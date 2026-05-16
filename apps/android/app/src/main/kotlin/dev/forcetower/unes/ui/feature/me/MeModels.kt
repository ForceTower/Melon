package dev.forcetower.unes.ui.feature.me

import androidx.compose.ui.graphics.Color

// Local UI projection of the KMP `MeProfile` payload — everything the hero
// card, semester strip, and footer render comes from a single value of this
// type. Mirrors iOS `ProfileIdentity` (apps/ios/UNES/Features/Me/Models/MeModels.swift).
internal data class ProfileIdentity(
    val name: String,
    val firstName: String,
    val course: String,
    val campus: String,
    val enrollment: String,
    val username: String,
    val avatarInitial: String,
    val semester: String,
    val semesterWeek: Int,
    val semesterTotalWeeks: Int,
    val progressPct: Int,
    val cr: Double,
    val crDelta: String,
    val creditsDone: Int,
    val creditsRequired: Int,
    val semesterStart: String,
    val semesterEnd: String,
    val finalExam: String,
)

// Tone palette applied to a shortcut tile's icon badge. Mirrors the `TONES`
// map in `screens-me.jsx`; on Android these route through `MaterialTheme.melon`
// where possible (brand identity colors) and through the discipline palette
// for `teal`, which doesn't have a brand slot.
internal enum class ShortcutTone { Plum, Magenta, Teal, Coral, Amber }

// Identifier for a single tile in the constellation. Same set as iOS
// `Shortcut.Kind`; the navigation handler only acts on `Calendar`/`Countdown`
// today, the rest are placeholders the design surfaces but doesn't route yet.
internal enum class ShortcutKind {
    Account,
    Zhonya,
    Flowchart,
    Bandejao,
    Calendar,
    Countdown,
    Request,
    Theme,
    Reminders,
    Adventure,
}

// Renderable shortcut record. The icon/label/hint come from `MeShortcuts`,
// which keeps the catalogue in one place so all shortcut sets pick from the
// same library.
internal data class Shortcut(
    val id: ShortcutKind,
    val labelRes: Int,
    val hintRes: Int,
    val tone: ShortcutTone,
    val icon: ShortcutIcon,
)

// Stroke-based icon set kept inside the Me feature so we can match the JSX
// prototype pixel-for-pixel without leaning on Material's filled icon set
// (which carries different visual weights). `MeIcon` renders these.
internal enum class ShortcutIcon {
    Account, Hourglass, Flow, Tray, Calendar, Timer, Doc, Brush, Bell, Compass,
}

internal enum class SettingsRowKind { Settings, About, Feedback, Licenses }

internal data class SettingsRow(
    val id: SettingsRowKind,
    val labelRes: Int,
    val hintRes: Int,
    val icon: SettingsIcon,
)

internal enum class SettingsIcon { Gear, Info, Bug, License }

// Background + foreground for a tone. Routed through theme colors at the
// call site so the hex literals stay out of the feature code (per the design
// system rules in CLAUDE.md).
internal data class ResolvedTone(val background: Color, val foreground: Color)
