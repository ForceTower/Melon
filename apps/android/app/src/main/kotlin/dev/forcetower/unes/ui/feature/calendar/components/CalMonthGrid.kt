package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.color
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

// Month grid card for the "Grade" view: month header with prev/next paging,
// sunday-first weekday initials, and day cells carrying up to three category
// dots. Selected day fills with the accent, today gets a tonal ring. Mirrors
// the dc `CalendarScreen` grid.
@Composable
internal fun CalMonthGrid(
    month: YearMonth,
    events: List<CalendarEvent>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { CalendarMath.today }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.melon.surface.card,
        border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
            MonthHeader(month = month, onPrevMonth = onPrevMonth, onNextMonth = onNextMonth)
            WeekdayHeader()
            MonthCells(
                month = month,
                events = events,
                selected = selected,
                today = today,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    val monthLabel = CalendarFormat.monthsLong[month.monthValue - 1]
        .replaceFirstChar { it.titlecase(Locale.ROOT) } + " " + month.year
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.36).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PagerButton(
                icon = Icons.Filled.ChevronLeft,
                label = stringResource(R.string.calendar_grid_prev_month),
                onClick = onPrevMonth,
            )
            PagerButton(
                icon = Icons.Filled.ChevronRight,
                label = stringResource(R.string.calendar_grid_next_month),
                onClick = onNextMonth,
            )
        }
    }
}

@Composable
private fun PagerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        CalendarFormat.weekdayInitials.forEach { initial ->
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MonthCells(
    month: YearMonth,
    events: List<CalendarEvent>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    // Sunday-first offset: java.time uses 1=monday…7=sunday.
    val leadingBlanks = month.atDay(1).dayOfWeek.value % 7
    val slots = leadingBlanks + month.lengthOfMonth()
    val weeks = (slots + 6) / 7

    Column {
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayOfMonth = week * 7 + column - leadingBlanks + 1
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayOfMonth in 1..month.lengthOfMonth()) {
                            DayCell(
                                date = month.atDay(dayOfMonth),
                                events = events,
                                selected = selected,
                                today = today,
                                onSelect = onSelect,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    events: List<CalendarEvent>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val isSelected = date == selected
    val isToday = date == today
    val categories = remember(events, date) {
        events.filter { CalendarMath.occursOn(it, date) }
            .map { CalendarMath.categorize(it) }
            .distinct()
            .take(3)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(date) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.surfaceContainer
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                ),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )
        }
        Row(
            modifier = Modifier.height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            categories.forEach { category ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.melon.fixed.onHero else category.color(),
                        ),
                )
            }
        }
    }
}
