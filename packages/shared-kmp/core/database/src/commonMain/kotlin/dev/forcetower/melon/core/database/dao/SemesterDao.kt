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

    @Upsert
    suspend fun upsert(semester: SemesterEntity)

    @Upsert
    suspend fun upsertAll(semesters: List<SemesterEntity>)

    @Query("DELETE FROM Semester WHERE id NOT IN (:keep)")
    suspend fun deleteMissing(keep: List<String>)

    @Query("DELETE FROM Semester")
    suspend fun clear()
}
