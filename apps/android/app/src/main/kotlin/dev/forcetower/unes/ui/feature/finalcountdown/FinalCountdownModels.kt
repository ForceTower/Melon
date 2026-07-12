package dev.forcetower.unes.ui.feature.finalcountdown

import java.util.UUID

// Mirrors iOS `FCRow` / `FCVerdict*` (UNESKit Features/FinalCountdown).
// One evaluation row in the calculator. `scoreText` is what the student typed
// (comma decimal); `score` is the parsed value, null while the grade hasn't
// happened yet — a blank row means "still to come".
internal data class FCRow(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val scoreText: String = "",
    val weight: Int = 1,
) {
    val score: Double?
        get() = scoreText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 10.0)

    companion object {
        // Keep digits plus a single comma, cap at 5 chars and value 10 — same
        // rules as iOS `FCRow.sanitizeScoreText`.
        fun sanitizeScoreText(raw: String): String {
            var sawComma = false
            val cleaned = buildString {
                for (ch in raw) {
                    when {
                        ch.isDigit() -> append(ch)
                        (ch == ',' || ch == '.') && !sawComma -> {
                            sawComma = true
                            append(',')
                        }
                    }
                }
            }.take(5)
            val value = cleaned.replace(',', '.').toDoubleOrNull() ?: return cleaned
            return if (value > 10.0) "10" else cleaned
        }

        // Seed text for a released grade — trailing zeros dropped, comma
        // decimal ("8,3", "7"). Blank for unreleased grades.
        fun text(value: Double?): String {
            if (value == null) return ""
            val asLong = value.toLong()
            val raw = if (value == asLong.toDouble()) {
                asLong.toString()
            } else {
                value.toString().trimEnd('0').trimEnd('.')
            }
            return raw.replace('.', ',')
        }
    }
}

// The eight outcomes the calculator surfaces, plus `Empty` when there's not
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
    // Grade required on the single missing row to close at 7 (partial states).
    val wildcardNeeded: Double? = null,
    // Grade required on the Prova Final to close at 5 (`Final` state).
    val need: Double? = null,
)

// Verdict → hero mesh family. Resolved to `MaterialTheme.melon.verdict.*`
// palettes at the call site (no hex literals in feature code).
internal enum class FCVerdictFamily { Passed, Track, Warn, Ember, Lost }

internal val FCVerdictKind.family: FCVerdictFamily
    get() = when (this) {
        FCVerdictKind.Passed -> FCVerdictFamily.Passed
        FCVerdictKind.OnTrack, FCVerdictKind.Empty -> FCVerdictFamily.Track
        FCVerdictKind.Borderline -> FCVerdictFamily.Warn
        FCVerdictKind.BorderlineFinal,
        FCVerdictKind.Final,
        FCVerdictKind.FailingTrack,
        -> FCVerdictFamily.Ember
        FCVerdictKind.Impossible, FCVerdictKind.Failed -> FCVerdictFamily.Lost
    }

// One pickable discipline in the "Trocar disciplina" sheet, plus the grades
// used to seed the calculator rows. `null` discipline in the UI state is
// "modo livre" — hypotheticals with no discipline attached.
internal data class FCDiscipline(
    val offerId: String,
    val code: String,
    val name: String,
    val teacher: String?,
    val semesterLabel: String,
    val seedGrades: List<FCSeedGrade>,
)

internal data class FCSeedGrade(
    val label: String,
    val value: Double?,
    val weight: Double?,
)
