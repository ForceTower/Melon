package dev.forcetower.unes.ui.feature.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentCodeChip
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentDock
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentTagPill
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentWorkloadCard
import dev.forcetower.unes.ui.feature.enrollment.components.workloadSignal
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Revisar step (dc `MatriculaScreen` review view): workload meter, blocker
// banners, the selected-section cards with remove + "aceitar outra turma"
// toggles, and the submit dock. When the window is closed the same screen is
// the read-only comprovante — no dock, no mutations.
@Composable
internal fun EnrollmentReviewScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            if (effect is EnrollmentEffect.Submitted) onSubmitted()
        }
    }
    EnrollmentReviewContent(
        state = state,
        onBack = onBack,
        onRemove = { vm.onIntent(EnrollmentIntent.RemovePick(it)) },
        onAllowsOtherChanged = { id, value -> vm.onIntent(EnrollmentIntent.AllowsOtherChanged(id, value)) },
        onSubmit = { vm.onIntent(EnrollmentIntent.Submit) },
        onDismissError = { vm.onIntent(EnrollmentIntent.DismissSubmitError) },
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun EnrollmentReviewContent(
    state: EnrollmentUiState,
    onBack: () -> Unit,
    onRemove: (Long) -> Unit,
    onAllowsOtherChanged: (Long, Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val window = state.window
    val picks = state.resolvedPicks
    val readonly = state.isReadonly

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
                .padding(bottom = bottomInset + if (readonly) 24.dp else 120.dp),
        ) {
            EnrollmentAppBar(
                title = stringResource(R.string.enrollment_review_appbar),
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 20)) {
                    Text(
                        text = stringResource(R.string.enrollment_review_title),
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
                            stringResource(R.string.enrollment_review_sub_empty)
                        } else {
                            pluralStringResource(
                                R.plurals.enrollment_review_sub,
                                picks.size,
                                picks.size,
                                state.totalHours,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (readonly) {
                    EnrollmentBanner(
                        tone = EnrollmentBannerTone.Ok,
                        icon = Icons.Filled.Check,
                        title = stringResource(R.string.enrollment_readonly_title),
                        text = stringResource(R.string.enrollment_readonly_text),
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .fadeUpOnAppear(delayMs = 40),
                    )
                }

                if (window != null) {
                    EnrollmentWorkloadCard(
                        totalHours = state.totalHours,
                        window = window,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 60),
                    )
                }

                if (!readonly) {
                    ReviewBanners(state = state, modifier = Modifier.padding(top = 14.dp))
                }

                if (picks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.enrollment_review_empty),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.enrollment_selected_count_format, picks.size),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.57).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 12.dp)
                            .fadeUpOnAppear(delayMs = 120),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fadeUpOnAppear(delayMs = 160),
                    ) {
                        picks.forEach { pick ->
                            ReviewCard(
                                pick = pick,
                                readonly = readonly,
                                onRemove = { onRemove(pick.discipline.id) },
                                onAllowsOtherChanged = { onAllowsOtherChanged(pick.discipline.id, it) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        if (window != null && !readonly) {
            val signal = workloadSignal(state.totalHours, window)
            val blockerLine = blockerLine(state)
            EnrollmentDock(
                totalHours = state.totalHours,
                maxHours = window.maxHours,
                hoursColor = signal.color,
                subText = if (state.conflicts.isNotEmpty()) {
                    pluralStringResource(R.plurals.enrollment_dock_conflicts, state.conflicts.size, state.conflicts.size)
                } else {
                    pluralStringResource(R.plurals.enrollment_dock_disciplines, picks.size, picks.size)
                },
                subColor = if (state.conflicts.isNotEmpty()) MaterialTheme.melon.status.bad else MaterialTheme.colorScheme.outline,
                primaryLabel = stringResource(
                    // A reopened proposal replaces the registered one wholesale.
                    if (state.reopened) R.string.enrollment_dock_resubmit
                    else R.string.enrollment_dock_submit,
                ),
                primaryIcon = Icons.AutoMirrored.Filled.Send,
                primaryEnabled = state.canSubmit,
                primaryFillsWidth = true,
                submitting = state.submitting,
                blockerText = blockerLine.takeIf { picks.isNotEmpty() && it.isNotEmpty() },
                onSave = onBack,
                onPrimary = onSubmit,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
            )
        }
    }

    val submitError = state.submitError
    if (submitError != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(text = stringResource(R.string.enrollment_submit_error_title)) },
            text = { Text(text = enrollmentErrorMessage(submitError)) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(text = stringResource(R.string.enrollment_ok))
                }
            },
        )
    }
}

