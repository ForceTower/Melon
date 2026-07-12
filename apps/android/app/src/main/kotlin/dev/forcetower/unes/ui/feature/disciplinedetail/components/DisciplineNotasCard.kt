package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.GradeEntry
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.disciplines.hasEqualWeights
import dev.forcetower.unes.ui.feature.disciplines.sectionsForGroup
import java.util.Locale

// "Notas" — iOS-style grade list inside one card: a row per evaluation with
// the tag chip, title, date, and the released grade. The Prova Final rides the
// same card in its own labeled section (never mixed into the regular rows).
@Composable
internal fun DisciplineNotasCard(
    discipline: Discipline,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
) {
    val subject = discipline.color
    val warn = MaterialTheme.melon.status.warn
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line
    val shape = RoundedCornerShape(22.dp)
    val grades = discipline.sectionsForGroup(selectedGroup).flatMap { it.grades }
    val finalExam = discipline.finalExam

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discipline_detail_grades_title).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (discipline.hasEqualWeights) {
                        R.string.discipline_detail_weight_equal
                    } else {
                        R.string.discipline_detail_weight_weighted
                    },
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(card)
                .border(1.dp, cardLine, shape),
        ) {
            if (grades.isEmpty() && finalExam == null) {
                Text(
                    text = stringResource(R.string.discipline_detail_grades_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                )
            }

            grades.forEachIndexed { index, grade ->
                if (index > 0) HorizontalDivider(color = line)
                GradeRow(
                    tag = grade.label.ifEmpty { stringResource(R.string.discipline_detail_grade_no_label) },
                    title = grade.title.ifEmpty { stringResource(R.string.discipline_detail_grade_no_title) },
                    date = grade.date,
                    score = grade.score,
                    tint = subject,
                )
            }

            if (finalExam != null) {
                FinalSectionLabel(warn = warn)
                GradeRow(
                    tag = stringResource(R.string.discipline_detail_final_tag),
                    title = stringResource(R.string.discipline_detail_final_section),
                    date = finalExam.date,
                    score = finalExam.score,
                    tint = warn,
                )
            }
        }
    }
}

@Composable
private fun FinalSectionLabel(warn: Color) {
    HorizontalDivider(thickness = 2.dp, color = warn.copy(alpha = 0.40f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(warn.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Flag,
            contentDescription = null,
            tint = warn,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = stringResource(R.string.discipline_detail_final_section).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
            ),
            color = warn,
        )
    }
}

@Composable
private fun GradeRow(
    tag: String,
    title: String,
    date: String?,
    score: Double?,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                ),
                color = tint,
                maxLines = 1,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date ?: stringResource(R.string.discipline_detail_grade_no_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Text(
            text = if (score != null) {
                formatGrade(score)
            } else {
                stringResource(R.string.discipline_detail_grade_empty_score)
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            ),
            color = DisciplineScoreColor.colorFor(score),
        )
    }
}
