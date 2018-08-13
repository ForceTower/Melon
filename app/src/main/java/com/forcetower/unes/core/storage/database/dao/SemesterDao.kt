package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.forcetower.unes.core.model.Semester

@Dao
interface SemesterDao {
    @Insert(onConflict = IGNORE)
    fun insertIgnoring(semesters: List<Semester>)
}