package dev.forcetower.melon.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files

@ContributesTo(AppScope::class)
interface JvmDatabaseGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun database(): MelonDatabase {
            val tempDir = Files.createTempDirectory("melon-db").toFile()
            val dbFile = File(tempDir, "melon.db")
            return Room.databaseBuilder<MelonDatabase>(name = dbFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
