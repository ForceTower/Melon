package dev.forcetower.unes.ui.feature.schedule.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.BlinkingFolio
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import kotlinx.coroutines.withTimeoutOrNull

// Free-day state (dc `UNES Horário - Android`, with the Folio mascot kept in
// place of the spec's bedtime glyph). Long-pressing the mascot opens the
// Folio runner — held distinctly longer than the system long-press (~500ms)
// so an accidental hold won't open the easter egg; discovery should feel
// like a deliberate secret. Mirrors iOS `DayColumn`.
@Composable
internal fun ScheduleEmptyDay(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.outline
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 60.dp)
            .fadeUpOnAppear(delayMs = 60, durationMs = 500, fromOffset = 14.dp)
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
    ) {
        BlinkingFolio(
            size = 96.dp,
            modifier = Modifier
                .alpha(0.85f)
                .padding(bottom = 16.dp),
        )
        Text(
            text = stringResource(R.string.schedule_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = ink,
        )
        Text(
            text = stringResource(R.string.schedule_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 220.dp),
        )
    }
}

// Held distinctly longer than the system long-press (~500ms) so an accidental
// hold won't trip the easter egg. Matches iOS `onLongPressGesture(minimumDuration: 1.2)`.
private const val LongPressMillis = 1_200L
