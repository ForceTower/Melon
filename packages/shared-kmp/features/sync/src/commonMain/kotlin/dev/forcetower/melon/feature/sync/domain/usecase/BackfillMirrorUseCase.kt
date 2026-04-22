package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus.PhaseStatus.State
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay

// Polls for server Phase 2 completion and mirrors historical data (every
// semester's payload + the full message archive) into the local DB. Runs
// once per authenticated session — gated by `backfillMirrorComplete` in
// SyncState, which is wiped by SessionStore.logout.
//
// The server's Phase 2 (backfill.ts) runs autonomously after Phase 1:
// enqueues one `kind='semester'` job per historical semester + one
// `kind='messages'` job for deep pagination. The iOS SyncView only
// mirrored Phase 1 (current semester + first 20 messages) before advancing
// to the connected shell. This use case fills the gap.
@Inject
class BackfillMirrorUseCase internal constructor(
    private val mirror: MirrorRepository,
    private val syncState: SyncStateRepository,
) {
    suspend operator fun invoke(): Outcome<Unit, SyncError> {
        if (syncState.getBackfillMirrorComplete()) return Outcome.Ok(Unit)

        // Wait for the server's Phase 2 to finish before pulling payloads —
        // mirroring mid-flight would produce an incomplete snapshot that
        // looks "done" to the flag.
        when (val result = awaitTerminalStatus()) {
            is Outcome.Err -> return result
            is Outcome.Ok -> Unit
        }

        val summaries = when (val result = mirror.syncSemesterList()) {
            is Outcome.Err -> return result
            is Outcome.Ok -> result.value
        }

        for (summary in summaries) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> return result
                is Outcome.Ok -> Unit
            }
        }

        when (val result = paginateMessages()) {
            is Outcome.Err -> return result
            is Outcome.Ok -> Unit
        }

        syncState.setBackfillMirrorComplete(true)
        return Outcome.Ok(Unit)
    }

    // Returns Ok once the server reports Phase 1 + Phase 2 (semesters +
    // messages) terminal, or after the hard cap elapses. Giving up on the
    // cap still returns Ok so the caller's flag-flip path can proceed —
    // next launch will re-poll, and a server stuck in `running` for 5
    // minutes is operator-visible via /sync/onboarding-status directly.
    private suspend fun awaitTerminalStatus(): Outcome<Unit, SyncError> {
        repeat(MAX_POLL_ITERATIONS) {
            when (val result = mirror.fetchOnboardingStatus()) {
                is Outcome.Err -> return result
                is Outcome.Ok -> if (isTerminal(result.value)) return Outcome.Ok(Unit)
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        return Outcome.Ok(Unit)
    }

    private suspend fun paginateMessages(): Outcome<Unit, SyncError> {
        var cursor: String? = null
        repeat(MAX_MESSAGE_PAGES) {
            val result = mirror.syncMessages(since = null, cursor = cursor)
            when (result) {
                is Outcome.Err -> return result
                is Outcome.Ok -> {
                    val next = result.value.nextCursor
                    if (next == null) return Outcome.Ok(Unit)
                    cursor = next
                }
            }
        }
        return Outcome.Ok(Unit)
    }

    // Phase 1 Failed short-circuits: Phase 2 is never enqueued, so the
    // semesters/messages fields stay Pending forever. Treat "Phase 1
    // failed" as terminal-for-everything so we don't loop to the cap on a
    // dead onboarding.
    private fun isTerminal(status: OnboardingStatus): Boolean {
        if (status.initial.state == State.Failed) return true
        if (status.initial.state != State.Done) return false
        val semTerminal = status.semesters.state == State.Done ||
            status.semesters.state == State.Partial ||
            status.semesters.state == State.Failed
        val msgTerminal = status.messages.state == State.Done ||
            status.messages.state == State.Failed
        return semTerminal && msgTerminal
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 5_000L
        const val MAX_POLL_ITERATIONS = 60
        const val MAX_MESSAGE_PAGES = 200
    }
}
