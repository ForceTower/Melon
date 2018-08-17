package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.forcetower.unes.core.model.AccessToken

@Dao
interface AccessTokenDao {
    @Insert(onConflict = REPLACE)
    fun insert(access: AccessToken)

    @Query("SELECT * FROM AccessToken LIMIT 1")
    fun getAccess(): LiveData<AccessToken?>

    @Query("SELECT * FROM AccessToken LIMIT 1")
    fun getAccessDirect(): AccessToken?

    @Query("DELETE FROM AccessToken")
    fun deleteAll()
}