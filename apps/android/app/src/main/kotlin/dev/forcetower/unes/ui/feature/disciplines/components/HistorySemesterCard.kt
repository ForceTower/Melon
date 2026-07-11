package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesFixtures
import dev.forcetower.unes.ui.feature.disciplines.Semester
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.disciplines.formatSemesterCode
import dev.forcetower.unes.ui.feature.disciplines.status
import dev.forcetower.unes.ui.feature.disciplines.tinted
import java.util.Locale

// Accordion card of the Histórico tab — semester header (term, discipline
// count, mean + approvals, per-discipline hue dots) that expands into one row
// per discipline with the final grade and verdict.
@Composable
internal fun HistorySemesterCard(
    semester: Semester,
    open: Boolean,
    onToggle: () -> Unit,
    onOpenDiscipline: (Discipline) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
    ) {
        HeaderRow(semester = semester, open = open, onToggle = onToggle)
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(animationSpec = tween(280, easing = MelonMotion.EmphasizedEasing)) +
                fadeIn(animationSpec = tween(280, easing = MelonMotion.EmphasizedEasing)),
            exit = shrinkVertically(animationSpec = tween(220, easing = MelonMotion.EmphasizedEasing)) +
                fadeOut(animationSpec = tween(220, easing = MelonMotion.EmphasizedEasing)),
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                semester.disciplines.forEach { discipline ->
                    DisciplineRow(
                        discipline = discipline,
                        onOpen = { onOpenDiscipline(discipline) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(semester: Semester, open: Boolean, onToggle: () -> Unit) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = MelonMotion.ease(),
        label = "chevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatSemesterCode(semester.id),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.disciplines_history_disc_count,
                        semester.disciplines.size,
                        semester.disciplines.size,
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            SummaryLine(semester = semester)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            semester.disciplines.take(6).forEach { discipline ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor(discipline)),
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .size(24.dp)
                .rotate(chevronAngle),
        )
    }
}

@Composable
private fun SummaryLine(semester: Semester) {
    val disciplines = semester.disciplines
    val approved = disciplines.count { it.approved == true }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.disciplines_past_mean_label),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = formatGrade(semesterMean(disciplines)),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "· " + stringResource(
                R.string.disciplines_past_approved_format,
                approved,
                disciplines.size,
            ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun DisciplineRow(discipline: Discipline, onOpen: () -> Unit) {
    val hue = discipline.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 13.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .heightIn(min = 34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(hue),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = discipline.code,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.55.sp,
                ),
                color = hue,
            )
            Text(
                text = discipline.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        VerdictColumn(discipline = discipline)
    }
}

@Composable
private fun VerdictColumn(discipline: Discipline) {
    val status = discipline.status
    val melon = MaterialTheme.melon
    val (color, labelRes) = when (status.key) {
        DisciplineStatus.Key.Approved -> melon.status.ok to R.string.disciplines_status_approved
        DisciplineStatus.Key.Failed -> melon.status.bad to R.string.disciplines_status_failed
        DisciplineStatus.Key.Final -> melon.status.warn to R.string.disciplines_status_final
        else -> MaterialTheme.colorScheme.outline to R.string.disciplines_status_pending
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = formatGrade(discipline.finalGrade),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                lineHeight = 20.sp,
            ),
            color = color,
        )
        Text(
            text = stringResource(labelRes).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun dotColor(discipline: Discipline): Color =
    if (discipline.approved == false) MaterialTheme.melon.status.bad else discipline.color

// Hours-weighted mean of the semester's final grades — the same weighting the
// lifetime CR uses, scoped to one semester. Falls back to equal weights when
// hour counts are missing.
private fun semesterMean(disciplines: List<Discipline>): Double? {
    var weightedSum = 0.0
    var weightSum = 0.0
    for (discipline in disciplines) {
        val grade = discipline.finalGrade ?: continue
        val weight = if (discipline.hours > 0) discipline.hours.toDouble() else 1.0
        weightedSum += grade * weight
        weightSum += weight
    }
    return if (weightSum > 0.0) weightedSum / weightSum else null
}

@Preview
@Composable
private fun HistorySemesterCardPreview() {
    MelonTheme {
        var open by rememberSaveable { mutableStateOf(true) }
        HistorySemesterCard(
            semester = DisciplinesFixtures.PAST.first().tinted(MaterialTheme.melon.palette),
            open = open,
            onToggle = { open = !open },
            onOpenDiscipline = {},
        )
    }
}
