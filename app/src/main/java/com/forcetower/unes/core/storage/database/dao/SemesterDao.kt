package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import com.forcetower.unes.core.model.Semester

@Dao
interface SemesterDao {
    @Insert(onConflict = REPLACE)
    fun insert(semesters: List<Semester>)
}