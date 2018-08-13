package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.unes.core.model.CalendarItem

@Dao
abstract class CalendarDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(items: List<CalendarItem>)

    @Transaction
    open fun deleteAndInsert(items: List<CalendarItem>?) {
        if (items != null && items.isNotEmpty()) {
            delete()
            insert(items)
        }
    }

    @Query("DELETE FROM CalendarItem")
    abstract fun delete()
}