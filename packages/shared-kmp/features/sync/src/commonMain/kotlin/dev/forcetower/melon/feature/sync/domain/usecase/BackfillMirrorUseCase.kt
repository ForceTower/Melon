package dev.forcetower.melon.feature.sync.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus.PhaseStatus.State
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.forcetower.melon.core.sync.domain.repository.SyncStateRepository
import dev.zacsweers.metro.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay

// Polls for server Phase 2 completion and mirrors historical data (every
// semester's payload + the full message archive) into the local DB. Runs
// until it completes once per authenticated session — gated by
// `backfillMirrorComplete` in SyncState, which is wiped by SessionStore.logout.
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
    logger: Logger,
) {
    private val log = logger.withTag("BackfillMirrorUseCase")

    suspend operator fun invoke(): Outcome<Unit, SyncError> {
        if (syncState.getBackfillMirrorComplete()) {
            log.d { "backfill skipped: already complete" }
            return Outcome.Ok(Unit)
        }
        log.i { "backfill start" }

        // Wait for the server's Phase 2 to finish before pulling payloads.
        // Only a settled Phase 2 may flip the flag below — a mid-flight
        // snapshot is mirrored but left incomplete, so the next appearance
        // re-polls and pulls the rest.
        val phaseTwo = when (val result = awaitPhaseTwo()) {
            is Outcome.Err -> {
                log.w { "backfill aborted waiting for terminal onboarding status err=${result.error}" }
                return result
            }
            is Outcome.Ok -> result.value
        }

        // Backfill the local Credentials row from server state. Critical for
        // passkey-login users who never typed username + password locally.
        // Failure is logged but not fatal — password-login users already have
        // credentials cached locally, and a missing creds row only degrades
        // background re-auth (the foreground session keeps working).
        when (val result = mirror.syncMyCredentials()) {
            is Outcome.Err -> log.w { "backfill credentials sync failed err=${result.error} (continuing)" }
            is Outcome.Ok -> Unit
        }

        val summaries = when (val result = mirror.syncSemesterList()) {
            is Outcome.Err -> {
                log.w { "backfill aborted at semester list err=${result.error}" }
                return result
            }
            is Outcome.Ok -> result.value
        }

        log.i { "backfill mirroring semesters count=${summaries.size}" }
        for (summary in summaries) {
            when (val result = mirror.syncSemester(summary.id)) {
                is Outcome.Err -> {
                    log.w { "backfill failed on semester id=${summary.id} err=${result.error}" }
                    return result
                }
                is Outcome.Ok -> Unit
            }
        }

        try {
            when (val result = paginateMessages()) {
                is Outcome.Err -> {
                    log.w { "backfill failed paginating messages err=${result.error}" }
                    return result
                }

                is Outcome.Ok -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log.e(e) { "backfill message pagination threw" }
            return Outcome.Err(SyncError.Unexpected)
        }

        if (phaseTwo != PhaseTwo.Settled) {
            log.w { "backfill mirrored but left incomplete phaseTwo=$phaseTwo" }
            return Outcome.Ok(Unit)
        }
        syncState.setBackfillMirrorComplete(true)
        log.i { "backfill complete" }
        return Outcome.Ok(Unit)
    }

    private enum class PhaseTwo { Settled, NeverStarted, StillRunning }

    // Polls until Phase 2 settles or Phase 1 fails (Phase 2 is then never
    // enqueued), or gives up at the hard cap with StillRunning.
    private suspend fun awaitPhaseTwo(): Outcome<PhaseTwo, SyncError> {
        repeat(MAX_POLL_ITERATIONS) {
            when (val result = mirror.fetchOnboardingStatus()) {
                is Outcome.Err -> return result
                is Outcome.Ok -> stoppedPhaseTwo(result.value)?.let { return Outcome.Ok(it) }
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        log.w { "backfill polling hit cap without terminal status" }
        return Outcome.Ok(PhaseTwo.StillRunning)
    }

    private suspend fun paginateMessages(): Outcome<Unit, SyncError> {
        var cursor: String? = null
        var pages = 0
        repeat(MAX_MESSAGE_PAGES) {
            val result = mirror.syncMessages(since = null, cursor = cursor)
            when (result) {
                is Outcome.Err -> return result
                is Outcome.Ok -> {
                    pages += 1
                    val next = result.value.nextCursor
                    if (next == null) {
                        log.i { "backfill messages pagination done pages=$pages" }
                        return Outcome.Ok(Unit)
                    }
                    cursor = next
                }
            }
        }
        log.w { "backfill messages pagination hit page cap=$MAX_MESSAGE_PAGES" }
        return Outcome.Ok(Unit)
    }

    // Null while polling should continue. A failed Phase 1 stops the poll —
    // the semesters/messages fields would stay Pending forever — but a later
    // server retry may still run Phase 2, so it never counts as settled.
    // Unknown (a state this client doesn't recognise) is never terminal.
    private fun stoppedPhaseTwo(status: OnboardingStatus): PhaseTwo? {
        if (status.initial.state == State.Failed) return PhaseTwo.NeverStarted
        if (status.initial.state != State.Done) return null
        val semTerminal = status.semesters.state == State.Done ||
            status.semesters.state == State.Partial ||
            status.semesters.state == State.Failed
        val msgTerminal = status.messages.state == State.Done ||
            status.messages.state == State.Failed
        return if (semTerminal && msgTerminal) PhaseTwo.Settled else null
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 5_000L
        const val MAX_POLL_ITERATIONS = 60
        const val MAX_MESSAGE_PAGES = 200
    }
}
