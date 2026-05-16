package dev.forcetower.unes.ui.feature.finalcountdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import dev.forcetower.unes.R

// Fixtures matching the iOS `FinalCountdownView` defaults + `ForcedMode`
// canned scenarios. The screen is intentionally fixture-driven on both
// platforms: the math + UI are validated against these snapshots rather than
// against live KMP grade data.

internal object FinalCountdownFixtures {
    val defaultRows: List<FCRow> = emptyList()

    val passed = listOf(
        FCRow(label = "AV1", score = 8.5),
        FCRow(label = "AV2", score = 7.8),
        FCRow(label = "Trab", score = 9.0, wildcard = true),
    )

    val final = listOf(
        FCRow(label = "AV1", score = 5.5),
        FCRow(label = "AV2", score = 4.0),
        FCRow(label = "Trab", score = 6.2, wildcard = true),
    )

    val failed = listOf(
        FCRow(label = "AV1", score = 1.5),
        FCRow(label = "AV2", score = 2.0),
        FCRow(label = "Trab", score = 2.8, wildcard = true),
    )

    val impossible = listOf(
        FCRow(label = "AV1", score = 2.0),
        FCRow(label = "AV2", score = 3.0),
        FCRow(label = "Trab", score = 3.5, wildcard = true),
    )

    val borderline = listOf(
        FCRow(label = "AV1", score = 6.5),
        FCRow(label = "AV2", score = 5.2),
        FCRow(label = "Trab", score = null, wildcard = true),
    )

    val onTrack = listOf(
        FCRow(label = "AV1", score = 8.0),
        FCRow(label = "AV2", score = 7.5),
        FCRow(label = "Trab", score = null, wildcard = true),
    )
}

