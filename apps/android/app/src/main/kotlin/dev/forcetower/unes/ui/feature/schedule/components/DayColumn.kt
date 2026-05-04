package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.BlinkingFolio
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.scheduleStateFor
import dev.forcetower.unes.ui.feature.schedule.startMin
import dev.forcetower.unes.ui.feature.schedule.endMin

// Expanded class list for one weekday — gap markers between sessions and an
// empty "free day" state when the day has no classes. The active day is
// keyed on `dayIdx` in the parent so swapping days remounts this composable
// and re-runs the staggered enter animation.
@Composable
internal fun ScheduleDayColumn(
    classes: List<ScheduleClass>,
    isToday: Boolean,
    nowMin: Int,
    showGaps: Boolean,
    entering: Boolean,
    modifier: Modifier = Modifier,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onOpenFolioRunner: () -> Unit = {},
) {
    if (classes.isEmpty()) {
        EmptyDay(modifier = modifier, onLongPress = onOpenFolioRunner)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 0.dp),
    ) {
        classes.forEachIndexed { index, cls ->
            val prev = classes.getOrNull(index - 1)
            val gapMin = if (prev != null) cls.startMin - prev.endMin else 0
            val baseDelayMs = if (entering) 320 + index * 80 else index * 40
            ScheduleClassBlock(
                cls = cls,
                state = scheduleStateFor(cls, isToday = isToday, nowMin = nowMin),
                showGap = showGaps && index > 0,
                gapMin = gapMin,
                onClick = if (cls.offerId != null) {
                    { onOpenDiscipline(cls) }
                } else null,
                modifier = Modifier.fadeUpOnAppear(
                    delayMs = baseDelayMs,
                    durationMs = 450,
                    fromOffset = 10.dp,
                ),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

// Easter-egg entry point: long-pressing the empty-state mascot opens the Folio
// runner. Held distinctly longer than the system long-press (~500ms) so an
// accidental hold won't open the easter egg — discovery should feel like a
// deliberate secret. Mirrors iOS `DayColumn`.
@Composable
private fun EmptyDay(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 60.dp)
            .pointerInput(onLongPress) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val released = withTimeoutOrNull(LongPressMillis) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: return@withTimeoutOrNull true
                            if (!change.pressed) return@withTimeoutOrNull true
                        }
                        @Suppress("UNREACHABLE_CODE") true
                    }
                    if (released == null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BlinkingFolio(
            size = 96.dp,
            modifier = Modifier
                .alpha(0.85f)
                .padding(bottom = 6.dp),
        )
        Text(
            text = stringResource(R.string.schedule_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                letterSpacing = (-0.2).sp,
            ),
            color = ink3,
        )
        Text(
            text = stringResource(R.string.schedule_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = ink4,
        )
    }
}

// Held distinctly longer than the system long-press (~500ms) so an accidental
// hold won't trip the easter egg. Matches iOS `onLongPressGesture(minimumDuration: 1.2)`.
private const val LongPressMillis = 1_200L
