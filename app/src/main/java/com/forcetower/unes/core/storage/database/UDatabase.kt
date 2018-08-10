package com.forcetower.unes.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forcetower.unes.core.model.sagres.SagresAccess
import com.forcetower.unes.core.model.sagres.SagresMessage
import com.forcetower.unes.core.storage.database.dao.AccessDao

@Database(entities = [
    SagresAccess::class,
    SagresMessage::class
], version = 1, exportSchema = true)
abstract class UDatabase: RoomDatabase() {
    abstract fun accessDao(): AccessDao
}