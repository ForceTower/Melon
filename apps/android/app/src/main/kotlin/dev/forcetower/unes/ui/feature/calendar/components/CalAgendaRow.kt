package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarCategory
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarStatus
import dev.forcetower.unes.ui.feature.calendar.color
import java.util.Locale

// Single row in the agenda variant: a 60dp date gutter (giant day, weekday)
// with a 1dp vertical rail connecting consecutive rows, then a card holding
// the eyebrow + title + optional "anual" tag. Mirrors `CalAgendaRow` in
// `screens-calendar.jsx` + iOS `CalAgendaRow`.
@Composable
internal fun CalAgendaRow(event: CalendarEvent, isLast: Boolean) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val status = remember(event) { CalendarMath.status(event) }
    val isPast = status == CalendarStatus.Past
    val isActive = status == CalendarStatus.Active
    val hasRange = event.end != null
    val accent = category.color()

    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .height(IntrinsicSize.Min)
            .alpha(if (isPast) 0.45f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DateGutter(
            event = event,
            isActive = isActive,
            isLast = isLast,
            hasRange = hasRange,
            accent = accent,
        )
        AgendaCard(
            event = event,
            category = category,
            accent = accent,
            isPast = isPast,
            isActive = isActive,
        )
    }
}

@Composable
private fun DateGutter(
    event: CalendarEvent,
    isActive: Boolean,
    isLast: Boolean,
    hasRange: Boolean,
    accent: Color,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val line = MaterialTheme.melon.surface.line

    Box(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight(),
    ) {
        Column(modifier = Modifier.padding(top = 14.dp)) {
            Text(
                text = "%02d".format(event.start.dayOfMonth),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.44).sp,
                    fontStyle = if (hasRange) FontStyle.Italic else FontStyle.Normal,
                ),
                color = if (isActive) accent else ink,
            )
            Spacer(modifier = Modifier.height(3.dp))
            WeekdayLabel(event = event)
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(line),
            )
        }
    }
}

@Composable
private fun AgendaCard(
    event: CalendarEvent,
    category: CalendarCategory,
    accent: Color,
    isPast: Boolean,
    isActive: Boolean,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPast) accent.copy(alpha = 0.4f) else accent),
        )
        Column(
            modifier = Modifier
                .padding(start = 11.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MetaRow(event = event, category = category, accent = accent, isActive = isActive)
            Text(
                text = event.displayDescription,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.17).sp,
                ),
                color = ink,
            )
            if (event.fixed) {
                AnnualBadge()
            }
        }
    }
}

@Composable
private fun WeekdayLabel(event: CalendarEvent) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val weekday = CalendarFormat.weekday(event.start).uppercase(Locale.ROOT)
    val end = event.end
    if (end == null) {
        Text(weekday, style = monoStyle(9.5f), color = ink3)
    } else {
        val startMonthIdx = event.start.monthValue - 1
        val endMonthIdx = end.monthValue - 1
        val trail = if (startMonthIdx == endMonthIdx) {
            "→ %02d".format(end.dayOfMonth)
        } else {
            "→ %02d %s".format(end.dayOfMonth, CalendarFormat.monthsShort[endMonthIdx].uppercase(Locale.ROOT))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(weekday, style = monoStyle(9.5f), color = ink3)
            Text(trail, style = monoStyle(9.5f), color = ink4)
        }
    }
}

@Composable
private fun MetaRow(
    event: CalendarEvent,
    category: CalendarCategory,
    accent: Color,
    isActive: Boolean,
) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accent.copy(alpha = 0.10f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalCategoryGlyph(category = category, color = accent, size = 11.dp)
                Text(
                    text = stringResource(category.labelRes).uppercase(Locale.ROOT),
                    style = monoStyle(9.5f, FontWeight.SemiBold).copy(letterSpacing = 1.14.sp),
                    color = accent,
                )
            }
        }
        Text(
            text = "· " + stringResource(event.scope.labelRes).uppercase(Locale.ROOT),
            style = monoStyle(9.5f).copy(letterSpacing = 0.95.sp),
            color = ink4,
        )
        if (isActive && !event.closed) {
            Spacer(modifier = Modifier.weight(1f))
            OpenBadge()
        }
    }
}

@Composable
private fun OpenBadge() {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "agenda-open-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "agenda-open-alpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = pulse)),
        )
        Text(
            text = stringResource(R.string.calendar_status_open).uppercase(Locale.ROOT),
            style = monoStyle(9f, FontWeight.SemiBold).copy(letterSpacing = 1.26.sp),
            color = accent,
        )
    }
}

@Composable
private fun AnnualBadge() {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.padding(top = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            val s = size.width
            val strokePx = 1.2f * density
            drawLine(
                color = ink4,
                start = Offset(s / 2f, 1.5f / 10f * s),
                end = Offset(s / 2f, 8.5f / 10f * s),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = ink4,
                start = Offset(1.5f / 10f * s, s / 2f),
                end = Offset(8.5f / 10f * s, s / 2f),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = stringResource(R.string.calendar_badge_annual).uppercase(Locale.ROOT),
            style = monoStyle(9.5f).copy(letterSpacing = 0.95.sp),
            color = ink4,
        )
    }
}

@Composable
private fun monoStyle(sizeSp: Float, weight: FontWeight = FontWeight.Normal) =
    MaterialTheme.typography.labelSmall.copy(
        fontSize = sizeSp.sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
    )
