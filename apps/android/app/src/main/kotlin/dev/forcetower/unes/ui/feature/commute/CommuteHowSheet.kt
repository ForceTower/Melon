package dev.forcetower.unes.ui.feature.commute

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitEstimateBasis
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

// "Como calculamos": plain language, the example's own numbers, and a tiny
// distribution of past trips with the shown band highlighted. An M3 bottom
// sheet over the board or the follow screen (iOS `CommuteHowSheet`).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommuteHowSheet(
    arrival: TransitArrival?,
    onDismiss: () -> Unit,
) {
    val now = rememberCommuteNow()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.commute_how_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.commute_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (arrival != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TransitLineBadge(number = arrival.lineNumber)
                    TransitEstimateText(estimate = CommuteFormat.estimate(arrival, now), size = 26.sp)
                    TransitChip(kind = transitChipKind(arrival), small = true)
                }
                val basis = arrival.basis
                if (basis != null && basis.sampleCount >= 2 && basis.dayCount >= 2) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.commute_how_basis, basis.sampleCount, basis.dayCount),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.commute_how_hit_rate, (basis.hitRate * 10).roundToInt()),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(18.dp))
                    Distribution(basis = basis, arrival = arrival)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Reason(Icons.Filled.Sensors, R.string.commute_how_live_title, R.string.commute_how_live_body)
                Reason(Icons.Filled.History, R.string.commute_how_history_title, R.string.commute_how_history_body)
                Reason(
                    Icons.Filled.CalendarMonth,
                    R.string.commute_how_ignored_title,
                    R.string.commute_how_ignored_body,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.commute_how_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

// One bar per histogram bin; the bins inside the shown band take the line's
// color, the rest stay quiet. Axes are implied by the three labels below.
@Composable
private fun Distribution(
    basis: TransitEstimateBasis,
    arrival: TransitArrival,
) {
    val color = MaterialTheme.melon.transit.line(arrival.lineNumber)
    val quiet = MaterialTheme.colorScheme.surfaceContainerHigh
    val binLength = basis.binMinutes.minutes
    val end = basis.histogramStart + binLength * basis.counts.size
    val band = clockRangeLabel(arrival.earliest, arrival.latest)
    val description = stringResource(R.string.commute_how_chart_accessibility, band)
    val maxCount = (basis.counts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .commuteCard()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)
            .semantics { contentDescription = description },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            val gap = 4.dp.toPx()
            val barWidth = (size.width - gap * (basis.counts.size - 1)) / basis.counts.size
            // The band spans the bins it overlaps; it sits behind the bars as a tint.
            val bandStart = basis.counts.indices.firstOrNull { index ->
                basis.histogramStart + binLength * (index + 1) > arrival.earliest
            }
            val bandEnd = basis.counts.indices.lastOrNull { index ->
                basis.histogramStart + binLength * index < arrival.latest
            }
            if (bandStart != null && bandEnd != null && bandEnd >= bandStart) {
                val left = bandStart * (barWidth + gap) - gap / 2
                val right = (bandEnd + 1) * (barWidth + gap) - gap / 2
                drawRoundRect(
                    color = color.copy(alpha = 0.12f),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
            basis.counts.forEachIndexed { index, count ->
                val inBand = bandStart != null && bandEnd != null && index in bandStart..bandEnd
                val height = size.height * count / maxCount
                drawRoundRect(
                    color = if (inBand) color else quiet,
                    topLeft = Offset(index * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AxisLabel(clockLabel(basis.histogramStart), bold = false)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AxisLabel(band, bold = true)
            }
            AxisLabel(clockLabel(end), bold = false)
        }
    }
}

@Composable
private fun AxisLabel(
    text: String,
    bold: Boolean,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        color = if (bold) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun Reason(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
