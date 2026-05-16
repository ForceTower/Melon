package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass

// Horizontal 7-pill week selector — weekday + date + class-count dots, with
// the active day filled and an accent dot marking today when inactive.
@Composable
internal fun ScheduleWeekSpine(
    activeIdx: Int,
    onChange: (Int) -> Unit,
    week: List<List<ScheduleClass>>,
    dates: List<Int>,
    todayIdx: Int,
    entering: Boolean,
    modifier: Modifier = Modifier,
) {
    val dayLabels = stringArrayResource(R.array.schedule_days_short)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0 until 7) {
            val pillModifier = if (entering) {
                Modifier
                    .weight(1f)
                    .fadeUpOnAppear(delayMs = 120 + i * 40, durationMs = 400, fromOffset = 8.dp)
            } else {
                Modifier.weight(1f)
            }
            WeekPill(
                modifier = pillModifier,
                label = dayLabels.getOrNull(i).orEmpty(),
                day = dates.getOrNull(i) ?: 0,
                isActive = i == activeIdx,
                isToday = i == todayIdx,
                isWeekend = i >= 5,
                count = week.getOrNull(i)?.size ?: 0,
                onClick = { onChange(i) },
            )
        }
    }
}

@Composable
private fun WeekPill(
    label: String,
    day: Int,
    isActive: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.melon.surface.line
    val interaction = remember { MutableInteractionSource() }

    val primaryText = when {
        isActive -> surface
        isWeekend -> ink4
        else -> ink2
    }
    val labelOpacity = if (isActive) 0.7f else 0.6f
    val dotColor = if (isActive) surface else ink4
    val dotOpacity = if (isActive) 0.7f else 0.5f

    val shape = RoundedCornerShape(14.dp)
    val baseBackground = if (isActive) ink else Color.Transparent
    val borderModifier = if (isActive) Modifier else Modifier.border(1.dp, line, shape)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(baseBackground)
                .then(borderModifier)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
                .semantics {
                    role = Role.Tab
                    selected = isActive
                }
                .padding(top = 10.dp, bottom = 9.dp, start = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
                color = primaryText.copy(alpha = labelOpacity),
            )
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.2).sp,
                ),
                color = primaryText,
            )
            Row(
                modifier = Modifier
                    .heightIn(min = 3.dp)
                    .padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val visibleDots = count.coerceAtMost(4)
                repeat(visibleDots) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = dotOpacity)),
                    )
                }
            }
        }
        if (isToday && !isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}
