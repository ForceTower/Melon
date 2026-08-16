package dev.forcetower.unes.ui.feature.courseprogress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumPeriod
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.CourseProgressFormat
import dev.forcetower.unes.ui.feature.courseprogress.CurriculumStatusBadge
import dev.forcetower.unes.ui.feature.courseprogress.CurriculumTrail
import dev.forcetower.unes.ui.feature.courseprogress.curriculumSlotSurface
import dev.forcetower.unes.ui.feature.courseprogress.curriculumStatusStyle
import dev.forcetower.unes.ui.feature.courseprogress.curriculumStatusTone
import dev.forcetower.unes.ui.feature.courseprogress.trailDim

// The fluxograma's three lenses over the same grid: one período at a time,
// the whole course as a map of squares, and the side-by-side grid of cards.

// ───────── Período rail ─────────

@Composable
internal fun CurriculumPeriodRail(
    periods: List<CurriculumPeriod>,
    selected: Int?,
    currentPeriod: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        periods.forEach { period ->
            val number = period.period ?: return@forEach
            RailPill(
                period = period,
                number = number,
                active = number == selected,
                isCurrent = number == currentPeriod,
                onClick = { onSelect(number) },
            )
        }
    }
}

@Composable
private fun RailPill(
    period: CurriculumPeriod,
    number: Int,
    active: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val onActive = MaterialTheme.colorScheme.onPrimaryContainer
    val total = period.entries.size
    val completed = period.completedCount
    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(shape)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.melon.surface.card
                },
            )
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.melon.surface.line,
                shape,
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = CourseProgressFormat.ordinal(number),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = if (active) onActive else MaterialTheme.colorScheme.onBackground,
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completed.toFloat() / total },
            color = if (active) onActive else MaterialTheme.melon.status.ok,
            trackColor = if (active) {
                onActive.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
        )
        Text(
            text = if (isCurrent) {
                stringResource(R.string.course_progress_rail_now)
            } else {
                stringResource(R.string.course_progress_rail_count_format, completed, total)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = when {
                active -> onActive.copy(alpha = 0.75f)
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        )
    }
}

// ───────── Lens · períodos ─────────

