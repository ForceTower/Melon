package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.ProfileStatement

@Dao
abstract class ProfileStatementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(statements: List<ProfileStatement>)

    @Query("SELECT * FROM ProfileStatement WHERE receiverId = :profileId")
    abstract fun getStatements(profileId: Long): LiveData<List<ProfileStatement>>
}
