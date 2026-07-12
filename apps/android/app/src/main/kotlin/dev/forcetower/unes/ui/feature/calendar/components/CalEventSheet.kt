package dev.forcetower.unes.ui.feature.calendar.components

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarEvent
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.CalendarStatus
import dev.forcetower.unes.ui.feature.calendar.CountdownToken
import dev.forcetower.unes.ui.feature.calendar.color
import java.time.ZoneId
import java.util.Locale

// M3 bottom sheet with the full event detail: category tile + title, big
// countdown card, 2×2 meta grid (Quando / Duração / Âmbito / Situação), the
// fixed-date pin, and an "add to system calendar" CTA. Mirrors the dc
// `CalendarScreen` sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalEventSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
) {
    val category = remember(event) { CalendarMath.categorize(event) }
    val accent = category.color()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
            SheetHeader(event = event, accent = accent, onDismiss = onDismiss)
            Spacer(modifier = Modifier.height(16.dp))
            CountdownCard(event = event)
            Spacer(modifier = Modifier.height(10.dp))
            MetaGrid(event = event)
            if (event.fixed) {
                Spacer(modifier = Modifier.height(10.dp))
                FixedBadge()
            }
            Spacer(modifier = Modifier.height(18.dp))
            AddToCalendarButton(event = event, accent = accent)
        }
    }
}

@Composable
private fun SheetHeader(
    event: CalendarEvent,
    accent: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
) {
    val category = remember(event) { CalendarMath.categorize(event) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CategoryTile(event = event, size = 42.dp, cornerRadius = 13.dp, iconSize = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(category.labelRes) + " · " + stringResource(event.scope.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
            )
            Text(
                text = event.displayDescription,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        val closeLabel = stringResource(R.string.calendar_sheet_close)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = closeLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CountdownCard(event: CalendarEvent) {
    val parts = remember(event) { CalendarMath.countdownParts(event) }
    val numberLabel = when (val n = parts.number) {
        CountdownToken.Today -> stringResource(R.string.calendar_countdown_today)
        CountdownToken.Tomorrow -> stringResource(R.string.calendar_countdown_tomorrow)
        is CountdownToken.Number -> n.value.toString()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.melon.surface.card,
        border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = numberLabel,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.14).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (parts.tailRes != null) {
                Text(
                    text = stringResource(parts.tailRes),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = CalendarFormat.dateRange(event.start, event.end),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun MetaGrid(event: CalendarEvent) {
    val span = remember(event) { CalendarMath.spanDays(event) }
    val duration = if (span == 1) {
        stringResource(R.string.calendar_sheet_single_day)
    } else {
        pluralStringResource(R.plurals.calendar_sheet_span_days, span, span)
    }
    val situation = when (CalendarMath.status(event)) {
        CalendarStatus.Active -> stringResource(R.string.calendar_sheet_status_active)
        CalendarStatus.Past -> stringResource(R.string.calendar_sheet_status_past)
        CalendarStatus.Future -> eventCountdownLabel(event)
            .replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
    val cells = listOf(
        stringResource(R.string.calendar_sheet_when) to CalendarFormat.dateRange(event.start, event.end),
        stringResource(R.string.calendar_sheet_duration) to duration,
        stringResource(R.string.calendar_sheet_scope) to stringResource(event.scope.labelRes),
        stringResource(R.string.calendar_sheet_status) to situation,
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        cells.chunked(2).forEach { rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowCells.forEach { (label, value) ->
                    MetaCell(label = label, value = value, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetaCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.15).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FixedBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = stringResource(R.string.calendar_sheet_fixed_badge),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddToCalendarButton(event: CalendarEvent, accent: androidx.compose.ui.graphics.Color) {
    val context = LocalContext.current
    Button(
        onClick = {
            val zone = ZoneId.systemDefault()
            val begin = event.start.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = event.endOrStart.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.displayDescription)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            }
            // No-op when the device has no calendar app to handle the insert.
            runCatching { context.startActivity(intent) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = MaterialTheme.melon.fixed.onHero,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.Event,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.size(9.dp))
        Text(
            text = stringResource(R.string.calendar_sheet_add_cta),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
