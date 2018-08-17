package com.forcetower.unes.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forcetower.unes.core.model.*
import com.forcetower.unes.core.storage.database.dao.*

@Database(entities = [
    Access::class,
    Profile::class,
    Semester::class,
    Message::class,
    CalendarItem::class,
    Discipline::class,
    AccessToken::class
], version = 1, exportSchema = true)
abstract class UDatabase: RoomDatabase() {
    abstract fun accessDao(): AccessDao
    abstract fun accessTokenDao(): AccessTokenDao
    abstract fun profileDao(): ProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun semesterDao(): SemesterDao
    abstract fun calendarDao(): CalendarDao
    abstract fun disciplineDao(): DisciplineDao
}