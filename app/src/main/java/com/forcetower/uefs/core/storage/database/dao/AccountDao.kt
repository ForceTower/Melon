package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetower.uefs.core.model.unes.Account

@Dao
abstract class AccountDao {
    @Query("SELECT * FROM Account LIMIT 1")
    abstract fun getAccount(): LiveData<Account>

    @Query("SELECT * FROM Account LIMIT 1")
    abstract fun getAccountNullable(): LiveData<Account?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(value: Account)

    @Query("DELETE FROM Account")
    abstract fun deleteAll()

    @Query("SELECT * FROM Account LIMIT 1")
    abstract fun getAccountDirect(): Account?
}
