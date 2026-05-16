package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.forcetower.melon.core.database.entity.PendingMutationEntity

@Dao
interface PendingMutationDao {
    // Oldest-first drain order preserves the per-device mutation sequence.
    @Query("SELECT * FROM PendingMutation ORDER BY createdAt ASC")
    suspend fun listPending(): List<PendingMutationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(mutation: PendingMutationEntity)

    @Query(
        "UPDATE PendingMutation SET attempts = :attempts, lastError = :error, lastAttemptedAt = :attemptedAt WHERE id = :id",
    )
    suspend fun markAttempt(id: String, attempts: Int, error: String?, attemptedAt: String)

    @Query("DELETE FROM PendingMutation WHERE id = :id")
    suspend fun delete(id: String)

    // Read-your-writes guard: callers consult this before upserting a row from
    // incoming sync data, to avoid clobbering a locally-committed mutation the
    // server hasn't seen yet. Matches on a JSON-extracted field in payloadJson.
    @Query("SELECT EXISTS(SELECT 1 FROM PendingMutation WHERE kind = :kind AND payloadJson LIKE '%\"' || :targetKey || '\":\"' || :targetValue || '\"%')")
    suspend fun hasPendingForTarget(kind: String, targetKey: String, targetValue: String): Boolean

    @Query("DELETE FROM PendingMutation")
    suspend fun clear()
}
