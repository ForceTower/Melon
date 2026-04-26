package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.components.AbsenceBar

// "Presença" block — full absence-tracking card. Headline (used / allowed)
// plus remaining slot count, the segmented bar, and a tone-coded warning
// banner once the ratio crosses 50%. Mirrors iOS `DisciplineFaltasBlock`.
@Composable
internal fun DisciplineFaltasBlock(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val ratio = if (discipline.allowedAbsences > 0) {
        discipline.absences.toDouble() / discipline.allowedAbsences.toDouble()
    } else {
        0.0
    }
    val tone = when {
        ratio >= 0.75 -> DisciplineScoreColor.danger()
        ratio >= 0.50 -> DisciplineScoreColor.caution()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val remaining = (discipline.allowedAbsences - discipline.absences).coerceAtLeast(0)

    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(18.dp)
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        DisciplineSectionHeader(title = stringResource(R.string.discipline_detail_presence_title))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(card)
                .border(1.dp, cardLine, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = discipline.absences.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 28.sp,
                                lineHeight = 28.sp,
                                letterSpacing = (-0.56).sp,
                            ),
                            color = tone,
                        )
                        Text(
                            text = stringResource(R.string.discipline_detail_presence_of_format, discipline.allowedAbsences),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 18.sp,
                                lineHeight = 18.sp,
                            ),
                            color = ink4,
                        )
                    }
                    Text(
                        text = stringResource(R.string.discipline_detail_presence_so_far),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = ink3,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = remaining.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                        color = ink,
                    )
                    Text(
                        text = stringResource(R.string.discipline_detail_presence_remaining_label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.9.sp,
                        ),
                        color = ink4,
                    )
                }
            }

            AbsenceBar(used = discipline.absences, allowed = discipline.allowedAbsences)

            if (ratio >= 0.5) {
                WarningBanner(
                    tone = tone,
                    text = if (ratio >= 0.75) {
                        stringResource(R.string.discipline_detail_presence_warn_high)
                    } else {
                        stringResource(R.string.discipline_detail_presence_warn_mid)
                    },
                )
            }
        }
    }
}

@Composable
private fun WarningBanner(tone: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tone.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WarningTriangleIcon(tint = tone)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = tone,
        )
    }
}

@Composable
private fun WarningTriangleIcon(tint: Color) {
    Canvas(modifier = Modifier.size(13.dp)) {
        val strokePx = 1.3.dp.toPx()
        val w = size.width
        val h = size.height
        val triangle = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            lineTo(w * 0.95f, h * 0.90f)
            lineTo(w * 0.05f, h * 0.90f)
            close()
        }
        drawPath(
            path = triangle,
            color = tint,
            style = Stroke(width = strokePx, join = StrokeJoin.Round),
        )
        // Exclamation: tall stem + dot.
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.40f),
            end = Offset(w * 0.5f, h * 0.65f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.78f),
            end = Offset(w * 0.5f, h * 0.83f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
    }
}

