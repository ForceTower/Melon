package com.forcetower.unes.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forcetower.unes.core.model.Access
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.model.Profile
import com.forcetower.unes.core.model.Semester
import com.forcetower.unes.core.storage.database.dao.AccessDao
import com.forcetower.unes.core.storage.database.dao.MessageDao
import com.forcetower.unes.core.storage.database.dao.ProfileDao
import com.forcetower.unes.core.storage.database.dao.SemesterDao

@Database(entities = [
    Access::class,
    Profile::class,
    Semester::class,
    Message::class
], version = 1, exportSchema = true)
abstract class UDatabase: RoomDatabase() {
    abstract fun accessDao(): AccessDao
    abstract fun profileDao(): ProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun semesterDao(): SemesterDao
}