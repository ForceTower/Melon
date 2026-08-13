package dev.forcetower.unes.firebase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber

// Spec 0008's Android push-refresh funnel: every push that certifies upstream
// data changed (foreground receipt, notification tap) lands here, and bursts
// collapse into a single session refresh. Mirrors iOS `CancelID.pushRefresh`'s
// 2s debounce plus the `BackgroundSyncLatch` single-flight.
//
// A request arriving while a refresh is in flight is not lost: the debounced
// value waits for the sequential collector, so one more refresh follows the
// current one. Overlap with `ConnectedViewModel`'s ON_START refresh is
// accepted — both are full pulls and the upserts are idempotent.
@Singleton
internal class PushSyncCoordinator internal constructor(
    scope: CoroutineScope,
    private val hasSession: suspend () -> Boolean,
    private val refresh: suspend () -> Outcome<Unit, SyncError>,
) {
    @Inject
    constructor(
        @ApplicationScope scope: CoroutineScope,
        sessionStore: SessionStore,
        refreshSession: RefreshSessionUseCase,
    ) : this(
        scope = scope,
        hasSession = { sessionStore.getAccessToken() != null },
        refresh = { refreshSession() },
    )

    // replay = 1 so a request emitted before the collector below has started
    // (first push right after process start) is not dropped.
    private val requests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            @OptIn(FlowPreview::class)
            requests.debounce(DEBOUNCE).collect { runRefresh() }
        }
    }

    fun request() {
        Timber.tag(TAG).i("push refresh requested")
        requests.tryEmit(Unit)
    }

    private suspend fun runRefresh() {
        try {
            if (!hasSession()) {
                Timber.tag(TAG).i("push refresh skipped — no session")
                return
            }
            Timber.tag(TAG).i("push refresh start")
            when (val outcome = refresh()) {
                is Outcome.Ok -> Timber.tag(TAG).i("push refresh ok")
                is Outcome.Err -> Timber.tag(TAG).w("push refresh failed err=%s", outcome.error)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (ex: Throwable) {
            // Swallow so one failure can't kill the collector for the rest of
            // the process lifetime.
            Timber.tag(TAG).w(ex, "push refresh crashed")
        }
    }

    private companion object {
        val DEBOUNCE = 2.seconds
        const val TAG = "PushSyncCoordinator"
    }
}
