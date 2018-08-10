package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.forcetower.unes.core.model.sagres.SagresAccess

@Dao
interface AccessDao {
    @Query("SELECT * FROM SagresAccess LIMIT 1")
    fun getAccess(): LiveData<SagresAccess>
}