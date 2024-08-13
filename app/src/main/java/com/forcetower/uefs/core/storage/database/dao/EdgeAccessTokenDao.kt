package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.forcetower.core.database.BaseDao
import com.forcetower.uefs.core.model.unes.EdgeAccessToken

@Dao
abstract class EdgeAccessTokenDao : BaseDao<EdgeAccessToken>() {
    @Query("SELECT * FROM EdgeAccessToken LIMIT 1")
    abstract suspend fun require(): EdgeAccessToken?

    @Query("DELETE FROM EdgeAccessToken")
    abstract suspend fun deleteAll()
}