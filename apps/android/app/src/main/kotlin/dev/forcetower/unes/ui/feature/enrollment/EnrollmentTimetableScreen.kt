package dev.forcetower.unes.ui.feature.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentAppBar
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentBanner
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentBannerTone
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentDock
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentTimetableGrid
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentTimetableLegend
import dev.forcetower.unes.ui.feature.enrollment.components.workloadSignal
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Weekly grid preview of the proposal (dc `MatriculaScreen` timetable view):
// summary banner, the custom Mon–Sat grid with conflict marks, the "a
// definir" note and the per-section legend chips.
@Composable
internal fun EnrollmentTimetableScreen(
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    EnrollmentTimetableContent(
        state = state,
        onBack = onBack,
        onOpenReview = onOpenReview,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun EnrollmentTimetableContent(
    state: EnrollmentUiState,
    onBack: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val picks = state.resolvedPicks
    val scheduled = picks.filter { it.section.hasSchedule }
    val pendingCount = picks.size - scheduled.size
    val conflicts = state.conflicts
    val window = state.window
    val palette = MaterialTheme.melon.palette

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 120.dp),
        ) {
            EnrollmentAppBar(
                title = stringResource(R.string.enrollment_timetable_title),
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 20)) {
                    Text(
                        text = stringResource(R.string.enrollment_timetable_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 30.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.9).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (picks.isEmpty()) {
                            stringResource(R.string.enrollment_timetable_sub_empty)
                        } else {
                            pluralStringResource(
                                R.plurals.enrollment_timetable_sub_count,
                                scheduled.size,
                                scheduled.size,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                when {
                    conflicts.isNotEmpty() -> EnrollmentBanner(
                        tone = EnrollmentBannerTone.Danger,
                        icon = Icons.Filled.Warning,
                        title = pluralStringResource(
                            R.plurals.enrollment_tt_conflicts_title,
                            conflicts.size,
                            conflicts.size,
                        ),
                        text = conflicts.map { conflict ->
                            stringResource(
                                R.string.enrollment_tt_conflict_item_format,
                                conflict.first.discipline.code,
                                conflict.first.section.label,
                                conflict.second.discipline.code,
                                conflict.second.section.label,
                                EnrollmentFormat.dayFull(conflict.day),
                            )
                        }.joinToString(" · "),
                        modifier = Modifier.fadeUpOnAppear(delayMs = 60),
                    )
                    picks.isEmpty() -> EnrollmentBanner(
                        tone = EnrollmentBannerTone.Neutral,
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.enrollment_tt_empty_title),
                        text = stringResource(R.string.enrollment_tt_empty_text),
                        modifier = Modifier.fadeUpOnAppear(delayMs = 60),
                    )
                    else -> EnrollmentBanner(
                        tone = EnrollmentBannerTone.Ok,
                        icon = Icons.Filled.Check,
                        title = stringResource(R.string.enrollment_tt_ok_title),
                        text = pluralStringResource(
                            R.plurals.enrollment_tt_ok_text,
                            scheduled.size,
                            scheduled.size,
                        ),
                        modifier = Modifier.fadeUpOnAppear(delayMs = 60),
                    )
                }

                EnrollmentTimetableGrid(
                    picks = scheduled,
                    hueFor = { code -> ColorFor.discipline(palette, code) },
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fadeUpOnAppear(delayMs = 120),
                )

                if (pendingCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 14.dp, start = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.enrollment_tt_tbd,
                                pendingCount,
                                pendingCount,
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (picks.isNotEmpty()) {
                    EnrollmentTimetableLegend(
                        picks = picks,
                        hueFor = { code -> ColorFor.discipline(palette, code) },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fadeUpOnAppear(delayMs = 200),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        if (window != null) {
            val signal = workloadSignal(state.totalHours, window)
            EnrollmentDock(
                totalHours = state.totalHours,
                maxHours = window.maxHours,
                hoursColor = signal.color,
                subText = if (conflicts.isNotEmpty()) {
                    pluralStringResource(R.plurals.enrollment_dock_conflicts, conflicts.size, conflicts.size)
                } else {
                    pluralStringResource(R.plurals.enrollment_dock_disciplines, picks.size, picks.size)
                },
                subColor = if (conflicts.isNotEmpty()) MaterialTheme.melon.status.bad else MaterialTheme.colorScheme.outline,
                primaryLabel = stringResource(R.string.enrollment_dock_review),
                primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onPrimary = onOpenReview,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
            )
        }
    }
}

@Preview
@Composable
private fun EnrollmentTimetablePreview() {
    MelonTheme {
        EnrollmentTimetableContent(
            state = EnrollmentFixtures.state,
            onBack = {},
            onOpenReview = {},
        )
    }
}
