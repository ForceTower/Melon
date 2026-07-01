package dev.forcetower.melon.core.common

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// A process-wide pulse that fires when the app returns to the foreground. The
// native shell calls `pulse()` from its foreground lifecycle. Time-derived
// flows merge this into their ticker (see `tickerFlow`) so wall-clock state —
// today's classes, "starts in", days-until — recomputes the instant the app
// resumes, instead of showing the day the app was backgrounded until the next
// delay tick fires (which is unreliable after a long OS suspension).
interface ForegroundSignal {
    val pulses: SharedFlow<Unit>
    fun pulse()
}

internal class ForegroundSignalImpl : ForegroundSignal {
    // replay = 0: a pulse only matters to flows currently collecting. A screen
    // subscribed after resume already gets a fresh tick from tickerFlow's own
    // immediate emit, so it doesn't need the missed pulse replayed.
    private val _pulses = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val pulses: SharedFlow<Unit> = _pulses.asSharedFlow()

    override fun pulse() {
        _pulses.tryEmit(Unit)
    }
}

@ContributesTo(AppScope::class)
interface ForegroundSignalGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun foregroundSignal(): ForegroundSignal = ForegroundSignalImpl()
    }
}
