package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.GradeEntry
import dev.forcetower.unes.ui.feature.disciplines.GradeSection
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import dev.forcetower.unes.ui.feature.disciplines.NeededProjection
import dev.forcetower.unes.ui.feature.disciplines.components.GradeRing
import dev.forcetower.unes.ui.feature.disciplines.sectionsForGroup
import dev.forcetower.unes.ui.feature.disciplines.status
import java.util.Locale

// Grades section for the detail view. Headline card with the partial average
// plus a projection of what's needed to close at 7.0, followed by a row per
// evaluation in each grade section. Mirrors iOS `DisciplineGradesBlock`.
@Composable
internal fun DisciplineGradesBlock(
    discipline: Discipline,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
) {
    val visibleSections = discipline.sectionsForGroup(selectedGroup)
    val visibleGrades = visibleSections.flatMap { it.grades }
    val scores = visibleGrades.mapNotNull { it.score }
    val average = if (scores.isEmpty()) null else scores.sum() / scores.size
    val finalGrade = discipline.finalGrade
    // Closed-out disciplines render the upstream final in the headline ring;
    // the "needed to close at 7" projection only makes sense while the
    // discipline is still in progress, so suppress it once a final exists.
    val needed = if (finalGrade != null) {
        null
    } else {
        run {
            val done = visibleGrades.mapNotNull { it.score }
            val pending = visibleGrades.filter { it.score == null }
            if (done.isEmpty() || pending.isEmpty()) {
                null
            } else {
                val sumDone = done.sum()
                val required = (7.0 * visibleGrades.size - sumDone) / pending.size.toDouble()
                NeededProjection(required = required, pending = pending.size, target = 7.0)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        DisciplineSectionHeader(title = stringResource(R.string.discipline_detail_grades_title))
        Headline(
            average = average,
            finalGrade = finalGrade,
            statusKey = discipline.status.key,
            needed = needed,
            selectedGroup = selectedGroup,
            accent = discipline.color,
        )
        visibleSections.forEach { sec ->
            SectionBlock(
                section = sec,
                accent = discipline.color,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun Headline(
    average: Double?,
    finalGrade: Double?,
    statusKey: DisciplineStatus.Key,
    needed: NeededProjection?,
    selectedGroup: String?,
    accent: Color,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(20.dp)

    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    val isClosed = finalGrade != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradeRing(score = finalGrade ?: average, size = 74.dp, stroke = 5.dp, color = accent)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val headlineLabel = when {
                isClosed && selectedGroup != null ->
                    stringResource(R.string.discipline_detail_grades_final_label_with_group_format, selectedGroup)
                isClosed ->
                    stringResource(R.string.discipline_detail_grades_final_label)
                selectedGroup != null ->
                    stringResource(R.string.discipline_detail_grades_partial_label_with_group_format, selectedGroup)
                else ->
                    stringResource(R.string.discipline_detail_grades_partial_label)
            }
            Text(
                text = headlineLabel.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.08.sp,
                ),
                color = ink4,
            )
            when {
                isClosed -> {
                    val subtitle = when (statusKey) {
                        DisciplineStatus.Key.Approved ->
                            stringResource(R.string.discipline_detail_grades_final_status_approved)
                        DisciplineStatus.Key.Failed ->
                            stringResource(R.string.discipline_detail_grades_final_status_failed)
                        DisciplineStatus.Key.Final ->
                            stringResource(R.string.discipline_detail_grades_final_status_final)
                        else ->
                            stringResource(R.string.discipline_detail_grades_final_status_approved)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = ink2,
                    )
                }
                needed != null -> NeededText(needed = needed, ink2 = ink2)
                else -> Text(
                    text = if (average != null) {
                        stringResource(R.string.discipline_detail_grades_doing_well)
                    } else {
                        stringResource(R.string.discipline_detail_grades_no_grades)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = ink3,
                )
            }
        }
    }
}

@Composable
private fun NeededText(needed: NeededProjection, ink2: Color) {
    val danger = DisciplineScoreColor.danger()
    val caution = DisciplineScoreColor.caution()
    val excellent = DisciplineScoreColor.excellent()
    val requiredColor = when {
        needed.required > 10 -> danger
        needed.required > 7 -> caution
        else -> excellent
    }
    val requiredLabel = if (needed.required > 10) {
        stringResource(R.string.discipline_detail_grades_needed_unreachable)
    } else {
        String.format(Locale.US, "%.1f", needed.required)
    }
    val lead = stringResource(R.string.discipline_detail_grades_needed_lead)
    val tail = if (needed.pending == 1) {
        stringResource(R.string.discipline_detail_grades_needed_pending_one_format, needed.pending)
    } else {
        stringResource(R.string.discipline_detail_grades_needed_pending_other_format, needed.pending)
    }
    val target = stringResource(R.string.discipline_detail_grades_needed_target)
    val dot = stringResource(R.string.discipline_detail_grades_needed_dot)
    val annotated = buildAnnotatedString {
        append(lead)
        withStyle(
            SpanStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = requiredColor,
            ),
        ) {
            append(requiredLabel)
        }
        append(tail)
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(target)
        }
        append(dot)
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        color = ink2,
    )
}

@Composable
private fun SectionBlock(
    section: GradeSection,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.name.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.26.sp,
                ),
                color = accent,
            )
            if (section.group != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = section.group,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.72.sp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(card)
                .border(1.dp, cardLine, shape),
        ) {
            section.grades.forEachIndexed { idx, grade ->
                GradeRow(grade = grade, accent = accent)
                if (idx < section.grades.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(line),
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeRow(grade: GradeEntry, accent: Color) {
    val hasScore = grade.score != null
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Label chip ("AV1" etc).
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(if (hasScore) accent.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = grade.label.ifEmpty { stringResource(R.string.discipline_detail_grade_no_label) },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = if (hasScore) accent else ink4,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = grade.title.ifEmpty { stringResource(R.string.discipline_detail_grade_no_title) },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = grade.date ?: stringResource(R.string.discipline_detail_grade_no_date),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
                color = ink4,
            )
        }

        val emptyScore = stringResource(R.string.discipline_detail_grade_empty_score)
        Text(
            text = if (hasScore) String.format(Locale.US, "%.1f", grade.score) else emptyScore,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.44).sp,
            ),
            color = DisciplineScoreColor.colorFor(grade.score),
        )
    }
}
