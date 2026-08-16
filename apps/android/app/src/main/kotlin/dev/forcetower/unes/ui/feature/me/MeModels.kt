package dev.forcetower.unes.ui.feature.me

import androidx.compose.ui.graphics.vector.ImageVector

// Local UI projection of the KMP `MeProfile` payload — everything the hero
// card and the semester progress card render comes from a single value of
// this type. Mirrors the dc `EuScreen` identity card + progress card fields.
internal data class ProfileIdentity(
    // Display name — the profile-customization alternate name when set, the
    // registry name otherwise.
    val name: String,
    val firstName: String,
    // Upstream registry name. University-issued surfaces (documents) render
    // this one regardless of customization; the edit sheet shows it in the
    // "vale só dentro do UNES" note.
    val officialName: String,
    // Public URL of the custom profile picture; null renders the gradient
    // initial avatar.
    val avatarUrl: String?,
    val course: String,
    // "UEFS · Módulo 5" — modal campus/módulo of the semester's allocations.
    // Null while no space data is synced; the card falls back to a static
    // university label.
    val campusLabel: String?,
    val enrollment: String,
    val username: String,
    val avatarInitial: String,
    val semesterWeek: Int,
    val semesterTotalWeeks: Int,
    val progressPct: Int,
    // Lifetime CR ("Score"). Null until the score flow emits — rendered "–".
    val cr: Double?,
    // Signed movement caused by the last closed semester; the delta row
    // renders only when |delta| ≥ 0.1 (same rule as iOS).
    val crDelta: Double?,
    val attendancePercent: Int?,
    // 1-based enrolled-semester ordinal ("6º").
    val semesterOrdinal: Int?,
    // Short pre-formatted dates ("18 fev") for the progress card footer.
    val semesterStart: String,
    val semesterEnd: String,
)

// Hue slot applied to a shortcut/settings tonal treatment. Values route
// through `MaterialTheme.melon.palette` in `MeTones.kt`; the mapping mirrors
// the dc `EuScreen` hue map.
internal enum class ShortcutTone { Teal, Coral, Magenta, Indigo, Violet, Amber, Green, Mustard }

// Same set as iOS `MeShortcut` (MeFeature.swift). Enrollment, CourseProgress,
// Certificate, History, Paradoxo, Library and Materials are remote-config
// gated; Calendar and Countdown always show.
internal enum class ShortcutKind {
    Enrollment,
    Calendar,
    Countdown,
    CourseProgress,
    Certificate,
    History,
    Paradoxo,
    Library,
    Materials,
}

internal data class Shortcut(
    val id: ShortcutKind,
    val labelRes: Int,
    // Null renders a label-only tile.
    val hintRes: Int? = null,
    val tone: ShortcutTone,
    val icon: ImageVector,
    val beta: Boolean = false,
)

internal enum class SettingsRowKind { Settings, About, Feedback, Licenses }

internal data class SettingsRow(
    val id: SettingsRowKind,
    val labelRes: Int,
    val hintRes: Int,
    val icon: ImageVector,
    // Null renders the neutral (surface-toned) icon container.
    val tone: ShortcutTone?,
)
