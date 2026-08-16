package dev.forcetower.unes.ui.feature.courseprogress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementProgress
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.CourseProgressFormat
import dev.forcetower.unes.ui.feature.courseprogress.curriculumStatusTone

// The three cards that make up "Progresso do curso": the overall hours gauge,
// the per-nature breakdown, and the "faltam ao todo" rollup — plus the
// fluxograma entrance with its mini-map of the whole grid.

// ───────── Overall gauge ─────────

@Composable
internal fun CourseProgressGaugeCard(
    progress: CourseProgress,
    modifier: Modifier = Modifier,
) {
    val summary = progress.summary
    val required = summary.requiredHours
    val ok = MaterialTheme.melon.status.ok
    // Prefer the server's capped percent: surplus electives shouldn't push a
    // raw hours ratio past 100.
    val fraction = summary.percent?.let { (it / 100).toFloat() }
        ?: required?.takeIf { it > 0 }?.let { summary.completedHours.toFloat() / it }

    CourseProgressCard(modifier = modifier, corner = 28.dp) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (required == null) {
                                R.string.course_progress_overall_hours_completed
                            } else {
                                R.string.course_progress_overall_completed
                            },
                        ).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = summary.percent?.let { CourseProgressFormat.headlinePercent(it) }
                                ?: CourseProgressFormat.count(summary.completedHours),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 44.sp,
                                lineHeight = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1.8).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (summary.percent == null) {
                            Text(
                                text = stringResource(R.string.course_progress_hour_unit),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (required == null) {
                            "—"
                        } else {
                            stringResource(
                                R.string.course_progress_hours_of_required_format,
                                CourseProgressFormat.count(summary.completedHours),
                                CourseProgressFormat.count(required),
                            )
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = summary.remainingHours?.let {
                            stringResource(
                                R.string.course_progress_remaining_short_format,
                                stringResource(
                                    R.string.course_progress_hours_format,
                                    CourseProgressFormat.count(it),
                                ),
                            )
                        } ?: stringResource(R.string.course_progress_total_unknown),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            Box(modifier = Modifier.padding(top = 16.dp)) {
                if (fraction == null) {
                    HatchedTrack(height = 12.dp)
                } else {
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        color = ok,
                        trackColor = ok.copy(alpha = 0.18f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val period = progress.currentPeriod
                if (period != null) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.melon.surface.line, CircleShape)
                            .padding(start = 8.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.course_progress_semester_format,
                                CourseProgressFormat.ordinal(period),
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(Modifier)
                }
                val curriculum = progress.curriculum
                if (curriculum != null) {
                    Text(
                        text = stringResource(
                            if (curriculum.stale) {
                                R.string.course_progress_curriculum_code_stale
                            } else {
                                R.string.course_progress_curriculum_code
                            },
                            curriculum.codeLabel,
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

// ───────── Fluxograma entrance ─────────

@Composable
internal fun CurriculumFlowCard(
    progress: CourseProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = progress.hasCurriculum
    val periods = progress.scheduledPeriods
    val entries = progress.entries
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .then(if (available) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 17.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.course_progress_flowchart_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.24).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (available) {
                        stringResource(
                            R.string.course_progress_flowchart_subtitle_format,
                            periods.size,
                            entries.size,
                            entries.count { it.status == CurriculumEntryStatus.Completed },
                        )
                    } else {
                        stringResource(R.string.course_progress_flowchart_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (available) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (available && periods.isNotEmpty()) {
            // Mini-map: one column per período, one bar per discipline. Not a
            // control — it's the shape of the course, so the reader knows what
            // the fluxograma opens onto.
            // Short períodos are padded with blank slots so every column ends
            // on the same line and the semester numbers read as one row.
            val tallest = periods.maxOf { it.entries.size }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                periods.forEach { period ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        period.entries.forEach { entry ->
                            MiniMapBar(status = entry.status)
                        }
                        repeat(tallest - period.entries.size) {
                            Spacer(Modifier.height(7.dp))
                        }
                        Text(
                            text = CourseProgressFormat.ordinal(period.period ?: 0),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = if (period.period == progress.currentPeriod) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMapBar(status: CurriculumEntryStatus) {
    val tone = curriculumStatusTone(status)
    val shape = RoundedCornerShape(3.dp)
    val filled = status == CurriculumEntryStatus.Completed
    val washed = status == CurriculumEntryStatus.InProgress || status == CurriculumEntryStatus.Failed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(shape)
            .background(
                when {
                    filled -> tone
                    washed -> tone.copy(alpha = 0.22f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .then(
                // The two situations that need a mark of their own at this
                // size: "cursando" gets a ring, "trancada" an outline.
                when (status) {
                    CurriculumEntryStatus.InProgress ->
                        Modifier.border(1.5.dp, tone, shape)
                    CurriculumEntryStatus.Withdrawn ->
                        Modifier.border(1.dp, tone.copy(alpha = 0.8f), shape)
                    else -> Modifier
                },
            ),
    )
}

// ───────── Per-nature breakdown ─────────

@Composable
internal fun CurriculumRequirementsCard(
    requirements: List<CurriculumRequirementProgress>,
    totalKnown: Boolean,
    modifier: Modifier = Modifier,
) {
    // The "paper certificates" note explains a rule, not a row — repeating it
    // under every unmeasurable bucket just buries the buckets. It rides the
    // first one; the explainer button below covers the rest.
    val firstUnmeasurable = requirements.firstOrNull { !it.derivable }?.code

    CourseProgressCard(modifier = modifier) {
        requirements.forEachIndexed { index, requirement ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
            }
            RequirementRow(
                requirement = requirement,
                totalKnown = totalKnown,
                showUnmeasurableNote = requirement.code == firstUnmeasurable,
            )
        }
    }
}

@Composable
private fun RequirementRow(
    requirement: CurriculumRequirementProgress,
    totalKnown: Boolean,
    showUnmeasurableNote: Boolean,
) {
    val ok = MaterialTheme.melon.status.ok
    val warn = MaterialTheme.melon.palette.orange
    // Two different reasons a bar can't be filled: the bucket itself isn't
    // observable, or the whole curriculum has no totals. Both hatch the track,
    // but only the first gets the explanatory note.
    val unmeasurable = !requirement.derivable
    val tone = if (unmeasurable) warn else ok
    val fraction = requirement.percent?.let { (it / 100).toFloat() }
        ?: requirement.hoursRequired.takeIf { it > 0 }
            ?.let { requirement.hoursCompleted.toFloat() / it }

    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = requirement.shortLabel,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.14).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    unmeasurable -> stringResource(
                        R.string.course_progress_hours_of_required_format,
                        CourseProgressFormat.count(requirement.hoursCompleted),
                        CourseProgressFormat.count(requirement.hoursRequired),
                    )
                    !totalKnown -> stringResource(
                        R.string.course_progress_hours_completed_short_format,
                        stringResource(
                            R.string.course_progress_hours_format,
                            CourseProgressFormat.count(requirement.hoursCompleted),
                        ),
                    )
                    else -> stringResource(
                        R.string.course_progress_hours_of_required_format,
                        CourseProgressFormat.count(requirement.hoursCompleted),
                        CourseProgressFormat.count(requirement.hoursRequired),
                    )
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (unmeasurable || !totalKnown) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }

        Box(modifier = Modifier.padding(top = 10.dp)) {
            if (unmeasurable || !totalKnown || fraction == null) {
                HatchedTrack()
            } else {
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    color = tone,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RequirementHint(
                requirement = requirement,
                totalKnown = totalKnown,
                modifier = Modifier.weight(1f),
            )
            if (totalKnown && !unmeasurable && requirement.hoursRemaining > 0) {
                Text(
                    text = stringResource(
                        R.string.course_progress_remaining_short_format,
                        stringResource(
                            R.string.course_progress_hours_format,
                            CourseProgressFormat.count(requirement.hoursRemaining),
                        ),
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        if (unmeasurable && showUnmeasurableNote) {
            val shape = RoundedCornerShape(14.dp)
            Text(
                text = stringResource(R.string.course_progress_not_counted_note),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 11.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(warn.copy(alpha = 0.10f))
                    .border(1.dp, warn.copy(alpha = 0.24f), shape)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun RequirementHint(
    requirement: CurriculumRequirementProgress,
    totalKnown: Boolean,
    modifier: Modifier = Modifier,
) {
    val ok = MaterialTheme.melon.status.ok
    val warn = MaterialTheme.melon.palette.orange
    val startsAt = requirement.startsAtPeriod
    val started = requirement.hoursCompleted > 0

    val icon = when {
        !requirement.derivable -> Icons.Filled.Description
        totalKnown && !started && startsAt != null -> Icons.Filled.Schedule
        else -> null
    }
    val text = when {
        !requirement.derivable -> stringResource(R.string.course_progress_not_counted_yet)
        !totalKnown -> stringResource(R.string.course_progress_no_total_for_curriculum)
        started -> stringResource(
            R.string.course_progress_completed_suffix_format,
            CourseProgressFormat.percent(
                requirement.percent
                    ?: (requirement.hoursCompleted * 100.0 / requirement.hoursRequired.coerceAtLeast(1)),
            ),
        )
        startsAt != null -> stringResource(
            R.string.course_progress_starts_at_format,
            stringResource(
                R.string.course_progress_semester_format,
                CourseProgressFormat.ordinal(startsAt),
            ),
        )
        else -> stringResource(R.string.course_progress_not_started)
    }
    val color = when {
        !requirement.derivable -> warn
        started && totalKnown -> ok
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ───────── "Faltam ao todo" ─────────

@Composable
internal fun CurriculumRemainingCard(
    requirements: List<CurriculumRequirementProgress>,
    curriculumCode: String,
    modifier: Modifier = Modifier,
) {
    val pending = requirements
        .filter { it.hoursRemaining > 0 }
        .sortedByDescending { it.hoursRemaining }
    if (pending.isEmpty()) return
    val warn = MaterialTheme.melon.palette.orange

    CourseProgressCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.course_progress_remaining_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.22).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    R.string.course_progress_hours_format,
                    CourseProgressFormat.count(pending.sumOf { it.hoursRemaining }),
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.36).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            pending.forEach { requirement ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (requirement.derivable) {
                                    MaterialTheme.colorScheme.outlineVariant
                                } else {
                                    warn
                                },
                            ),
                    )
                    Text(
                        text = requirement.shortLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.course_progress_hours_format,
                            CourseProgressFormat.count(requirement.hoursRemaining),
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        Text(
            text = stringResource(
                R.string.course_progress_remaining_footnote_format,
                curriculumCode,
            ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 15.dp),
        )
    }
}

@Composable
internal fun requirementsHint(count: Int): String =
    pluralStringResource(R.plurals.course_progress_requirements_hint, count, count)
