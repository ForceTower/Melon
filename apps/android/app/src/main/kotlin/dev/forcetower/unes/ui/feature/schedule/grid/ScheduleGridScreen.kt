package dev.forcetower.unes.ui.feature.schedule.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.ScheduleFixtures
import dev.forcetower.unes.ui.feature.schedule.ScheduleUiState
import dev.forcetower.unes.ui.feature.schedule.ScheduleViewModel
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleEmptyDay
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleHeader
import dev.forcetower.unes.ui.feature.schedule.endMin
import dev.forcetower.unes.ui.feature.schedule.formatWeekRange
import dev.forcetower.unes.ui.feature.schedule.mapWeek
import dev.forcetower.unes.ui.feature.schedule.startMin
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// "Horário" tab week-grid rendering (dc project `UNES Grade - Android`,
// compact density): the same pinned M3 app bar as the timeline screen, then a
// pinned weekday header, an hour-railed grid of tonal per-discipline blocks
// with the accent "agora" line, and the week's agenda in list form below the
// grid. Tapping a block or an agenda row opens the M3 detail bottom sheet.
// Users pick this rendering over `ScheduleScreen` in Configurações;
// `ScheduleRoute` does the swap.
@Composable
internal fun ScheduleGridScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onOpenFolioRunner: () -> Unit = {},
) {
    val vm: ScheduleViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.melon.palette
    val week = remember(state.raw, palette) { mapWeek(state.raw, palette) }
    // Ticks each minute so the now line and the live block track the clock.
    val nowMinute by produceState(initialValue = minuteOfDayNow()) {
        while (true) {
            delay(60.seconds)
            value = minuteOfDayNow()
        }
    }

    ScheduleGridContent(
        state = state,
        week = week,
        nowMinute = nowMinute,
        onOpenDiscipline = { cls -> vm.trackOpenDiscipline(cls); onOpenDiscipline(cls) },
        onOpenFolioRunner = onOpenFolioRunner,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

// Where a class sits relative to the current minute — drives the tonal (
// future), filled (now), and dimmed (done) block/row treatments. Classes on
// other days always render as future.
internal enum class GridClassState { Future, Now, Done }

internal fun gridClassState(
    dayIdx: Int,
    todayIdx: Int,
    cls: ScheduleClass,
    nowMinute: Int,
): GridClassState = when {
    dayIdx != todayIdx -> GridClassState.Future
    nowMinute >= cls.endMin -> GridClassState.Done
    nowMinute >= cls.startMin -> GridClassState.Now
    else -> GridClassState.Future
}

// The class a grid block / agenda row tap hands to the sheet, plus its day so
// the sheet can caption "Quinta · 17 abr".
internal data class GridSheetTarget(val dayIdx: Int, val cls: ScheduleClass)

@Composable
private fun ScheduleGridContent(
    state: ScheduleUiState,
    week: List<List<ScheduleClass>>,
    nowMinute: Int,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onOpenFolioRunner: () -> Unit = {},
) {
    // Weekends join the grid only when they actually hold class.
    val dayCount = when {
        week[6].isNotEmpty() -> 7
        week[5].isNotEmpty() -> 6
        else -> 5
    }
    val days = week.take(dayCount)
    var sheetTarget by remember { mutableStateOf<GridSheetTarget?>(null) }

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        ScheduleHeader(
            weekNumber = state.weekNumber,
            weekRange = formatWeekRange(state.firstIso, state.lastIso),
            modifier = Modifier.fadeUpOnAppear(
                delayMs = 80,
                durationMs = 500,
                fromOffset = (-10).dp,
            ),
        )
        GridWeekdayHeader(
            dayCount = dayCount,
            todayIdx = state.todayIdx,
            dates = state.dates,
            modifier = Modifier.fadeUpOnAppear(delayMs = 140, durationMs = 500, fromOffset = (-8).dp),
        )
        PinnedHeaderHairline(scrolled = scrolled)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 32.dp),
        ) {
            if (days.all { it.isEmpty() }) {
                // A fully free week keeps the Folio mascot (and its runner
                // easter egg) instead of an empty ruled grid.
                ScheduleEmptyDay(onLongPress = onOpenFolioRunner)
            } else {
                ScheduleGridWeek(
                    days = days,
                    todayIdx = state.todayIdx,
                    nowMinute = nowMinute,
                    onOpenClass = { dayIdx, cls -> sheetTarget = GridSheetTarget(dayIdx, cls) },
                )
                ScheduleGridAgenda(
                    days = days,
                    dateIsos = state.dateIsos,
                    todayIdx = state.todayIdx,
                    nowMinute = nowMinute,
                    onOpenClass = { dayIdx, cls -> sheetTarget = GridSheetTarget(dayIdx, cls) },
                )
            }
        }
    }

    sheetTarget?.let { target ->
        ScheduleGridSheet(
            target = target,
            state = gridClassState(target.dayIdx, state.todayIdx, target.cls, nowMinute),
            dateIso = state.dateIsos.getOrNull(target.dayIdx),
            onOpenDiscipline = onOpenDiscipline,
            onDismiss = { sheetTarget = null },
        )
    }
}

// Pinned weekday rail: three-letter weekday over the date circle; today gets
// the accent-filled circle with its soft glow, everyone else stays quiet.
@Composable
private fun GridWeekdayHeader(
    dayCount: Int,
    todayIdx: Int,
    dates: List<Int>,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val letters = remember {
        DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .replace(".", "")
                .uppercase(Locale.getDefault())
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
    ) {
        Spacer(Modifier.width(ScheduleGridRailWidth))
        for (i in 0 until dayCount) {
            val isToday = i == todayIdx
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = letters.getOrNull(i).orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = if (isToday) accent else MaterialTheme.colorScheme.outlineVariant,
                )
                Box(
                    modifier = Modifier
                        .then(
                            if (isToday) {
                                Modifier.shadow(
                                    elevation = 5.dp,
                                    shape = CircleShape,
                                    ambientColor = accent,
                                    spotColor = accent,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isToday) accent else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (dates.getOrNull(i) ?: 0).toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

private fun minuteOfDayNow(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

@Preview
@Composable
private fun ScheduleGridScreenPreview() {
    MelonTheme {
        val palette = MaterialTheme.melon.palette
        val raw = ScheduleFixtures.kmpWeek()
        ScheduleGridContent(
            state = ScheduleUiState(raw = raw, gridEnabled = true),
            week = mapWeek(raw, palette),
            // Mid-morning on the fixture's Thursday, so the preview shows a
            // live block, a done block, and the now line at once.
            nowMinute = 10 * 60 + 45,
        )
    }
}
