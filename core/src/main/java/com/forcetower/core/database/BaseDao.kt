package com.forcetower.core.database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update

abstract class BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(value: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIgnore(value: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(values: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAllIgnore(values: List<T>): List<Long>

    @Update(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun update(value: T)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun updateAll(value: List<T>)

    @Delete
    abstract suspend fun delete(values: List<T>)

    @Delete
    abstract suspend fun delete(value: T)

    @Transaction
    open suspend fun insertOrUpdateAll(values: List<T>) {
        val insertResult = insertAllIgnore(values)
        val updateList = mutableListOf<T>()

        insertResult.forEachIndexed { index, result ->
            if (result == -1L) updateList += values[index]
        }

        if (updateList.isNotEmpty()) updateAll(updateList)
    }

    @Transaction
    open suspend fun insertOrUpdate(value: T) {
        val insertResult = insertIgnore(value)
        if (insertResult == -1L) update(value)
    }
}
