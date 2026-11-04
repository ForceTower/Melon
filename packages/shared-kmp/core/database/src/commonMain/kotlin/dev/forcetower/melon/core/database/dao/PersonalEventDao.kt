package dev.forcetower.melon.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.forcetower.melon.core.database.entity.PersonalEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalEventDao {
    @Query("SELECT * FROM PersonalEvent ORDER BY start ASC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<PersonalEventEntity>>

    // One-shot read for the reminder snapshot, which runs outside composition.
    @Query("SELECT * FROM PersonalEvent ORDER BY start ASC, createdAt ASC, id ASC")
    suspend fun all(): List<PersonalEventEntity>

    // A discipline's checklist: pending entries by due date, completed ones
    // after them.
    @Query(
        """
        SELECT * FROM PersonalEvent WHERE disciplineId = :disciplineId
        ORDER BY completedAt IS NOT NULL, start ASC, createdAt ASC, id ASC
        """,
    )
    fun observeByDiscipline(disciplineId: String): Flow<List<PersonalEventEntity>>

    // The composer always sends the whole entry — an edit upserts over its
    // own id.
    @Upsert
    suspend fun upsert(event: PersonalEventEntity)

    // The checklist toggle touches only this column, so it can't clobber an
    // edit made elsewhere.
    @Query("UPDATE PersonalEvent SET completedAt = :completedAt WHERE id = :id")
    suspend fun setCompletedAt(
        id: String,
        completedAt: Long?,
    )

    @Query("DELETE FROM PersonalEvent WHERE id = :id")
    suspend fun deleteById(id: String)

    // Sign-out only: the next account on this device must not inherit them.
    @Query("DELETE FROM PersonalEvent")
    suspend fun clear()
}
