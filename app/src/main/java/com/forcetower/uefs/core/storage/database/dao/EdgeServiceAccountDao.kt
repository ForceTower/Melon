package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.forcetower.core.database.BaseDao
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EdgeServiceAccountDao : BaseDao<EdgeServiceAccount>() {
    @Query("SELECT * FROM EdgeServiceAccount WHERE me = 1 LIMIT 1")
    abstract fun me(): Flow<EdgeServiceAccount>
}
