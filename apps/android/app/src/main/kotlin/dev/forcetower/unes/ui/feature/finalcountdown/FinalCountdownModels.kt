package dev.forcetower.unes.ui.feature.finalcountdown

import java.util.UUID

// Mirrors iOS `FCRow` / `FCVerdict*` (apps/ios/UNES/Features/FinalCountdown/Models).
// One evaluation row in the calculator: `score` is null when the grade hasn't
// happened yet; `wildcard` marks the row the student can still influence
// (the one whose required mark the UI solves for).
internal data class FCRow(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val score: Double? = null,
    val weight: Int = 1,
    val wildcard: Boolean = false,
)

// The seven outcomes the calculator surfaces, plus `Empty` when there's not
// enough data to compute anything yet. Mirrors iOS `FCVerdictKind`.
internal enum class FCVerdictKind {
    Passed,
    OnTrack,
    Borderline,
    BorderlineFinal,
    Final,
    Impossible,
    Failed,
    FailingTrack,
    Empty,
}

internal data class FCVerdict(
    val kind: FCVerdictKind,
    val avg: Double?,
    val best: Double? = null,
    val worst: Double? = null,
    // Grade required on the wildcard row to close at 7 (partial-fill states).
    val wildcardNeeded: Double? = null,
    // Grade required on the Final to close at 5 (`Final` state).
    val need: Double? = null,
)

// Tone selection for the verdict hero / breakdown / rules. Mirrors iOS `FCTone`
// — colors are resolved at the call site through `MaterialTheme.melon.brand.*`
// or the discipline palette (no hex literals leak into feature code).
internal enum class FCTone { Plum, Magenta, Teal, Coral, Amber, Green }

internal data class FCVerdictCopy(
    val eyebrow: String,
    val titleLines: List<String>,
    val headline: String,
    val sub: String,
    val message: String,
    val tone: FCTone,
    val icon: FCVerdictIcon,
)

// Glyph slot for the verdict eyebrow chip. Resolved to a vector path in
// `FCVerdictHero` so this enum stays free of Compose imports.
internal enum class FCVerdictIcon { Trophy, Check, Bolt, Flag, Skull, Sparkle }
