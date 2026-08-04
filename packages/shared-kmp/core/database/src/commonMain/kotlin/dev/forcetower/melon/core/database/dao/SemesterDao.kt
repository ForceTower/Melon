package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.SemesterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM Semester ORDER BY startDate DESC")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM Semester ORDER BY startDate DESC")
    suspend fun listAll(): List<SemesterEntity>

    @Query("SELECT * FROM Semester WHERE id = :id")
    suspend fun get(id: String): SemesterEntity?

    // Already-mirrored semesters (their offer subtree exists locally) whose
    // server `dirtyAt` moved past the one last applied — including rows
    // mirrored before the stamp existed. Never-synced semesters are excluded:
    // the backfill owns those.
    @Query(
        """
        SELECT s.id FROM Semester s
         WHERE s.dirtyAt IS NOT NULL
           AND (s.appliedDirtyAt IS NULL OR s.appliedDirtyAt != s.dirtyAt)
           AND EXISTS (SELECT 1 FROM DisciplineOffer o WHERE o.semesterId = s.id)
        """,
    )
    suspend fun listStaleMirroredIds(): List<String>

    @Upsert
    suspend fun upsert(semester: SemesterEntity)

    @Upsert
    suspend fun upsertAll(semesters: List<SemesterEntity>)

    @Query("DELETE FROM Semester WHERE id NOT IN (:keep)")
    suspend fun deleteMissing(keep: List<String>)

    @Query("DELETE FROM Semester")
    suspend fun clear()
}
