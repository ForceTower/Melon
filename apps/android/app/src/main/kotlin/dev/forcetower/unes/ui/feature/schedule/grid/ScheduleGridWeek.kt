package dev.forcetower.unes.ui.feature.schedule.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.RevealShadow
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.durationMin
import dev.forcetower.unes.ui.feature.schedule.endMin
import dev.forcetower.unes.ui.feature.schedule.startMin

// The week grid proper (dc `ScheduleGridScreen`, compact density): the hour
// rail on the left, one column per day with tonal class blocks positioned by
// minute, hairline hour lines, an accent tint over today's column, and the
// accent "agora" line. Greedy lane packing (as in `EnrollmentTimetableGrid`)
// keeps overlapping blocks side by side instead of stacked.

// dc compact density: 46px per hour.
private val HourHeight = 46.dp
internal val ScheduleGridRailWidth = 40.dp

// The axis hugs the week's classes — first hour of the earliest class down to
// the last hour of the latest, no empty rows on either end. The dc mock's
// 07:00–18:00 span survives only as the fallback for a class-less week
// (which `ScheduleGridContent` never actually renders as a grid).
private const val FallbackStartHour = 7
private const val FallbackEndHour = 18

private data class GridBlock(
    val cls: ScheduleClass,
    val lane: Int,
    val lanes: Int,
)

