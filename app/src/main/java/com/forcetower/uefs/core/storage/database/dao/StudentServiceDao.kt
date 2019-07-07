package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.SStudent

@Dao
interface StudentServiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(values: List<SStudent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSingle(value: SStudent)

    @Query("SELECT * FROM SStudent WHERE id = :profileId")
    fun getProfileById(profileId: Long): LiveData<SStudent>

    @Query("SELECT * FROM SStudent WHERE me = 1")
    fun getMeStudent(): LiveData<SStudent>
}
