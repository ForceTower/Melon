package dev.forcetower.melon.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@ContributesTo(AppScope::class)
interface IosDatabaseGraph {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        @Provides
        @SingleIn(AppScope::class)
        fun database(): MelonDatabase {
            val documentsUrl = NSFileManager.defaultManager
                .URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                ) as NSURL
            val dbPath = requireNotNull(documentsUrl.path) + "/melon.db"
            return Room.databaseBuilder<MelonDatabase>(name = dbPath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