@Composable
internal fun ScheduleGridWeek(
    days: List<List<ScheduleClass>>,
    todayIdx: Int,
    nowMinute: Int,
    onOpenClass: (Int, ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classes = days.flatten()
    val startHour = classes.minOfOrNull { it.startMin }?.div(60) ?: FallbackStartHour
    val endHour = classes.maxOfOrNull { (it.endMin + 59) / 60 } ?: FallbackEndHour
    val startMinute = startHour * 60
    val gridHeight = HourHeight * (endHour - startHour)
    val line = MaterialTheme.melon.surface.line

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 20.dp)
            .fadeInOnAppear(delayMs = 200, durationMs = 400),
    ) {
        HourRail(
            startHour = startHour,
            endHour = endHour,
            modifier = Modifier
                .width(ScheduleGridRailWidth)
                .height(gridHeight),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(gridHeight)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    for (hour in 0..(endHour - startHour)) {
                        val y = hour * HourHeight.toPx()
                        drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                },
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                days.forEachIndexed { dayIdx, dayClasses ->
                    GridDayColumn(
                        dayIdx = dayIdx,
                        classes = dayClasses,
                        todayIdx = todayIdx,
                        nowMinute = nowMinute,
                        startMinute = startMinute,
                        onOpenClass = onOpenClass,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            if (todayIdx in days.indices && nowMinute in startMinute..(endHour * 60)) {
                NowLine(
                    offsetY = HourHeight * ((nowMinute - startMinute) / 60f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HourRail(startHour: Int, endHour: Int, modifier: Modifier = Modifier) {
    val line = MaterialTheme.melon.surface.line
    Box(
        modifier = modifier.drawBehind {
            drawLine(
                color = line,
                start = Offset(size.width, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        },
    ) {
        for (hour in startHour..endHour) {
            Text(
                text = stringResource(R.string.schedule_grid_hour_format, hour),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = HourHeight * (hour - startHour) - 8.dp),
            )
        }
    }
}

@Composable
private fun GridDayColumn(
    dayIdx: Int,
    classes: List<ScheduleClass>,
    todayIdx: Int,
    nowMinute: Int,
    startMinute: Int,
    onOpenClass: (Int, ScheduleClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.melon.surface.line
    val accent = MaterialTheme.colorScheme.primary
    val dark = LocalMelonDarkTheme.current
    val isToday = dayIdx == todayIdx

    Box(
        modifier = modifier
            .then(
                if (isToday) {
                    Modifier.background(accent.copy(alpha = if (dark) 0.09f else 0.06f))
                } else {
                    Modifier
                },
            )
            .drawBehind {
                if (dayIdx > 0) {
                    drawLine(line, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                }
            },
    ) {
        packLanes(classes).forEachIndexed { eventIdx, block ->
            val top = HourHeight * ((block.cls.startMin - startMinute) / 60f)
            val height = (HourHeight * (block.cls.durationMin / 60f) - 3.dp).coerceAtLeast(18.dp)
            val laneWidth = 1f / block.lanes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = top),
            ) {
                if (block.lane > 0) Spacer(Modifier.weight(block.lane * laneWidth))
                GridClassBlock(
                    cls = block.cls,
                    state = gridClassState(dayIdx, todayIdx, block.cls, nowMinute),
                    blockHeight = height,
                    delayMs = 220 + dayIdx * 55 + eventIdx * 45,
                    onClick = { onOpenClass(dayIdx, block.cls) },
                    modifier = Modifier.weight(laneWidth),
                )
                val remaining = 1f - (block.lane + 1) * laneWidth
                if (remaining > 0f) Spacer(Modifier.weight(remaining))
            }
        }
    }
}

// Greedy first-free-lane packing, same scheme as `EnrollmentTimetableGrid` —
// a regular enrolled week has one lane per day, but sync conflicts must not
// draw blocks on top of each other.
private fun packLanes(classes: List<ScheduleClass>): List<GridBlock> {
    val sorted = classes.sortedBy { it.startMin }
    val laneEnds = mutableListOf<Int>()
    val lanes = sorted.map { cls ->
        val lane = laneEnds.indexOfFirst { cls.startMin >= it }
        if (lane >= 0) {
            laneEnds[lane] = cls.endMin
            lane
        } else {
            laneEnds += cls.endMin
            laneEnds.size - 1
        }
    }
    val laneCount = laneEnds.size.coerceAtLeast(1)
    return sorted.mapIndexed { index, cls -> GridBlock(cls, lanes[index], laneCount) }
}

@Composable
private fun GridClassBlock(
    cls: ScheduleClass,
    state: GridClassState,
    blockHeight: Dp,
    delayMs: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalMelonDarkTheme.current
    val shape = RoundedCornerShape(10.dp)
    val isNow = state == GridClassState.Now
    val onHero = MaterialTheme.melon.fixed.onHero
    val background = if (isNow) {
        cls.color
    } else {
        cls.color
            .copy(alpha = if (dark) 0.22f else 0.13f)
            .compositeOverBackground()
    }
    val foreground = if (isNow) onHero else cls.color
    val insetBar = if (isNow) onHero.copy(alpha = 0.6f) else cls.color

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(blockHeight)
            .scaleInOnAppear(
                delayMs = delayMs,
                durationMs = 420,
                fromScale = 0.86f,
                shadow = if (isNow) {
                    RevealShadow(
                        elevation = 5.dp,
                        shape = shape,
                        spotColor = cls.color,
                        ambientColor = cls.color,
                    )
                } else {
                    null
                },
            )
            .then(if (state == GridClassState.Done) Modifier.alpha(0.45f) else Modifier)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .drawBehind {
                val x = 1.5.dp.toPx()
                drawLine(insetBar, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx())
            }
            .padding(start = 9.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
    ) {
        Text(
            text = cls.code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val room = cls.room ?: cls.modulo
        if (blockHeight >= 34.dp && room != null) {
            Text(
                text = room,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = foreground.copy(alpha = foreground.alpha * if (isNow) 0.9f else 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (blockHeight >= 74.dp) {
            Column(modifier = Modifier.padding(top = 3.dp)) {
                for (time in listOf(cls.start, cls.end)) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = foreground.copy(alpha = foreground.alpha * if (isNow) 0.8f else 0.55f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun NowLine(offsetY: Dp, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        // Centers the 8dp dot (and the 2dp line) on the current minute.
        modifier = modifier
            .offset(y = offsetY - 4.dp)
            .height(8.dp)
            .fadeInOnAppear(delayMs = 620, durationMs = 400),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(2.dp)
                .background(accent),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-3).dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(accent),
        )
    }
}

// Tonal block fill = discipline hue mixed into the page background, matching
// the dc `color-mix(…)` recipe.
@Composable
private fun Color.compositeOverBackground(): Color =
    compositeOver(MaterialTheme.colorScheme.background)