// Verdict copy ported from iOS `FinalCountdownCopy.copy(for:)`. Reads strings
// from `strings.xml` so the texts stay translatable; values that the math
// computes (averages, "needed" grades) are formatted into the message via
// `final_countdown_*_format` placeholders.
@Composable
@ReadOnlyComposable
internal fun fcCopyFor(verdict: FCVerdict): FCVerdictCopy = when (verdict.kind) {
    FCVerdictKind.Passed -> FCVerdictCopy(
        eyebrow = stringResource(R.string.final_countdown_verdict_passed_eyebrow),
        titleLines = listOf(
            stringResource(R.string.final_countdown_verdict_passed_title_1),
            stringResource(R.string.final_countdown_verdict_passed_title_2),
            stringResource(R.string.final_countdown_verdict_passed_title_3),
        ),
        headline = FinalCountdownMath.formatGrade(verdict.avg),
        sub = stringResource(R.string.final_countdown_verdict_passed_sub),
        message = stringResource(
            R.string.final_countdown_verdict_passed_message_format,
            FinalCountdownMath.formatGrade(verdict.avg),
        ),
        tone = FCTone.Green,
        icon = FCVerdictIcon.Trophy,
    )

    FCVerdictKind.OnTrack -> {
        val needed = verdict.wildcardNeeded
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_ontrack_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_ontrack_title_1),
                stringResource(R.string.final_countdown_verdict_ontrack_title_2),
                "",
            ),
            headline = stringResource(R.string.final_countdown_verdict_ontrack_headline),
            sub = if (needed != null) {
                stringResource(
                    R.string.final_countdown_verdict_ontrack_sub_with_need_format,
                    FinalCountdownMath.formatGrade(needed),
                )
            } else {
                stringResource(R.string.final_countdown_verdict_ontrack_sub_no_need)
            },
            message = if (needed != null) {
                stringResource(
                    R.string.final_countdown_verdict_ontrack_message_with_need_format,
                    FinalCountdownMath.formatGrade(needed),
                )
            } else {
                stringResource(R.string.final_countdown_verdict_ontrack_message_no_need)
            },
            tone = FCTone.Teal,
            icon = FCVerdictIcon.Check,
        )
    }

    FCVerdictKind.Borderline -> {
        val need = FinalCountdownMath.formatGrade(verdict.wildcardNeeded)
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_borderline_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_borderline_title_1),
                stringResource(R.string.final_countdown_verdict_borderline_title_2),
                "",
            ),
            headline = need,
            sub = stringResource(R.string.final_countdown_verdict_borderline_sub),
            message = stringResource(R.string.final_countdown_verdict_borderline_message_format, need),
            tone = FCTone.Amber,
            icon = FCVerdictIcon.Bolt,
        )
    }

    FCVerdictKind.BorderlineFinal -> FCVerdictCopy(
        eyebrow = stringResource(R.string.final_countdown_verdict_borderline_final_eyebrow),
        titleLines = listOf(
            stringResource(R.string.final_countdown_verdict_borderline_final_title_1),
            stringResource(R.string.final_countdown_verdict_borderline_final_title_2),
            "",
        ),
        headline = stringResource(R.string.final_countdown_verdict_borderline_final_headline),
        sub = stringResource(R.string.final_countdown_verdict_borderline_final_sub),
        message = stringResource(R.string.final_countdown_verdict_borderline_final_message),
        tone = FCTone.Coral,
        icon = FCVerdictIcon.Flag,
    )

    FCVerdictKind.Final -> {
        val avg = FinalCountdownMath.formatGrade(verdict.avg)
        val need = FinalCountdownMath.formatGrade(verdict.need)
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_final_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_final_title_1),
                stringResource(R.string.final_countdown_verdict_final_title_2),
                "",
            ),
            headline = need,
            sub = stringResource(R.string.final_countdown_verdict_final_sub),
            message = stringResource(R.string.final_countdown_verdict_final_message_format, avg, need),
            tone = FCTone.Coral,
            icon = FCVerdictIcon.Flag,
        )
    }

    FCVerdictKind.Impossible -> {
        val avg = FinalCountdownMath.formatGrade(verdict.avg)
        val need = FinalCountdownMath.formatGrade(verdict.need)
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_impossible_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_impossible_title_1),
                stringResource(R.string.final_countdown_verdict_impossible_title_2),
                "",
            ),
            headline = stringResource(R.string.final_countdown_verdict_impossible_headline),
            sub = stringResource(R.string.final_countdown_verdict_impossible_sub),
            message = stringResource(R.string.final_countdown_verdict_impossible_message_format, avg, need),
            tone = FCTone.Plum,
            icon = FCVerdictIcon.Skull,
        )
    }

    FCVerdictKind.Failed -> {
        val avg = FinalCountdownMath.formatGrade(verdict.avg)
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_failed_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_failed_title_1),
                stringResource(R.string.final_countdown_verdict_failed_title_2),
                "",
            ),
            headline = avg,
            sub = stringResource(R.string.final_countdown_verdict_failed_sub),
            message = stringResource(R.string.final_countdown_verdict_failed_message),
            tone = FCTone.Plum,
            icon = FCVerdictIcon.Skull,
        )
    }

    FCVerdictKind.FailingTrack -> {
        val best = FinalCountdownMath.formatGrade(verdict.best)
        FCVerdictCopy(
            eyebrow = stringResource(R.string.final_countdown_verdict_failing_track_eyebrow),
            titleLines = listOf(
                stringResource(R.string.final_countdown_verdict_failing_track_title_1),
                stringResource(R.string.final_countdown_verdict_failing_track_title_2),
                "",
            ),
            headline = best,
            sub = stringResource(R.string.final_countdown_verdict_failing_track_sub),
            message = stringResource(R.string.final_countdown_verdict_failing_track_message_format, best),
            tone = FCTone.Coral,
            icon = FCVerdictIcon.Flag,
        )
    }

    FCVerdictKind.Empty -> FCVerdictCopy(
        eyebrow = stringResource(R.string.final_countdown_verdict_empty_eyebrow),
        titleLines = listOf(
            stringResource(R.string.final_countdown_verdict_empty_title_1),
            stringResource(R.string.final_countdown_verdict_empty_title_2),
            "",
        ),
        headline = stringResource(R.string.final_countdown_verdict_empty_headline),
        sub = stringResource(R.string.final_countdown_verdict_empty_sub),
        message = stringResource(R.string.final_countdown_verdict_empty_message),
        tone = FCTone.Plum,
        icon = FCVerdictIcon.Sparkle,
    )
}
