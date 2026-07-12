package dev.forcetower.unes.ui.feature.finalcountdown

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.forcetower.unes.R

// Everything the verdict hero renders for one outcome. Copy ported from iOS
// `FCVerdict.style(nextSemesterLabel:)`; layout slots mirror the dc verdict
// map (eyebrow chip · title · lead · stat label/value · detail · final scale).
internal data class FCVerdictStyle(
    val eyebrow: String,
    val title: String,
    val lead: String,
    val statLabel: String,
    val statValue: String,
    val detail: String,
    val icon: ImageVector,
    val family: FCVerdictFamily,
    val showScale: Boolean = false,
)

@Composable
@ReadOnlyComposable
internal fun fcVerdictStyle(verdict: FCVerdict, nextSemesterLabel: String?): FCVerdictStyle {
    val family = verdict.kind.family
    return when (verdict.kind) {
        FCVerdictKind.Passed -> FCVerdictStyle(
            eyebrow = stringResource(R.string.final_countdown_verdict_passed_eyebrow),
            title = stringResource(R.string.final_countdown_verdict_passed_title),
            lead = stringResource(R.string.final_countdown_verdict_passed_lead),
            statLabel = stringResource(R.string.final_countdown_verdict_passed_stat_label),
            statValue = FinalCountdownMath.formatGrade(verdict.avg),
            detail = stringResource(
                R.string.final_countdown_verdict_passed_detail_format,
                FinalCountdownMath.formatGrade(verdict.avg),
            ),
            icon = Icons.Filled.EmojiEvents,
            family = family,
        )

        FCVerdictKind.OnTrack -> {
            val needed = verdict.wildcardNeeded
            FCVerdictStyle(
                eyebrow = stringResource(R.string.final_countdown_verdict_ontrack_eyebrow),
                title = stringResource(R.string.final_countdown_verdict_ontrack_title),
                lead = stringResource(
                    if (needed != null) R.string.final_countdown_verdict_ontrack_lead_need
                    else R.string.final_countdown_verdict_ontrack_lead_no_need,
                ),
                statLabel = stringResource(
                    if (needed != null) R.string.final_countdown_stat_needed_next
                    else R.string.final_countdown_verdict_ontrack_stat_label_projection,
                ),
                statValue = if (needed != null) {
                    stringResource(
                        R.string.final_countdown_verdict_ontrack_stat_value_format,
                        FinalCountdownMath.formatGrade(needed),
                    )
                } else {
                    stringResource(R.string.final_countdown_verdict_ontrack_stat_value_ok)
                },
                detail = if (needed != null) {
                    stringResource(
                        R.string.final_countdown_verdict_ontrack_detail_need_format,
                        FinalCountdownMath.formatGrade(needed),
                    )
                } else {
                    stringResource(R.string.final_countdown_verdict_ontrack_detail_no_need)
                },
                icon = Icons.Filled.Check,
                family = family,
            )
        }

        FCVerdictKind.Borderline -> {
            val need = FinalCountdownMath.formatGrade(verdict.wildcardNeeded)
            FCVerdictStyle(
                eyebrow = stringResource(R.string.final_countdown_verdict_borderline_eyebrow),
                title = stringResource(R.string.final_countdown_verdict_borderline_title),
                lead = stringResource(R.string.final_countdown_verdict_borderline_lead),
                statLabel = stringResource(R.string.final_countdown_stat_needed_next),
                statValue = need,
                detail = stringResource(
                    R.string.final_countdown_verdict_borderline_detail_format,
                    need,
                ),
                icon = Icons.Filled.Bolt,
                family = family,
            )
        }

        FCVerdictKind.BorderlineFinal -> FCVerdictStyle(
            eyebrow = stringResource(R.string.final_countdown_verdict_borderline_final_eyebrow),
            title = stringResource(R.string.final_countdown_verdict_borderline_final_title),
            lead = stringResource(R.string.final_countdown_verdict_borderline_final_lead),
            statLabel = stringResource(R.string.final_countdown_verdict_borderline_final_stat_label),
            statValue = stringResource(R.string.final_countdown_verdict_borderline_final_stat_value),
            detail = stringResource(R.string.final_countdown_verdict_borderline_final_detail),
            icon = Icons.Filled.Flag,
            family = family,
        )

        FCVerdictKind.Final -> {
            val avg = FinalCountdownMath.formatGrade(verdict.avg)
            val need = FinalCountdownMath.formatGrade(verdict.need)
            FCVerdictStyle(
                eyebrow = stringResource(R.string.final_countdown_verdict_final_eyebrow),
                title = stringResource(R.string.final_countdown_verdict_final_title),
                lead = stringResource(R.string.final_countdown_verdict_final_lead),
                statLabel = stringResource(R.string.final_countdown_verdict_final_stat_label),
                statValue = need,
                detail = stringResource(
                    R.string.final_countdown_verdict_final_detail_format,
                    avg,
                    need,
                ),
                icon = Icons.Filled.Flag,
                family = family,
                showScale = true,
            )
        }

        FCVerdictKind.Impossible -> {
            val avg = FinalCountdownMath.formatGrade(verdict.avg)
            val need = FinalCountdownMath.formatGrade(verdict.need)
            FCVerdictStyle(
                eyebrow = stringResource(R.string.final_countdown_verdict_impossible_eyebrow),
                title = stringResource(R.string.final_countdown_verdict_impossible_title),
                lead = stringResource(R.string.final_countdown_verdict_impossible_lead),
                statLabel = stringResource(R.string.final_countdown_verdict_impossible_stat_label),
                statValue = need,
                detail = stringResource(
                    R.string.final_countdown_verdict_impossible_detail_format,
                    avg,
                    need,
                ),
                icon = Icons.Filled.SentimentVeryDissatisfied,
                family = family,
            )
        }

        FCVerdictKind.Failed -> FCVerdictStyle(
            eyebrow = stringResource(R.string.final_countdown_verdict_failed_eyebrow),
            title = stringResource(R.string.final_countdown_verdict_failed_title),
            lead = if (nextSemesterLabel != null) {
                stringResource(
                    R.string.final_countdown_verdict_failed_lead_semester_format,
                    nextSemesterLabel,
                )
            } else {
                stringResource(R.string.final_countdown_verdict_failed_lead_fallback)
            },
            statLabel = stringResource(R.string.final_countdown_verdict_failed_stat_label),
            statValue = FinalCountdownMath.formatGrade(verdict.avg),
            detail = stringResource(R.string.final_countdown_verdict_failed_detail),
            icon = Icons.Filled.SentimentVeryDissatisfied,
            family = family,
        )

        FCVerdictKind.FailingTrack -> {
            val best = FinalCountdownMath.formatGrade(verdict.best)
            FCVerdictStyle(
                eyebrow = stringResource(R.string.final_countdown_verdict_failing_track_eyebrow),
                title = stringResource(R.string.final_countdown_verdict_failing_track_title),
                lead = stringResource(R.string.final_countdown_verdict_failing_track_lead),
                statLabel = stringResource(R.string.final_countdown_verdict_failing_track_stat_label),
                statValue = best,
                detail = stringResource(
                    R.string.final_countdown_verdict_failing_track_detail_format,
                    best,
                ),
                icon = Icons.Filled.Flag,
                family = family,
            )
        }

        FCVerdictKind.Empty -> FCVerdictStyle(
            eyebrow = stringResource(R.string.final_countdown_verdict_empty_eyebrow),
            title = stringResource(R.string.final_countdown_verdict_empty_title),
            lead = stringResource(R.string.final_countdown_verdict_empty_lead),
            statLabel = stringResource(R.string.final_countdown_verdict_empty_stat_label),
            statValue = FinalCountdownMath.formatGrade(null),
            detail = stringResource(R.string.final_countdown_verdict_empty_detail),
            icon = Icons.Filled.AutoAwesome,
            family = family,
        )
    }
}
