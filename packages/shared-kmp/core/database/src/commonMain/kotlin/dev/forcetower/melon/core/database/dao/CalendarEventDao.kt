package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.AcademicCalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CalendarEventDao {
    @Query("SELECT * FROM AcademicCalendarEvent ORDER BY start ASC, id ASC")
    abstract fun observeAll(): Flow<List<AcademicCalendarEventEntity>>

    @Query("SELECT * FROM AcademicCalendarEvent WHERE start BETWEEN :start AND :end ORDER BY start ASC, id ASC")
    abstract fun observeBetween(start: String, end: String): Flow<List<AcademicCalendarEventEntity>>

    @Upsert
    abstract suspend fun upsertAll(events: List<AcademicCalendarEventEntity>)

    @Query("DELETE FROM AcademicCalendarEvent")
    abstract suspend fun clear()

    // The server emits the canonical 90-day window every sync, so a wholesale
    // replace is the simplest correct shape — it propagates upstream deletes
    // without per-row reconciliation. Wrapped in a transaction so observers
    // never see an intermediate empty state.
    @Transaction
    open suspend fun replaceAll(events: List<AcademicCalendarEventEntity>) {
        clear()
        if (events.isNotEmpty()) upsertAll(events)
    }
}
