package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineDate
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.NeededProjection
import dev.forcetower.unes.ui.feature.disciplines.allGrades
import dev.forcetower.unes.ui.feature.disciplines.completedCount
import dev.forcetower.unes.ui.feature.disciplines.groupsShortLabel
import dev.forcetower.unes.ui.feature.disciplines.hasMultipleGroups
import dev.forcetower.unes.ui.feature.disciplines.needed
import dev.forcetower.unes.ui.feature.disciplines.nextEvaluation
import dev.forcetower.unes.ui.feature.disciplines.partialAverage
import dev.forcetower.unes.ui.feature.disciplines.status
import dev.forcetower.unes.ui.feature.disciplines.totalEvaluations
import java.util.Locale

// Rich list card for a current-semester discipline. Code chip + status pill,
// title, professor/hours meta, a grade ring, eval chips, and a footer with
// absences + the appropriate projection (next eval / needed average / progress).
// Mirrors iOS `ActiveDisciplineCard`.
@Composable
internal fun ActiveDisciplineCard(
    discipline: Discipline,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .drawBehind {
                // Color rail painted directly inside the clipped card rect:
                // 3dp wide, inset 14dp top and bottom, matches iOS
                // `ActiveDisciplineCard` rail geometry. Drawn here instead of
                // overlaying a sibling Box so it tracks the Column-driven
                // card height without fighting `matchParentSize`/width
                // ordering.
                val railWidthPx = 3.dp.toPx()
                val railInsetPx = 14.dp.toPx()
                val railHeight = (size.height - railInsetPx * 2f).coerceAtLeast(0f)
                drawRect(
                    color = discipline.color,
                    topLeft = Offset(0f, railInsetPx),
                    size = Size(railWidthPx, railHeight),
                )
            }
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TopRow(discipline)
            EvalChips(discipline)
            FooterRow(discipline)
        }
    }
}

@Composable
private fun TopRow(discipline: Discipline) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = discipline.fullCode,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = discipline.color,
                )
                StatusPill(status = discipline.status)
            }
            Text(
                text = discipline.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.30).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaRow(discipline)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GradeRing(
                score = discipline.partialAverage,
                size = 56.dp,
                stroke = 4.dp,
                color = discipline.color,
            )
            Text(
                text = stringResource(R.string.disciplines_card_grade_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.72.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun MetaRow(discipline: Discipline) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = discipline.prof,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        DotSeparator()
        Text(
            text = stringResource(R.string.disciplines_card_hours_format, discipline.hours),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = ink3,
        )
        if (discipline.hasMultipleGroups) {
            DotSeparator()
            GroupsBadge(label = discipline.groupsShortLabel.orEmpty(), accent = discipline.color)
        }
    }
}

@Composable
private fun DotSeparator() {
    Text(
        text = "·",
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun GroupsBadge(label: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.38.sp,
            ),
            color = accent,
        )
    }
}

@Composable
private fun EvalChips(discipline: Discipline) {
    val grades = discipline.allGrades
    if (grades.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        grades.forEach { grade ->
            EvalChip(grade = grade, accent = discipline.color)
        }
    }
}

@Composable
private fun FooterRow(discipline: Discipline) {
    val line = MaterialTheme.melon.surface.line
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 10.dp)
            .drawBehind {
                // Dashed top separator. iOS uses a 1px dashed stroke; mirror
                // that inside Compose's draw scope so we don't pull in a
                // heavier dependency just for the separator.
                val strokePx = 1.dp.toPx()
                val dash = floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                drawLine(
                    color = line,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = strokePx,
                    pathEffect = PathEffect.dashPathEffect(dash, 0f),
                )
            }
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FooterCell(
                title = stringResource(R.string.disciplines_card_footer_absences),
                modifier = Modifier.weight(1f),
            ) {
                AbsenceBar(used = discipline.absences, allowed = discipline.allowedAbsences)
            }
            FooterCell(
                title = footerTitle(discipline),
                modifier = Modifier.weight(1f),
            ) {
                FooterValue(discipline)
            }
        }
    }
}

@Composable
private fun footerTitle(discipline: Discipline): String {
    val next = discipline.nextEvaluation
    val countdown = next?.date?.let(DisciplineDate::daysUntil)
    return when {
        countdown != null && countdown >= 0 -> stringResource(R.string.disciplines_card_footer_next_eval)
        discipline.needed(7.0) != null -> stringResource(R.string.disciplines_card_footer_for_seven)
        else -> stringResource(R.string.disciplines_card_footer_progress)
    }
}

@Composable
private fun FooterCell(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        content()
    }
}

@Composable
private fun FooterValue(discipline: Discipline) {
    val next = discipline.nextEvaluation
    val countdown = next?.date?.let(DisciplineDate::daysUntil)

    when {
        next != null && countdown != null && countdown >= 0 ->
            CountdownValue(days = countdown, label = next.label)
        else -> {
            val needed = discipline.needed(7.0)
            if (needed != null) NeededValue(needed) else ProgressValue(discipline)
        }
    }
}

@Composable
private fun CountdownValue(days: Int, label: String) {
    val accent = if (days <= 3) DisciplineScoreColor.caution() else MaterialTheme.colorScheme.onBackground
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = if (days == 0) {
                stringResource(R.string.disciplines_card_countdown_today)
            } else {
                stringResource(R.string.disciplines_card_countdown_days_format, days)
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 18.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.36).sp,
            ),
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NeededValue(needed: NeededProjection) {
    val color = when {
        needed.required > 10 -> DisciplineScoreColor.danger()
        needed.required > 7 -> DisciplineScoreColor.caution()
        else -> DisciplineScoreColor.excellent()
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = if (needed.required > 10) "—" else String.format(Locale.US, "%.1f", needed.required),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 18.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.36).sp,
            ),
            color = color,
        )
        Text(
            text = if (needed.required > 10) {
                stringResource(R.string.disciplines_card_needed_unreachable)
            } else {
                pluralStringResource(R.plurals.disciplines_card_needed_remaining, needed.pending, needed.pending)
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressValue(discipline: Discipline) {
    val completed = discipline.completedCount
    val total = discipline.totalEvaluations
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = "$completed/$total",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 18.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.36).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.disciplines_card_progress_evaluations),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

