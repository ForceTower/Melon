package dev.forcetower.melon.core.sync.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError

// Pulls server state into the local mirror. Write side only — reads go through
// the DAOs directly (or domain-specific repositories layered on them).
interface MirrorRepository {
    suspend fun syncProfile(): Outcome<Unit, SyncError>
    suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError>
    suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError>

    // Polled by SyncView's retry gates while the server's backfill worker
    // catches up. Pure read; no local writes.
    suspend fun fetchOnboardingStatus(): Outcome<OnboardingStatus, SyncError>

    // Pulls and applies one page of the inbox. Caller decides whether to
    // continue paging based on `nextCursor`.
    suspend fun syncMessages(since: String?, cursor: String?): Outcome<MessagePageResult, SyncError>

    // Pulls the canonical 90-day academic-calendar window and replaces the
    // local mirror wholesale. Returns the number of events applied.
    suspend fun syncCalendarEvents(): Outcome<Int, SyncError>

    // Bumps last_active_at on the server. Fire-and-forget; the only failure
    // anyone cares about is auth — surfaced via Outcome like everything else.
    suspend fun pingActivity(): Outcome<Unit, SyncError>

    // Mirrors the server's view of the user's upstream Snowpiercer credentials
    // into the local Credentials row. Critical for passkey-login users, who
    // never typed username + password locally — without this, background
    // re-authentication has nothing to replay. Returns Ok when the server
    // reports no credentials on file.
    suspend fun syncMyCredentials(): Outcome<Unit, SyncError>
}
