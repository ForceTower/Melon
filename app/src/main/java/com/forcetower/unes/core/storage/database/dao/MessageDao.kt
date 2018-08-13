package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.forcetower.unes.core.model.Message

@Dao
interface MessageDao {
    @Insert(onConflict = REPLACE)
    fun insert(messages: List<Message>)

    @Query("SELECT * FROM Message")
    fun getAllMessages(): LiveData<List<Message>>
}