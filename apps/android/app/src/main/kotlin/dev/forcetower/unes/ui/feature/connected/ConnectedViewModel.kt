package dev.forcetower.unes.ui.feature.connected

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.sync.domain.usecase.BackfillMirrorUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// Mirrors iOS `ConnectedView`'s sync orchestration: every entry into the
// authenticated shell — first launch, logout→login, background→foreground —
// fires `RefreshSession` to refresh profile + first-page messages and pull
// active-semester payloads (1h-throttled inside the use case) and `PingActivity`
// to bump `users.last_active_at` so the worker keeps the student on the hourly
// cadence tier. The once-per-session `BackfillMirror` runs on first appearance
// only; its own `backfillMirrorComplete` flag in SyncState makes subsequent
// calls cheap, but we still gate locally so re-foregrounding the app doesn't
// restart the poll-and-paginate work that's already done.
@HiltViewModel
internal class ConnectedViewModel @Inject constructor(
    private val refreshSession: RefreshSessionUseCase,
    private val backfillMirror: BackfillMirrorUseCase,
    private val pingActivity: PingActivityUseCase,
    logger: Logger,
) : ViewModel() {
    private val log = logger.withTag("ConnectedViewModel")
    private val refreshMutex = Mutex()
    private val pingMutex = Mutex()
    private var backfillStarted = false

    // Called from the screen on every `Lifecycle.Event.ON_START` — covers
    // first composition AND background → foreground transitions in one path.
    fun onAppeared(reason: String = "onStart") {
        log.i { "Firing on appear" }
        viewModelScope.launch { runRefresh(reason) }
        viewModelScope.launch { runPing(reason) }
        if (!backfillStarted) {
            backfillStarted = true
            viewModelScope.launch { runBackfill() }
        }
    }

    private suspend fun runRefresh(reason: String) {
        if (!refreshMutex.tryLock()) {
            log.i { "skip session refresh — already in flight reason=$reason" }
            return
        }
        try {
            log.i { "refreshing session reason=$reason" }
            when (val outcome = refreshSession(force = false)) {
                is Outcome.Ok -> log.i { "session refresh ok reason=$reason" }
                is Outcome.Err -> log.w { "session refresh failed reason=$reason err=${outcome.error}" }
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private suspend fun runBackfill() {
        when (val outcome = backfillMirror()) {
            is Outcome.Ok -> log.i { "mirror backfill ok" }
            is Outcome.Err -> log.w { "mirror backfill failed err=${outcome.error}" }
        }
    }

    private suspend fun runPing(reason: String) {
        if (!pingMutex.tryLock()) {
            log.i { "skip ping — already in flight reason=$reason" }
            return
        }
        try {
            when (val outcome = pingActivity()) {
                is Outcome.Ok -> log.i { "ping ok reason=$reason" }
                is Outcome.Err -> log.w { "ping failed reason=$reason err=${outcome.error}" }
            }
        } finally {
            pingMutex.unlock()
        }
    }
}
