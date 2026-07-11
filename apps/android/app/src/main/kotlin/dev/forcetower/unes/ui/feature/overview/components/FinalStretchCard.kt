package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewFinalStretch
import dev.forcetower.unes.ui.feature.overview.OverviewFixtures

// "Reta final" card. When an evaluation is scheduled the card focuses on it
// (design 2a — "Próxima prova"); otherwise it counts down to the semester end
// (design 1a, number style).
@Composable
internal fun FinalStretchCard(
    data: OverviewFinalStretch,
    modifier: Modifier = Modifier,
) {
    when (data) {
        is OverviewFinalStretch.Exam -> ExamCard(data, modifier)
        is OverviewFinalStretch.Semester -> SemesterCard(data, modifier)
    }
}

@Composable
private fun SemesterCard(data: OverviewFinalStretch.Semester, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface(cornerRadius = 20.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = data.daysLeft.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 50.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            StretchEyebrow(stringResource(R.string.overview_final_stretch_label))
            Spacer(Modifier.height(3.dp))
            val days = pluralStringResource(
                R.plurals.overview_final_stretch_days,
                data.daysLeft,
                data.daysLeft,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        MaterialTheme.typography.bodyMedium
                            .copy(fontWeight = FontWeight.Bold)
                            .toSpanStyle(),
                    ) { append(days) }
                    append(" ")
                    append(stringResource(R.string.overview_final_stretch_rest, data.semesterLabel))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExamCard(data: OverviewFinalStretch.Exam, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface(cornerRadius = 22.dp)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StretchEyebrow(stringResource(R.string.overview_next_exam_label))
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                text = data.daysUntil.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pluralStringResource(R.plurals.overview_next_exam_unit, data.daysUntil),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = data.disciplineName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = data.dateLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun StretchEyebrow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// White (card token) plate with hairline border + soft shadow — the design's
// standard elevated card treatment.
@Composable
internal fun Modifier.cardSurface(cornerRadius: Dp): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation = 2.dp, shape = shape, clip = false)
        .clip(shape)
        .background(MaterialTheme.melon.surface.card)
        .border(1.dp, MaterialTheme.melon.surface.line, shape)
}

@Preview
@Composable
private fun FinalStretchSemesterPreview() {
    MelonTheme {
        FinalStretchCard(data = OverviewFixtures.finalStretchSemester)
    }
}

@Preview
@Composable
private fun FinalStretchExamPreview() {
    MelonTheme {
        FinalStretchCard(data = OverviewFixtures.finalStretchExam)
    }
}
