package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.forcetower.core.database.BaseDao
import com.forcetower.uefs.core.model.unes.EdgeAppMessage
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EdgeAppMessageDao : BaseDao<EdgeAppMessage>() {
    @Query("SELECT * FROM EdgeAppMessage ORDER BY createdAt DESC")
    abstract fun getAll(): Flow<List<EdgeAppMessage>>

    @Query("DELETE FROM EdgeAppMessage")
    abstract suspend fun deleteAll()
}