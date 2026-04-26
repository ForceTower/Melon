package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleDayColumn
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleHeader
import dev.forcetower.unes.ui.feature.schedule.components.ScheduleWeekSpine
import kotlinx.coroutines.delay

// "Horário" tab inside `ConnectedScreen`. Day-focused weekly view: header +
// horizontal week pills + the active day's class column. Mirrors iOS
// `ScheduleFocusedView` and the JSX prototype `UNES Schedule.html`. Until
// the KMP-backed view model lands, fixtures from `ScheduleFixtures` drive
// the rendering — same payload shape iOS uses, so the prototype, iOS, and
// Android stay visually identical.
@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val week = ScheduleFixtures.week()
    val dates = ScheduleFixtures.dates
    val todayIdx = ScheduleFixtures.TODAY_INDEX
    val nowMin = ScheduleFixtures.NOW_MIN
    val weekNumber = ScheduleFixtures.WEEK_NUMBER

    var activeIdx by rememberSaveable { mutableIntStateOf(todayIdx) }
    // `entering` flips false after the first staggered enter pass so subsequent
    // day swaps use the lighter horizontal slide animation on each block.
    var entering by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (entering) {
            delay(1400)
            entering = false
        }
    }

    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        AmbientMeshTop(surface = surface, modifier = Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            ScheduleHeader(
                weekNumber = weekNumber,
                weekRange = formatWeekRange(),
                showTodayButton = activeIdx != todayIdx,
                onToday = { activeIdx = todayIdx },
                entering = entering,
            )

            ScheduleWeekSpine(
                activeIdx = activeIdx,
                onChange = { activeIdx = it },
                week = week,
                dates = dates,
                todayIdx = todayIdx,
                entering = entering,
            )

            // Keying on `activeIdx` remounts the day column so its enter
            // animation re-runs on each pill tap — same trick the JSX uses
            // with `<DayColumn key={activeIdx} … />`.
            DayColumnSlot(
                key = activeIdx,
                classes = week.getOrNull(activeIdx).orEmpty(),
                isToday = activeIdx == todayIdx,
                nowMin = nowMin,
                entering = entering,
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DayColumnSlot(
    key: Int,
    classes: List<ScheduleClass>,
    isToday: Boolean,
    nowMin: Int,
    entering: Boolean,
) {
    key(key) {
        ScheduleDayColumn(
            classes = classes,
            isToday = isToday,
            nowMin = nowMin,
            showGaps = true,
            entering = entering,
        )
    }
}

@Composable
private fun AmbientMeshTop(surface: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.28f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.95f to surface,
                    ),
                ),
        )
    }
}

@Composable
private fun formatWeekRange(): String {
    val first = ScheduleFixtures.dates.first()
    val last = ScheduleFixtures.dates.last()
    val month = stringResource(R.string.schedule_month_apr_short)
    return stringResource(R.string.schedule_week_range_format, first, last, month)
}

@Preview
@Composable
private fun ScheduleScreenPreview() {
    MelonTheme { ScheduleScreen() }
}
