package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.forcetower.unes.core.model.Discipline

@Dao
interface DisciplineDao {
    @Insert(onConflict = REPLACE)
    fun insert(discipline: List<Discipline>)

    @Query("SELECT * FROM Discipline WHERE code = :code LIMIT 1")
    fun getDisciplineByCodeDirect(code: String): Discipline
}