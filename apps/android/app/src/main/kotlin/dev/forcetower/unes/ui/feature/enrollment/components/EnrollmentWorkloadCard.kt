package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Resolved meter palette for a given hour total — shared by the workload
// card and the action dock so both read the same signal.
internal data class WorkloadSignal(
    val color: Color,
    val statusText: String,
    val under: Boolean,
    val over: Boolean,
)

@Composable
internal fun workloadSignal(totalHours: Int, window: EnrollmentWindow): WorkloadSignal {
    val under = totalHours < window.minHours
    val over = totalHours > window.maxHours
    return WorkloadSignal(
        color = when {
            over -> MaterialTheme.melon.status.bad
            totalHours == 0 -> MaterialTheme.colorScheme.outlineVariant
            under -> MaterialTheme.melon.status.warn
            else -> MaterialTheme.melon.status.ok
        },
        statusText = when {
            totalHours == 0 -> stringResource(R.string.enrollment_workload_status_empty)
            over -> stringResource(R.string.enrollment_workload_status_over)
            under -> stringResource(R.string.enrollment_workload_status_under_format, window.minHours - totalHours)
            else -> stringResource(R.string.enrollment_workload_status_ok)
        },
        under = under,
        over = over,
    )
}

// Carga-horária meter card (status + review): big hour readout, status chip
// and the min/max band track (dc `MatriculaScreen` workload block).
@Composable
internal fun EnrollmentWorkloadCard(
    totalHours: Int,
    window: EnrollmentWindow,
    modifier: Modifier = Modifier,
) {
    val signal = workloadSignal(totalHours, window)
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.enrollment_workload_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(
                        text = totalHours.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 34.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp,
                        ),
                        color = signal.color,
                    )
                    Text(
                        text = stringResource(R.string.enrollment_workload_hours_suffix),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Text(
                text = signal.statusText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = signal.color,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(signal.color.copy(alpha = 0.16f))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }

        WorkloadTrack(
            totalHours = totalHours,
            window = window,
            fillColor = signal.color,
            modifier = Modifier.padding(top = 14.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.enrollment_workload_min_format, window.minHours),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = stringResource(R.string.enrollment_workload_max_format, window.maxHours),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

// The min→max valid band sits tinted on the track with tick marks at both
// bounds; the fill bar is the current total.
@Composable
private fun WorkloadTrack(
    totalHours: Int,
    window: EnrollmentWindow,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    val trackMax = maxOf(window.maxHours, totalHours).coerceAtLeast(1)
    fun fraction(value: Int): Float = (value.toFloat() / trackMax).coerceIn(0f, 1f)
    val minFraction = fraction(window.minHours)
    val maxFraction = fraction(window.maxHours)
    val fillFraction = fraction(totalHours)
    val ok = MaterialTheme.melon.status.ok

    Box(modifier = modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            if (minFraction > 0f) Box(modifier = Modifier.weight(minFraction))
            if (maxFraction > minFraction) {
                Box(
                    modifier = Modifier
                        .weight(maxFraction - minFraction)
                        .fillMaxHeight()
                        .background(ok.copy(alpha = 0.2f)),
                )
            }
            if (maxFraction < 1f) Box(modifier = Modifier.weight(1f - maxFraction))
        }
        if (fillFraction > 0f) {
            Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(fillColor),
                )
                if (fillFraction < 1f) Box(modifier = Modifier.weight(1f - fillFraction))
            }
        }
        WorkloadTick(fraction = minFraction)
        WorkloadTick(fraction = maxFraction)
    }
}

@Composable
private fun BoxScope.WorkloadTick(fraction: Float) {
    Box(
        modifier = Modifier
            .align(BiasAlignment(horizontalBias = fraction * 2f - 1f, verticalBias = 0f))
            .width(1.5.dp)
            .height(14.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
