package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentFormat
import dev.forcetower.unes.ui.feature.enrollment.ResolvedPick
import dev.forcetower.unes.ui.feature.enrollment.allSlots
import dev.forcetower.unes.ui.feature.enrollment.hasSchedule
import dev.forcetower.unes.ui.feature.enrollment.slotMinutes

// Weekly proposal grid (dc `MatriculaScreen` timetable view): Mon–Sat columns
// on a 07:00–23:00 axis, greedy lane packing for same-day overlaps, conflict
// blocks flagged red. Custom-drawn — no Compose primitive models this.

private const val StartMinute = 7 * 60
private const val EndMinute = 23 * 60
private val GridHeight = 520.dp
private val Days = 1..6

private data class GridBlock(
    val code: String,
    val day: Int,
    val start: Int,
    val end: Int,
    val startLabel: String,
    val hue: Color,
    val disciplineId: Long,
    var conflict: Boolean = false,
    var lane: Int = 0,
    var lanes: Int = 1,
)

@Composable
internal fun EnrollmentTimetableGrid(
    picks: List<ResolvedPick>,
    hueFor: (String) -> Color,
    modifier: Modifier = Modifier,
) {
    val blocks = buildBlocks(picks, hueFor)
    val shape = RoundedCornerShape(22.dp)
    val line = MaterialTheme.melon.surface.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, line, shape)
            .padding(start = 4.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Box(modifier = Modifier.width(28.dp))
            Days.forEach { day ->
                Text(
                    text = EnrollmentFormat.dayShort(day),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(GridHeight)) {
            HourRail(modifier = Modifier.width(28.dp).fillMaxSize())
            Days.forEach { day ->
                DayColumn(
                    blocks = blocks.filter { it.day == day },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
            }
        }
    }
}

private fun buildBlocks(picks: List<ResolvedPick>, hueFor: (String) -> Color): List<GridBlock> {
    val blocks = picks
        .filter { it.section.hasSchedule }
        .flatMap { pick ->
            pick.section.allSlots
                .filter { it.day in Days }
                .map { slot ->
                    GridBlock(
                        code = pick.discipline.code,
                        day = slot.day,
                        start = slotMinutes(slot.start),
                        end = slotMinutes(slot.end),
                        startLabel = EnrollmentFormat.slotTime(slot.start),
                        hue = hueFor(pick.discipline.code),
                        disciplineId = pick.discipline.id,
                    )
                }
        }
    // Conflict marks: overlapping blocks of *different* disciplines.
    for (a in blocks) {
        for (b in blocks) {
            if (a !== b && a.day == b.day && a.disciplineId != b.disciplineId &&
                a.start < b.end && b.start < a.end
            ) {
                a.conflict = true
                b.conflict = true
            }
        }
    }
    // Greedy first-free-lane packing per day so overlaps render side-by-side.
    Days.forEach { day ->
        val sorted = blocks.filter { it.day == day }.sortedBy { it.start }
        val laneEnds = mutableListOf<Int>()
        sorted.forEach { block ->
            val lane = laneEnds.indexOfFirst { block.start >= it }
            if (lane >= 0) {
                block.lane = lane
                laneEnds[lane] = block.end
            } else {
                block.lane = laneEnds.size
                laneEnds += block.end
            }
        }
        val lanes = laneEnds.size.coerceAtLeast(1)
        sorted.forEach { it.lanes = lanes }
    }
    return blocks
}

@Composable
private fun HourRail(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        for (hour in 7..23 step 2) {
            val fraction = (hour * 60 - StartMinute).toFloat() / (EndMinute - StartMinute)
            Text(
                text = hour.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = GridHeight * fraction - 5.dp),
            )
        }
    }
}

@Composable
private fun DayColumn(blocks: List<GridBlock>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.melon.surface.line
    val bad = MaterialTheme.melon.status.bad
    Box(
        modifier = modifier.drawBehind {
            drawLine(
                color = line,
                start = Offset.Zero,
                end = Offset(0f, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            for (hour in 7..23 step 2) {
                val y = size.height * (hour * 60 - StartMinute).toFloat() / (EndMinute - StartMinute)
                drawLine(
                    color = line.copy(alpha = line.alpha * 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        },
    ) {
        val span = (EndMinute - StartMinute).toFloat()
        blocks.forEach { block ->
            val top = GridHeight * ((block.start - StartMinute) / span)
            val height = (GridHeight * ((block.end - block.start) / span) - 2.dp).coerceAtLeast(16.dp)
            val laneWidth = 1f / block.lanes
            val hue = if (block.conflict) bad else block.hue
            Row(modifier = Modifier.fillMaxWidth().offset(y = top + 1.dp)) {
                if (block.lane > 0) Box(modifier = Modifier.weight(block.lane * laneWidth))
                Box(
                    modifier = Modifier
                        .weight(laneWidth)
                        .padding(horizontal = 1.dp)
                        .height(height)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (block.conflict) bad.copy(alpha = 0.16f) else hue.copy(alpha = 0.22f))
                        .then(
                            if (block.conflict) Modifier.border(1.5.dp, bad, RoundedCornerShape(7.dp)) else Modifier,
                        )
                        .drawBehind {
                            drawLine(
                                color = hue,
                                start = Offset(1.25.dp.toPx(), 0f),
                                end = Offset(1.25.dp.toPx(), size.height),
                                strokeWidth = 2.5.dp.toPx(),
                            )
                        }
                        .padding(start = 5.dp, top = 3.dp, end = 3.dp),
                ) {
                    Column {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = hue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (height > 30.dp) {
                            Text(
                                text = block.startLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.5.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = hue.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
                val remaining = 1f - (block.lane + 1) * laneWidth
                if (remaining > 0f) Box(modifier = Modifier.weight(remaining))
            }
        }
    }
}

// Legend chip strip under the grid — one per picked section.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EnrollmentTimetableLegend(
    picks: List<ResolvedPick>,
    hueFor: (String) -> Color,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.melon.surface.line
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        picks.forEach { pick ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.melon.surface.card)
                    .border(1.dp, line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(9.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(hueFor(pick.discipline.code)),
                )
                Text(
                    text = pick.discipline.code,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = pick.section.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
