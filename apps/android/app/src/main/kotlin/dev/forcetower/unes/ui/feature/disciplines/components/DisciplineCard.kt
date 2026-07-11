package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesFixtures
import dev.forcetower.unes.ui.feature.disciplines.allGrades
import dev.forcetower.unes.ui.feature.disciplines.completedCount
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.disciplines.partialAverage
import dev.forcetower.unes.ui.feature.disciplines.status
import dev.forcetower.unes.ui.feature.disciplines.tinted
import dev.forcetower.unes.ui.feature.disciplines.totalEvaluations
import java.util.Locale

// Tonal course card of the "Atual" tab — code + status chips, discipline
// name, teacher line, the média progress ring, one chip per evaluation, and
// the faltas bar + avaliações count footer. Card plate and accents are tinted
// by the discipline's stable palette hue. Mirrors the dc course card.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DisciplineCard(
    discipline: Discipline,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = discipline.color
    val cardBg = hue.copy(alpha = 0.08f).compositeOver(MaterialTheme.melon.surface.card)
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(1.dp, hue.copy(alpha = 0.22f), shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodeChip(code = discipline.code, hue = hue)
                    StatusChip(status = discipline.status)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = discipline.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        letterSpacing = (-0.17).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subLine(discipline),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(14.dp))
            AverageRing(average = discipline.partialAverage, hue = hue)
        }

        val grades = discipline.allGrades
        if (grades.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            // Wraps past three chips per row — real semesters can carry more
            // evaluations than the design's three (e.g. the "Adicional" finals
            // row), and squeezing them clips the grade values.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3,
            ) {
                grades.forEach { grade ->
                    EvaluationChip(
                        label = grade.label,
                        score = grade.score,
                        hue = hue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.melon.surface.line),
        )
        Spacer(Modifier.height(13.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            AbsencesMeter(
                absences = discipline.absences,
                allowed = discipline.allowedAbsences,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(18.dp))
            EvaluationsCount(
                done = discipline.completedCount,
                total = discipline.totalEvaluations,
            )
        }
    }
}

@Composable
private fun CodeChip(code: String, hue: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(hue.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.48.sp,
            ),
            color = hue,
        )
    }
}

@Composable
private fun StatusChip(status: DisciplineStatus) {
    val melon = MaterialTheme.melon
    val (color, icon, labelRes) = when (status.key) {
        DisciplineStatus.Key.Approved ->
            Triple(melon.status.ok, Icons.Filled.CheckCircle, R.string.disciplines_status_approved)
        DisciplineStatus.Key.Failed ->
            Triple(melon.status.bad, Icons.Filled.Cancel, R.string.disciplines_status_failed)
        DisciplineStatus.Key.Final ->
            Triple(melon.status.warn, Icons.Filled.Error, R.string.disciplines_status_final)
        DisciplineStatus.Key.Low ->
            Triple(melon.status.warn, Icons.Filled.Error, R.string.disciplines_status_low)
        DisciplineStatus.Key.Ongoing, DisciplineStatus.Key.Pending ->
            Triple(MaterialTheme.colorScheme.primary, Icons.Filled.Schedule, R.string.disciplines_status_ongoing)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.22.sp),
            color = color,
        )
    }
}

// 60dp progress ring — arc sweep is the partial average over the 0–10 scale,
// value centered, "MÉDIA" caption underneath. Track is the same hue faded so
// the ring stays on-palette in both themes.
@Composable
private fun AverageRing(average: Double?, hue: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            val track = hue.copy(alpha = 0.20f)
            val sweep = ((average ?: 0.0) / 10.0).coerceIn(0.0, 1.0).toFloat() * 360f
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 5.dp.toPx())
                val inset = stroke.width / 2f
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width),
                )
                drawArc(
                    color = hue,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width),
                )
            }
            Text(
                text = formatGrade(average),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.36).sp,
                ),
                color = hue,
            )
        }
        Text(
            text = stringResource(R.string.disciplines_card_grade_label).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun EvaluationChip(
    label: String,
    score: Double?,
    hue: Color,
    modifier: Modifier = Modifier,
) {
    val melon = MaterialTheme.melon
    val valueColor = when {
        score == null -> MaterialTheme.colorScheme.outlineVariant
        score < 5.0 -> melon.status.bad
        score >= 9.0 -> melon.status.ok
        else -> MaterialTheme.colorScheme.onBackground
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(hue.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                ),
                color = hue,
                maxLines = 1,
            )
        }
        Text(
            text = formatGrade(score),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
        )
    }
}

// Linear absences meter — fill fraction is hours missed over the allowed
// budget; color escalates ok → warn → bad as the budget is consumed.
@Composable
private fun AbsencesMeter(
    absences: Int,
    allowed: Int,
    modifier: Modifier = Modifier,
) {
    val melon = MaterialTheme.melon
    val ratio = if (allowed > 0) absences.toFloat() / allowed else 0f
    val fill = when {
        ratio < 0.5f -> melon.status.ok
        ratio < 0.8f -> melon.status.warn
        else -> melon.status.bad
    }
    val remaining = (allowed - absences).coerceAtLeast(0)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.disciplines_card_footer_absences).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(9.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.melon.surface.line),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .widthIn(min = if (absences > 0) 4.dp else 0.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fill),
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = remaining.toString(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = fill,
            )
            Text(
                text = pluralStringResource(R.plurals.disciplines_card_free_absences_label, remaining),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EvaluationsCount(done: Int, total: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(R.string.disciplines_card_evaluations_label).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                text = done.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "/$total",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun subLine(discipline: Discipline): String {
    val hours = stringResource(R.string.disciplines_card_hours_format, discipline.hours)
    return if (discipline.prof.isBlank()) hours else "${discipline.prof} · $hours"
}

@Preview
@Composable
private fun DisciplineCardPreview() {
    MelonTheme {
        val discipline = DisciplinesFixtures.CURRENT.disciplines[1]
            .tinted(MaterialTheme.melon.palette)
        DisciplineCard(discipline = discipline, onOpen = {})
    }
}
