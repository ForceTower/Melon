package dev.forcetower.unes.ui.feature.schedule.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.formatShortDayMonth
import dev.forcetower.unes.ui.feature.schedule.formatShortDayName
import java.util.Locale

// The week's agenda in list form, under the grid (dc `ScheduleGridScreen`):
// one section per day with class, each row a tonal card with the time column,
// the discipline color bar, and the "Agora" badge on the live class.
@Composable
internal fun ScheduleGridAgenda(
    days: List<List<ScheduleClass>>,
    dateIsos: List<String?>,
    todayIdx: Int,
    nowMinute: Int,
    onOpenClass: (Int, ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rowIndex = 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp),
    ) {
        days.forEachIndexed { dayIdx, classes ->
            if (classes.isEmpty()) return@forEachIndexed
            val iso = dateIsos.getOrNull(dayIdx)
            Column(modifier = Modifier.padding(bottom = 22.dp)) {
                AgendaDayHeader(
                    name = formatShortDayName(iso),
                    date = formatShortDayMonth(iso),
                    isToday = dayIdx == todayIdx,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    classes.forEach { cls ->
                        AgendaRow(
                            cls = cls,
                            state = gridClassState(dayIdx, todayIdx, cls, nowMinute),
                            delayMs = 500 + rowIndex * 50,
                            onClick = { onOpenClass(dayIdx, cls) },
                        )
                        rowIndex++
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaDayHeader(
    name: String,
    date: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = name.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.3.sp,
            ),
            color = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun AgendaRow(
    cls: ScheduleClass,
    state: GridClassState,
    delayMs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalMelonDarkTheme.current
    val shape = RoundedCornerShape(20.dp)
    // Location and teacher get a line each — real locations ("Módulo 3 ·
    // PAT36 · UEFS") plus a full teacher name never fit one row. Same call
    // the timeline's `FooterRow` makes.
    val location = listOfNotNull(cls.modulo, cls.room, cls.campus).joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fadeUpOnAppear(delayMs = delayMs, durationMs = 440, fromOffset = 10.dp)
            .then(if (state == GridClassState.Done) Modifier.alpha(0.5f) else Modifier)
            .clip(shape)
            .background(
                cls.color
                    .copy(alpha = if (dark) 0.10f else 0.07f)
                    .compositeOver(MaterialTheme.melon.surface.card),
            )
            .border(1.dp, cls.color.copy(alpha = 0.24f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(44.dp)) {
            Text(
                text = cls.start,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = cls.end,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(cls.color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cls.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.15).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (location.isNotEmpty()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (cls.prof.isNotEmpty()) {
                Text(
                    text = cls.prof,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (state == GridClassState.Now) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(cls.color)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.schedule_grid_now_badge)
                        .uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                    ),
                    color = MaterialTheme.melon.fixed.onHero,
                )
            }
        }
    }
}
