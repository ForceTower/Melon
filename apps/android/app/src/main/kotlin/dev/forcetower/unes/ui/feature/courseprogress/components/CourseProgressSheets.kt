package dev.forcetower.unes.ui.feature.courseprogress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementProgress
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.CourseProgressFormat
import dev.forcetower.unes.ui.feature.courseprogress.CurriculumStatusBadge
import dev.forcetower.unes.ui.feature.courseprogress.curriculumStatusStyle

// The two modal surfaces of the feature: the "why is this 0 h" explainer for
// buckets the portal can't measure, and the discipline sheet the fluxograma
// opens for any slot in the grid.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComplementaryHoursSheet(
    requirement: CurriculumRequirementProgress,
    onDismiss: () -> Unit,
) {
    val warn = MaterialTheme.melon.palette.orange
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(warn.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = warn,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requirement.label,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.42).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(
                            R.string.course_progress_explainer_counted_format,
                            CourseProgressFormat.count(requirement.hoursCompleted),
                            CourseProgressFormat.count(requirement.hoursRequired),
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.course_progress_explainer_body1),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = buildAnnotatedString {
                    val emphasis = stringResource(R.string.course_progress_explainer_body2_lead)
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                    ) {
                        append(emphasis)
                    }
                    append(" ")
                    append(stringResource(R.string.course_progress_explainer_body2_tail))
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

// "Ficha da disciplina" — situation, what it depends on, what it unlocks, and
// the entry point into the trail highlight.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurriculumEntrySheet(
    entry: CurriculumEntry,
    progress: CourseProgress,
    onOpenEntry: (String) -> Unit,
    onShowTrail: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val style = curriculumStatusStyle(entry.status)
    val prerequisites = entry.prerequisites.mapNotNull(progress::entry)
    val corequisites = progress.corequisites(entry.code)
    val unlocks = progress.unlocks(entry.code)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CurriculumStatusBadge(
                    status = entry.status,
                    size = 44.dp,
                    corner = 14.dp,
                    iconSize = 22.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.code,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            CurriculumStatusChip(
                status = entry.status,
                full = true,
                modifier = Modifier.padding(top = 14.dp),
            )

            EntryNotice(
                entry = entry,
                progress = progress,
                modifier = Modifier.padding(top = 14.dp),
            )

            EntryFacts(
                entry = entry,
                progress = progress,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
            )

            EntryRelationSection(
                label = stringResource(R.string.course_progress_depends_on),
                entries = prerequisites,
                emptyLabel = stringResource(R.string.course_progress_no_prerequisites),
                progress = progress,
                onOpenEntry = onOpenEntry,
            )

            if (corequisites.isNotEmpty()) {
                EntryRelationSection(
                    label = stringResource(R.string.course_progress_taken_alongside),
                    entries = corequisites,
                    emptyLabel = null,
                    progress = progress,
                    onOpenEntry = onOpenEntry,
                )
            }

            EntryRelationSection(
                label = stringResource(R.string.course_progress_unlocks),
                entries = unlocks,
                emptyLabel = stringResource(R.string.course_progress_unlocks_nothing),
                progress = progress,
                onOpenEntry = onOpenEntry,
            )

            if (prerequisites.isNotEmpty() || unlocks.isNotEmpty()) {
                Button(
                    onClick = { onShowTrail(entry.code) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 9.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.course_progress_show_trail),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

// The one sentence that matters for this situation. Only the four situations
// that need an explanation get one — "cumprida" and "não cursada" speak for
// themselves.
@Composable
private fun EntryNotice(
    entry: CurriculumEntry,
    progress: CourseProgress,
    modifier: Modifier = Modifier,
) {
    val pending = entry.prerequisites
        .mapNotNull(progress::entry)
        .filter { it.status != CurriculumEntryStatus.Completed }

    data class Notice(val tone: Color, val icon: ImageVector, val title: String, val body: String)

    val notice = when (entry.status) {
        CurriculumEntryStatus.Withdrawn -> Notice(
            tone = MaterialTheme.melon.palette.orange,
            icon = Icons.Filled.Pause,
            title = stringResource(R.string.course_progress_withdrawn_title),
            body = stringResource(R.string.course_progress_withdrawn_body),
        )
        CurriculumEntryStatus.Failed -> Notice(
            tone = MaterialTheme.melon.status.bad,
            icon = Icons.Filled.Close,
            title = stringResource(R.string.course_progress_failed_title),
            body = stringResource(R.string.course_progress_failed_body),
        )
        CurriculumEntryStatus.Blocked -> if (pending.isEmpty()) {
            null
        } else {
            Notice(
                tone = MaterialTheme.colorScheme.outline,
                icon = Icons.Filled.Lock,
                title = pluralStringResource(
                    R.plurals.course_progress_blocked_title,
                    pending.size,
                    pending.size,
                ),
                body = pending.joinToString(" · ") { pendingEntry ->
                    "${pendingEntry.code} ${pendingEntry.name}"
                },
            )
        }
        CurriculumEntryStatus.Available -> Notice(
            tone = MaterialTheme.melon.palette.violet,
            icon = Icons.Filled.Add,
            title = stringResource(R.string.course_progress_available_title),
            body = stringResource(
                if (entry.prerequisites.isEmpty()) {
                    R.string.course_progress_available_body_no_prereqs
                } else {
                    R.string.course_progress_available_body
                },
            ),
        )
        else -> null
    } ?: return

    CourseProgressNotice(
        tone = notice.tone,
        icon = notice.icon,
        title = notice.title,
        body = notice.body,
        modifier = modifier,
    )
}

@Composable
private fun EntryFacts(
    entry: CurriculumEntry,
    progress: CourseProgress,
    modifier: Modifier = Modifier,
) {
    val facts = buildList {
        add(
            stringResource(R.string.course_progress_fact_period) to (
                entry.period?.let { CourseProgressFormat.ordinal(it) }
                    ?: stringResource(R.string.course_progress_fact_elective)
                ),
        )
        add(
            stringResource(R.string.course_progress_fact_hours) to stringResource(
                R.string.course_progress_hours_format,
                CourseProgressFormat.count(entry.hours),
            ),
        )
        progress.requirementLabel(entry)?.let {
            add(stringResource(R.string.course_progress_fact_requirement) to it)
        }
        progress.curriculum?.let {
            add(stringResource(R.string.course_progress_fact_curriculum) to it.codeLabel)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        facts.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, value) ->
                    FactTile(key = key, value = value, modifier = Modifier.weight(1f))
                }
                // Keeps a lone trailing tile at half width instead of letting
                // it stretch across the row.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FactTile(key: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = key.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.42.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.21).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun EntryRelationSection(
    label: String,
    entries: List<CurriculumEntry>,
    emptyLabel: String?,
    progress: CourseProgress,
    onOpenEntry: (String) -> Unit,
) {
    if (entries.isEmpty() && emptyLabel == null) return
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.69.sp,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
    if (entries.isEmpty()) {
        Text(
            text = emptyLabel.orEmpty(),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 2.dp, bottom = 18.dp),
        )
        return
    }
    CourseProgressCard(corner = 20.dp, modifier = Modifier.padding(bottom = 18.dp)) {
        entries.forEachIndexed { index, related ->
            CurriculumEntryRow(
                entry = related,
                meta = curriculumEntryMeta(related, progress),
                onClick = { onOpenEntry(related.code) },
                showDivider = index > 0,
            )
        }
    }
}

// "CHF344 · 3º semestre · Cumprida" — the one line every related row carries.
@Composable
internal fun curriculumEntryMeta(entry: CurriculumEntry, progress: CourseProgress): String {
    val style = curriculumStatusStyle(entry.status)
    val period = entry.period?.let {
        stringResource(
            R.string.course_progress_semester_format,
            CourseProgressFormat.ordinal(it),
        )
    } ?: progress.requirementLabel(entry)
    return listOfNotNull(entry.code, period, style.label).joinToString(" · ")
}
