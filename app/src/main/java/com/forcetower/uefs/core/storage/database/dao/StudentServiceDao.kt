package com.forcetower.uefs.core.storage.database.dao

import androidx.room.Dao
import com.forcetower.uefs.core.model.unes.SStudent

@Dao
interface StudentServiceDao {
    fun insert(values: List<SStudent>)
}
