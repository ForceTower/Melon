package com.forcetower.unes.core.storage.database.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.unes.core.model.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoring(messages: List<Message>)

    @Query("SELECT * FROM Message ORDER BY timestamp DESC")
    fun getAllMessages(): DataSource.Factory<Int, Message>
}