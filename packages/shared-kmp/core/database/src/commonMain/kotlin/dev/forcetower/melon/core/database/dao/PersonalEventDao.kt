package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.PersonalEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalEventDao {
    @Query("SELECT * FROM PersonalEvent ORDER BY start ASC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<PersonalEventEntity>>

    // One-shot read for the reminder snapshot, which runs outside composition.
    @Query("SELECT * FROM PersonalEvent ORDER BY start ASC, createdAt ASC, id ASC")
    suspend fun all(): List<PersonalEventEntity>

    // The composer always sends the whole entry, so there is no partial-update
    // path — an edit upserts over its own id.
    @Upsert
    suspend fun upsert(event: PersonalEventEntity)

    @Query("DELETE FROM PersonalEvent WHERE id = :id")
    suspend fun deleteById(id: String)

    // Sign-out only: the next account on this device must not inherit them.
    @Query("DELETE FROM PersonalEvent")
    suspend fun clear()
}
