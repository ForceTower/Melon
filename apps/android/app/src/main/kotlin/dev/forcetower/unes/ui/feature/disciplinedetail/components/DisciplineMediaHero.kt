package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import dev.forcetower.unes.ui.feature.disciplines.allGrades
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.disciplines.isAwaitingFinal
import dev.forcetower.unes.ui.feature.disciplines.partialAverage
import dev.forcetower.unes.ui.feature.disciplines.status
import java.util.Locale

// Tonal hero card — big partial (or final) mean, released-grades tally, the
// upstream verdict pill, and an M3 determinate ring of the mean out of 10.
// Tinted with the discipline color like the dc `Média parcial` card.
@Composable
internal fun DisciplineMediaHero(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val subject = discipline.color
    val card = MaterialTheme.melon.surface.card
    val shape = RoundedCornerShape(26.dp)
    // "Média final" only once upstream recorded a verdict — finalGrade is
    // published LIVE (as the partial mean) while the Prova Final is pending,
    // so its presence alone doesn't close the discipline.
    val closed = discipline.approved != null
    val shown = if (closed) {
        discipline.finalGrade ?: discipline.partialAverage
    } else {
        discipline.partialAverage
    }
    val released = discipline.allGrades.count { it.score != null }
    val total = discipline.allGrades.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(subject.copy(alpha = 0.10f).compositeOver(card))
            .border(1.dp, subject.copy(alpha = 0.24f), shape),
    ) {
        // Soft color bloom on the top-right corner, echoing the dc blurred
        // circle — a radial brush fading to transparent reads the same
        // without a real blur pass.
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(subject.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (closed) R.string.discipline_detail_media_final else R.string.discipline_detail_media_partial,
                    ).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                    ),
                    color = subject,
                )
                StatusPill(discipline = discipline)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = formatGrade(shown),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 60.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-2.4).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val caption = when {
                        closed && (discipline.wentToFinals || discipline.finalExam != null) ->
                            stringResource(R.string.discipline_detail_media_after_final)
                        total == 0 || released == 0 ->
                            stringResource(R.string.discipline_detail_media_no_grades)
                        else ->
                            pluralStringResource(R.plurals.discipline_detail_media_released, total, released, total)
                    }
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                MediaRing(value = shown, subject = subject)
            }
        }
    }
}

// M3 determinate circular indicator with the mean centered — the ring fills
// mean/10.
@Composable
private fun MediaRing(value: Double?, subject: Color) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { ((value ?: 0.0) / 10.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.size(84.dp),
            color = subject,
            trackColor = subject.copy(alpha = 0.20f),
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = formatGrade(value),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
            ),
            color = subject,
        )
    }
}

// Verdict pill. Upstream `approved` is authoritative; "Prova final" while the
// exam is pending; grade-threshold statuses only while in progress.
@Composable
private fun StatusPill(discipline: Discipline, modifier: Modifier = Modifier) {
    val ok = MaterialTheme.melon.status.ok
    val warn = MaterialTheme.melon.status.warn
    val bad = MaterialTheme.melon.status.bad

    data class Pill(val label: String, val color: Color, val icon: ImageVector)
    val pill = when {
        discipline.approved == true && (discipline.wentToFinals || discipline.finalExam != null) -> Pill(
            stringResource(R.string.discipline_detail_status_approved_final),
            ok,
            Icons.Filled.WorkspacePremium,
        )
        discipline.approved == true -> Pill(
            stringResource(R.string.discipline_detail_status_approved),
            ok,
            Icons.Filled.CheckCircle,
        )
        discipline.approved == false -> Pill(
            stringResource(R.string.discipline_detail_status_failed),
            bad,
            Icons.Filled.Cancel,
        )
        discipline.isAwaitingFinal -> Pill(
            stringResource(R.string.discipline_detail_status_finals),
            warn,
            Icons.Filled.Flag,
        )
        else -> when (discipline.status.key) {
            DisciplineStatus.Key.Low -> Pill(
                stringResource(R.string.discipline_detail_status_low),
                warn,
                Icons.Filled.WarningAmber,
            )
            DisciplineStatus.Key.Pending -> Pill(
                stringResource(R.string.discipline_detail_status_pending),
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Filled.HourglassEmpty,
            )
            else -> Pill(
                stringResource(R.string.discipline_detail_status_ongoing),
                MaterialTheme.colorScheme.primary,
                Icons.Filled.TrendingUp,
            )
        }
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pill.color.copy(alpha = 0.16f))
            .padding(start = 9.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = pill.icon,
            contentDescription = null,
            tint = pill.color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = pill.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = pill.color,
        )
    }
}
