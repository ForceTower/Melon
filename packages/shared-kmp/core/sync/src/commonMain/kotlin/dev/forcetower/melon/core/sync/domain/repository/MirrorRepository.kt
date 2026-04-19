package dev.forcetower.melon.core.sync.domain.repository

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SemesterSummary
import dev.forcetower.melon.core.sync.domain.model.SyncError

// Pulls server state into the local mirror. Write side only — reads go through
// the DAOs directly (or domain-specific repositories layered on them).
interface MirrorRepository {
    suspend fun syncProfile(): Outcome<Unit, SyncError>
    suspend fun syncSemesterList(): Outcome<List<SemesterSummary>, SyncError>
    suspend fun syncSemester(semesterId: String): Outcome<Unit, SyncError>
}
