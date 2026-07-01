package dev.forcetower.melon.core.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// Emits the current LocalDateTime immediately, then every `periodMs`, and again
// whenever `resume` fires. Time-derived flows combine with this so derived
// state (today's classes, "starts in", days-until) refreshes on a tick even
// when the DB hasn't changed — and, via `resume`, the instant the app returns
// to the foreground rather than waiting on the delay timer, which doesn't
// reliably fire after a long OS suspension. See [ForegroundSignal].
fun tickerFlow(periodMs: Long, resume: Flow<Unit>): Flow<LocalDateTime> = merge(
    flow {
        while (true) {
            emit(nowLocalDateTime())
            delay(periodMs)
        }
    },
    resume.map { nowLocalDateTime() },
)

private fun nowLocalDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
