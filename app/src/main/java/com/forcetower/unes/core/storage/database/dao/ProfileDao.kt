package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import com.forcetower.unes.core.model.Profile

@Dao
interface ProfileDao {
    @Insert(onConflict = REPLACE)
    fun insert(profile: Profile)

}