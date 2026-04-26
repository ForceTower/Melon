package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.DisciplineDate
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.GradeEntry
import java.util.Locale

// Small chip for a single evaluation: label badge + score (or "·"/"—" placeholder).
// Used inside `ActiveDisciplineCard` to give a glance-level summary of every
// graded item. Mirrors iOS `EvalChip`.
@Composable
internal fun EvalChip(
    grade: GradeEntry,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val hasScore = grade.score != null
    val past = grade.date?.let { (DisciplineDate.daysUntil(it) ?: 0) < 0 } ?: false

    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasScore) surface2 else Color.Transparent)
            .then(
                if (hasScore) Modifier
                else Modifier.border(1.dp, line, RoundedCornerShape(10.dp)),
            )
            .padding(start = 3.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (hasScore) accent.copy(alpha = 0.13f) else line)
                .padding(horizontal = 5.dp, vertical = 2.dp),
        ) {
            Text(
                text = grade.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.54.sp,
                ),
                color = if (hasScore) accent else ink4,
            )
        }
        Text(
            text = scoreText(grade, hasScore, past),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 14.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.14).sp,
            ),
            color = if (hasScore) DisciplineScoreColor.colorFor(grade.score) else ink4,
        )
    }
}

private fun scoreText(grade: GradeEntry, hasScore: Boolean, past: Boolean): String = when {
    hasScore -> String.format(Locale.US, "%.1f", grade.score)
    past -> "—"
    else -> "·"
}