@Composable
internal fun CurriculumPeriodsLens(
    progress: CourseProgress,
    period: CurriculumPeriod,
    trail: CurriculumTrail?,
    onSelectPeriod: (Int) -> Unit,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val number = period.period ?: return
    val scheduled = progress.scheduledPeriods
    val index = scheduled.indexOfFirst { it.period == number }
    val previous = scheduled.getOrNull(index - 1)?.period
    val next = scheduled.getOrNull(index + 1)

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 0.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.course_progress_semester_format,
                        CourseProgressFormat.ordinal(number),
                    ),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.55).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(
                        R.string.course_progress_period_subtitle_format,
                        period.entries.size,
                        stringResource(
                            R.string.course_progress_hours_format,
                            CourseProgressFormat.count(period.hours),
                        ),
                        period.completedCount,
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            OutlinedIconButton(
                onClick = { previous?.let(onSelectPeriod) },
                enabled = previous != null,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.course_progress_previous_semester),
                )
            }
            OutlinedIconButton(
                onClick = { next?.period?.let(onSelectPeriod) },
                enabled = next != null,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.course_progress_next_semester),
                )
            }
        }

        CourseProgressCard {
            // Ordered by situation: what is done, what is happening, what can
            // be picked, then what is stuck — the enum's own order.
            period.entries.sortedBy { it.status.ordinal }.forEachIndexed { position, entry ->
                CurriculumEntryRow(
                    entry = entry,
                    meta = entryRowMeta(entry),
                    onClick = { onOpenEntry(entry.code) },
                    showDivider = position > 0,
                    dimmed = trail != null && entry.code !in trail.codes,
                    // Short label only: a long one ("Pré-requisito não
                    // cumprido") squeezes the discipline name into three
                    // lines. The full wording lives in the legend and sheet.
                    trailing = { CurriculumStatusChip(status = entry.status) },
                )
            }
        }

        val nextNumber = next?.period
        if (next != null && nextNumber != null) {
            val available = next.count(CurriculumEntryStatus.Available)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { onSelectPeriod(nextNumber) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.course_progress_semester_format,
                            CourseProgressFormat.ordinal(nextNumber),
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (available > 0) {
                            pluralStringResource(
                                R.plurals.course_progress_next_available,
                                available,
                                available,
                            )
                        } else {
                            stringResource(R.string.course_progress_next_none_available)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// "CHF344 · 60 h" — the row's own identity; the situation rides the chip.
@Composable
private fun entryRowMeta(entry: CurriculumEntry): String = listOf(
    entry.code,
    stringResource(
        R.string.course_progress_hours_format,
        CourseProgressFormat.count(entry.hours),
    ),
).joinToString(" · ")

// ───────── Lens · map ─────────

@Composable
internal fun CurriculumMapLens(
    progress: CourseProgress,
    trail: CurriculumTrail?,
    onOpenEntry: (String) -> Unit,
    onSelectPeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = progress.scheduledPeriods
    val entries = progress.entries

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.course_progress_map_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.55).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    R.string.course_progress_map_subtitle_format,
                    entries.size,
                    entries.count { it.status == CurriculumEntryStatus.Completed },
                    periods.size,
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        CourseProgressCard {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                periods.forEach { period ->
                    val number = period.period ?: return@forEach
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = CourseProgressFormat.ordinal(number),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = if (number == progress.currentPeriod) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(28.dp)
                                .clickable(role = Role.Button) { onSelectPeriod(number) }
                                .padding(bottom = 3.dp)
                                .trailDim(
                                    trail != null && period.entries.none { it.code in trail.codes },
                                ),
                        )
                        period.entries.forEach { entry ->
                            MapTile(
                                entry = entry,
                                trail = trail,
                                onClick = { onOpenEntry(entry.code) },
                            )
                        }
                    }
                }
            }
        }

        if (trail != null) {
            TrailGroups(
                progress = progress,
                trail = trail,
                onOpenEntry = onOpenEntry,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Text(
            text = stringResource(R.string.course_progress_legend).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.69.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
        )
        CurriculumLegend()
        Text(
            text = stringResource(R.string.course_progress_map_hint),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp),
        )
    }
}

@Composable
private fun MapTile(
    entry: CurriculumEntry,
    trail: CurriculumTrail?,
    onClick: () -> Unit,
) {
    val inTrail = trail != null && entry.code in trail.codes
    val focused = trail?.focus == entry.code
    val tone = curriculumStatusTone(entry.status)
    val description = "${entry.code} ${entry.name} — ${curriculumStatusStyle(entry.status).label}"
    val ringShape = RoundedCornerShape(11.dp)
    Box(
        modifier = Modifier
            .trailDim(trail != null && !inTrail)
            .then(
                // The chain reads as a ring around the tiles that belong to
                // it; the focus gets the strongest one. The 2dp gutter is
                // always reserved so turning a trail on never reflows the map.
                when {
                    focused -> Modifier.border(2.dp, tone, ringShape)
                    inTrail -> Modifier.border(1.5.dp, tone.copy(alpha = 0.5f), ringShape)
                    else -> Modifier
                },
            )
            .padding(2.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        CurriculumStatusBadge(
            status = entry.status,
            size = 28.dp,
            corner = 9.dp,
            iconSize = 15.dp,
        )
    }
}

// With a chain highlighted, the map alone can't say which square is which —
// these two lists name them, split by direction.
@Composable
private fun TrailGroups(
    progress: CourseProgress,
    trail: CurriculumTrail,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = progress.entry(trail.focus) ?: return
    val before = progress.chainEntries(progress.upstream(trail.focus))
    val after = progress.chainEntries(progress.downstream(trail.focus))
    val directBefore = focus.prerequisites.toSet()
    val directAfter = progress.unlocks(trail.focus).map { it.code }.toSet()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TrailGroup(
            icon = Icons.Filled.Lock,
            iconTint = MaterialTheme.colorScheme.outline,
            title = stringResource(R.string.course_progress_trail_before),
            entries = before,
            direct = directBefore,
            emptyLabel = stringResource(R.string.course_progress_trail_before_empty),
            progress = progress,
            onOpenEntry = onOpenEntry,
        )
        TrailGroup(
            icon = Icons.Filled.LockOpen,
            iconTint = MaterialTheme.melon.palette.sky,
            title = stringResource(R.string.course_progress_trail_after),
            entries = after,
            direct = directAfter,
            emptyLabel = stringResource(R.string.course_progress_unlocks_nothing),
            progress = progress,
            onOpenEntry = onOpenEntry,
        )
    }
}

