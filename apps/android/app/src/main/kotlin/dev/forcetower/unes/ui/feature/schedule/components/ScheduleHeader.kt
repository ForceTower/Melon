package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear

// Top header for the schedule screen: week-number eyebrow, large serif title,
// week-range subtitle, and a "hoje" pill button on the right (only when the
// active pill isn't today).
@Composable
internal fun ScheduleHeader(
    weekNumber: Int,
    weekRange: String,
    showTodayButton: Boolean,
    onToday: () -> Unit,
    entering: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface

    val baseModifier = if (entering) {
        modifier.fadeUpOnAppear(delayMs = 20, durationMs = 500, fromOffset = 14.dp)
    } else {
        modifier
    }

    Row(
        modifier = baseModifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.schedule_eyebrow_format,
                    weekNumber.toString().padStart(2, '0'),
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                ),
                color = ink3,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.schedule_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.64).sp,
                ),
                color = ink,
            )
            Text(
                text = weekRange,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = ink3,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (showTodayButton) {
            TodayButton(onClick = onToday, ink = ink, surface = surface)
        }
    }
}

@Composable
private fun TodayButton(
    onClick: () -> Unit,
    ink: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ink)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TodayDotGlyph(color = surface)
            Spacer(Modifier.width(0.dp))
            Text(
                text = stringResource(R.string.schedule_today_button),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.06).sp,
                ),
                color = surface,
                maxLines = 1,
            )
        }
    }
}
