package dev.forcetower.melon.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.entity.UserEntity

@Database(entities = [UserEntity::class], version = 1)
@ConstructedBy(MelonDatabaseConstructor::class)
abstract class MelonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

// Room KSP generates the actual per-target; the expect declaration lives here.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MelonDatabaseConstructor : RoomDatabaseConstructor<MelonDatabase> {
    override fun initialize(): MelonDatabase
}
