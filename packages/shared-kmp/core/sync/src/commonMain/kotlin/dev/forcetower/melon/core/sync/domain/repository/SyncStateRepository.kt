package dev.forcetower.melon.core.sync.domain.repository

// Thin K/V wrapper around the SyncState table. Owns well-known keys for the
// client-side throttle logic.
interface SyncStateRepository {
    suspend fun getLastActiveSemesterPulledAt(): Long?
    suspend fun setLastActiveSemesterPulledAt(epochMillis: Long)
    suspend fun getOnboardingComplete(): Boolean
    suspend fun setOnboardingComplete(value: Boolean)
    suspend fun getBackfillMirrorComplete(): Boolean
    suspend fun setBackfillMirrorComplete(value: Boolean)
    suspend fun reset()
}
