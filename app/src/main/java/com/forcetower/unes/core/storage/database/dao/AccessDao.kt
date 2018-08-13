package com.forcetower.unes.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.unes.core.model.Access

@Dao
abstract class AccessDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(access: Access)

    @Transaction
    fun insert(username: String, password: String) {
        val access = findDirect(username, password)
        if (access != null) return
        deleteAll()
        insert(Access(username = username, password = password))
    }

    @Query("SELECT * FROM Access WHERE username = :username AND password = :password LIMIT 1")
    abstract fun findDirect(username: String, password: String): Access?

    @Query("DELETE FROM Access")
    abstract fun deleteAll()
}