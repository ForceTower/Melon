package dev.forcetower.melon.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.forcetower.melon.core.common.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface AndroidDatabaseGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun database(appContext: ApplicationContext): MelonDatabase {
            val dbFile = appContext.context.getDatabasePath("melon.db")
            dbFile.parentFile?.mkdirs()
            return Room.databaseBuilder<MelonDatabase>(
                context = appContext.context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
