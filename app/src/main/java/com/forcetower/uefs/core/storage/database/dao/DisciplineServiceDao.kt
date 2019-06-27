package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.forcetower.uefs.core.model.unes.SDiscipline

@Dao
interface DisciplineServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(values: List<SDiscipline>)
}