@Composable
private fun TrailGroup(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    entries: List<CurriculumEntry>,
    direct: Set<String>,
    emptyLabel: String,
    progress: CourseProgress,
    onOpenEntry: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.69.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            // The empty case says so in a full sentence below; a "0" here
            // would only repeat it.
            if (entries.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.course_progress_trail_group_count,
                        entries.size,
                        entries.size,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        CourseProgressCard(corner = 20.dp) {
            if (entries.isEmpty()) {
                Text(
                    text = emptyLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    CurriculumEntryRow(
                        entry = entry,
                        meta = curriculumEntryMeta(entry, progress),
                        onClick = { onOpenEntry(entry.code) },
                        showDivider = index > 0,
                        emphasized = entry.code in direct,
                    )
                }
            }
        }
    }
}

// ───────── Lens · grid ─────────

@Composable
internal fun CurriculumGridLens(
    progress: CourseProgress,
    trail: CurriculumTrail?,
    onOpenEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.course_progress_grid_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.55).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.course_progress_grid_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            progress.scheduledPeriods.forEach { period ->
                val number = period.period ?: return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GridColumnHeader(
                        number = number,
                        hours = period.hours,
                        isCurrent = number == progress.currentPeriod,
                    )
                    period.entries.forEach { entry ->
                        GridCell(
                            entry = entry,
                            dimmed = trail != null && entry.code !in trail.codes,
                            onClick = { onOpenEntry(entry.code) },
                        )
                    }
                }
            }
        }

        CurriculumLegend(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

@Composable
private fun GridColumnHeader(number: Int, hours: Int, isCurrent: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .width(156.dp)
            .clip(shape)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (isCurrent) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = stringResource(
                R.string.course_progress_semester_format,
                CourseProgressFormat.ordinal(number),
            ),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.26).sp,
            ),
            color = content,
        )
        Text(
            text = stringResource(
                R.string.course_progress_hours_format,
                CourseProgressFormat.count(hours),
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = content.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun GridCell(
    entry: CurriculumEntry,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val style = curriculumStatusStyle(entry.status)
    val corner = 18.dp
    val faintEdge = MaterialTheme.melon.surface.line
    Column(
        modifier = Modifier
            .width(156.dp)
            .heightIn(min = 96.dp)
            .trailDim(dimmed)
            .clip(RoundedCornerShape(corner))
            .then(
                if (style.fillAlpha == 0f) {
                    Modifier.background(MaterialTheme.melon.surface.card)
                } else {
                    Modifier
                },
            )
            .curriculumSlotSurface(style, corner, faintEdge)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.code,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.32.sp,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
            CurriculumStatusBadge(
                status = entry.status,
                size = 20.dp,
                corner = 7.dp,
                iconSize = 13.dp,
            )
        }
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.12).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.course_progress_hours_format,
                    CourseProgressFormat.count(entry.hours),
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = style.shortLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = if (entry.status == CurriculumEntryStatus.NotTaken) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    style.tone
                },
                maxLines = 1,
            )
        }
    }
}

// ───────── Trail banner ─────────

@Composable
internal fun CurriculumTrailBanner(
    focusCode: String,
    linkedCount: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = MaterialTheme.melon.palette.sky
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.3f), shape)
            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AccountTree,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(
                R.string.course_progress_trail_banner_format,
                focusCode,
                linkedCount,
            ),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) {
            Text(
                text = stringResource(R.string.course_progress_trail_clear),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                ),
                color = tone,
            )
        }
    }
}
