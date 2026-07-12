package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarStatus
import dev.forcetower.unes.ui.feature.calendar.color
import dev.forcetower.unes.ui.feature.calendar.icon
import java.util.Locale

// Tonal event card shared by the agenda list and the grid's selected-day
// list: date gutter (agenda only) + filled category tile + eyebrow / title /
// countdown + trailing chevron. Tapping opens the event sheet. Mirrors
// `mkEvent` rows in the dc `CalendarScreen`.
@Composable
internal fun CalEventRow(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val status = remember(event) { CalendarMath.status(event) }
    val accent = category.color()
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (status == CalendarStatus.Past) 0.5f else 1f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.melon.surface.card,
        border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showDate) {
                DateColumn(event = event, accent = accent, isActive = status == CalendarStatus.Active)
            }
            CategoryTile(event = event)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = stringResource(category.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = accent,
                    )
                    Text(
                        text = "· " + stringResource(event.scope.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = ink4,
                    )
                }
                Text(
                    text = event.displayDescription,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.14).sp,
                    ),
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = eventCountdownLabel(event),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = ink3,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = ink4,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DateColumn(
    event: CalendarEvent,
    accent: androidx.compose.ui.graphics.Color,
    isActive: Boolean,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val end = event.end
    val subtitle = CalendarFormat.weekday(event.start).uppercase(Locale.ROOT) +
        if (end != null) "–%02d".format(end.dayOfMonth) else ""
    Column(
        modifier = Modifier.width(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "%02d".format(event.start.dayOfMonth),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 21.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.42).sp,
            ),
            color = if (isActive) accent else ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = ink4,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

// 38dp filled tile carrying the category glyph — shared shape with the sheet's
// 42dp header tile.
@Composable
internal fun CategoryTile(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 11.dp,
    iconSize: androidx.compose.ui.unit.Dp = 19.dp,
) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val accent = category.color()
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier.size(iconSize),
        )
    }
}

// Relative sentence under the row title — "em 12 dias", "termina hoje",
// "há 3 dias". Mirrors `countdown()` in the dc.
@Composable
internal fun eventCountdownLabel(event: CalendarEvent): String {
    val today = CalendarMath.today
    val toStart = CalendarMath.daysBetween(today, event.start)
    val toEnd = CalendarMath.daysBetween(today, event.endOrStart)
    return when {
        toStart == 0 -> stringResource(R.string.calendar_countdown_today)
        toStart == 1 -> stringResource(R.string.calendar_countdown_tomorrow)
        toStart > 0 -> pluralStringResource(R.plurals.calendar_row_countdown_in, toStart, toStart)
        toEnd == 0 -> stringResource(R.string.calendar_row_countdown_ends_today)
        toEnd > 0 -> pluralStringResource(R.plurals.calendar_row_countdown_ends_in, toEnd, toEnd)
        else -> pluralStringResource(R.plurals.calendar_row_countdown_ago, -toStart, -toStart)
    }
}
