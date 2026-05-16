package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.AbsenceRisk
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.absenceRisk
import dev.forcetower.unes.ui.feature.disciplines.partialAverage
import java.util.Locale

// 3-cell summary strip rendered at the top of the list. Mirrors iOS
// `CurrentSemesterSummary` — Média parcial · Disciplinas · Atenção.
@Composable
internal fun CurrentSemesterSummary(
    disciplines: List<Discipline>,
    modifier: Modifier = Modifier,
) {
    val averages = disciplines.mapNotNull { it.partialAverage }
    val mean = if (averages.isEmpty()) null else averages.sum() / averages.size
    val atRisk = disciplines.count { it.absenceRisk != AbsenceRisk.Ok }
    val lows = disciplines.count { (it.partialAverage ?: 10.0) < 6.0 && it.partialAverage != null }
    val attention = atRisk + lows

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCell(
            label = stringResource(R.string.disciplines_summary_partial_average),
            value = mean?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
            color = if (mean != null) DisciplineScoreColor.colorFor(mean) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        SummaryCell(
            label = stringResource(R.string.disciplines_summary_count),
            value = disciplines.size.toString(),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        SummaryCell(
            label = stringResource(R.string.disciplines_summary_attention),
            value = attention.toString(),
            color = if (attention > 0) DisciplineScoreColor.caution() else MaterialTheme.colorScheme.onBackground,
            subtitle = if (attention > 0) {
                stringResource(R.string.disciplines_summary_attention_items)
            } else {
                stringResource(R.string.disciplines_summary_attention_none)
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.85.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.44).sp,
            ),
            color = color,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
