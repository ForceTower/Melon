package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarMonthGroup

// Month section in the agenda variant: italic month name + a small "year ·
// N eventos" line + an optional "AGORA" pill if this is the current month,
// then the stack of `CalAgendaRow`s. Mirrors iOS `CalMonthSection`.
@Composable
internal fun CalMonthSection(group: CalendarMonthGroup) {
    val today = remember { CalendarMath.today }
    val isCurrent = today.year == group.year && today.monthValue == group.month
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val monthName = CalendarFormat.monthsLong[group.month - 1]

    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.52).sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = if (isCurrent) accent else ink,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.calendar_month_summary,
                    group.events.size,
                    group.year,
                    group.events.size,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                ),
                color = ink3,
            )
            if (isCurrent) {
                Spacer(modifier = Modifier.weight(1f))
                NowBadge(accent = accent)
            }
        }

        group.events.forEachIndexed { idx, ev ->
            CalAgendaRow(event = ev, isLast = idx == group.events.lastIndex)
        }
    }
}

@Composable
private fun NowBadge(accent: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "month-now-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "month-now-alpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = pulse)),
        )
        Text(
            text = stringResource(R.string.calendar_month_now_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5f.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.33.sp,
            ),
            color = accent,
        )
    }
}