@Composable
private fun blockerLine(state: EnrollmentUiState): String =
    state.blockers.map { blocker ->
        when (blocker) {
            EnrollmentBlocker.Empty -> stringResource(R.string.enrollment_blocker_empty)
            is EnrollmentBlocker.Conflicts ->
                pluralStringResource(R.plurals.enrollment_blocker_conflicts, blocker.count, blocker.count)
            is EnrollmentBlocker.UnderMinimum ->
                stringResource(R.string.enrollment_blocker_under_format, blocker.missing)
            is EnrollmentBlocker.OverMaximum ->
                stringResource(R.string.enrollment_blocker_over_format, blocker.excess)
        }
    }.joinToString(" · ")

@Composable
private fun ReviewBanners(state: EnrollmentUiState, modifier: Modifier = Modifier) {
    val window = state.window ?: return
    val conflicts = state.conflicts
    val unmet = state.resolvedPicks.filter { it.discipline.hasUnmetPrerequisite }
    val signal = workloadSignal(state.totalHours, window)
    if (conflicts.isEmpty() && !signal.under && !signal.over && unmet.isEmpty()) return
    if (state.resolvedPicks.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (conflicts.isNotEmpty()) {
            EnrollmentBanner(
                tone = EnrollmentBannerTone.Danger,
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.enrollment_review_conflicts_title),
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
            )
        }
        if (signal.under) {
            EnrollmentBanner(
                tone = EnrollmentBannerTone.Warn,
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.enrollment_review_under_title),
                text = stringResource(
                    R.string.enrollment_review_under_text_format,
                    window.minHours - state.totalHours,
                    window.minHours,
                ),
            )
        }
        if (signal.over) {
            EnrollmentBanner(
                tone = EnrollmentBannerTone.Danger,
                icon = Icons.Filled.Warning,
                title = stringResource(R.string.enrollment_review_over_title),
                text = stringResource(
                    R.string.enrollment_review_over_text_format,
                    state.totalHours - window.maxHours,
                ),
            )
        }
        if (unmet.isNotEmpty()) {
            EnrollmentBanner(
                tone = EnrollmentBannerTone.Warn,
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.enrollment_review_prereq_title),
                text = pluralStringResource(
                    R.plurals.enrollment_review_prereq_text,
                    unmet.size,
                    unmet.joinToString(", ") { it.discipline.code },
                ),
            )
        }
    }
}

@Composable
private fun ReviewCard(
    pick: ResolvedPick,
    readonly: Boolean,
    onRemove: () -> Unit,
    onAllowsOtherChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = ColorFor.discipline(pick.discipline.code)
    val shape = RoundedCornerShape(20.dp)
    val line = MaterialTheme.melon.surface.line
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, line, shape),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(hue),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 15.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnrollmentCodeChip(code = pick.discipline.code, hue = hue)
                    Text(
                        text = pick.section.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "· " + stringResource(R.string.enrollment_hours_format, pick.discipline.workload),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Box(modifier = Modifier.weight(1f))
                    if (!readonly) {
                        val removeLabel = stringResource(R.string.enrollment_remove)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, line, RoundedCornerShape(9.dp))
                                .clickable(role = Role.Button, onClickLabel = removeLabel, onClick = onRemove),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = removeLabel,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Text(
                    text = pick.discipline.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.42).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Column(modifier = Modifier.padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!pick.section.hasSchedule) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = stringResource(R.string.enrollment_schedule_tbd),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    } else {
                        scheduleLines(pick.section).forEach { scheduleLine ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(
                                    text = scheduleLine.days,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = scheduleLine.time,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
                if (pick.discipline.hasUnmetPrerequisite || pick.waitlist) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 9.dp),
                    ) {
                        if (pick.discipline.hasUnmetPrerequisite) {
                            EnrollmentTagPill(
                                text = stringResource(R.string.enrollment_prereq_pending_tag),
                                hue = MaterialTheme.melon.status.bad,
                                icon = Icons.Filled.PriorityHigh,
                            )
                        }
                        if (pick.waitlist) {
                            EnrollmentTagPill(
                                text = if (pick.section.waitlistCount > 0) {
                                    stringResource(
                                        R.string.enrollment_waitlist_position_format,
                                        pick.section.waitlistCount + 1,
                                    )
                                } else {
                                    stringResource(R.string.enrollment_waitlist_waiting)
                                },
                                hue = MaterialTheme.melon.status.warn,
                            )
                        }
                    }
                }
            }
        }
        if (!readonly) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = line,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.enrollment_allows_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(
                            R.string.enrollment_allows_review_format,
                            pick.section.label,
                            pick.discipline.code,
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.5.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(checked = pick.allowsOther, onCheckedChange = onAllowsOtherChanged)
            }
        }
    }
}

@Preview
@Composable
private fun EnrollmentReviewPreview() {
    MelonTheme {
        EnrollmentReviewContent(
            state = EnrollmentFixtures.state,
            onBack = {},
            onRemove = {},
            onAllowsOtherChanged = { _, _ -> },
            onSubmit = {},
            onDismissError = {},
        )
    }
}
