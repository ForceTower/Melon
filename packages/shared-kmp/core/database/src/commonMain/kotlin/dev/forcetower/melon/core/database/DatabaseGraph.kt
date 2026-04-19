package dev.forcetower.melon.core.database

import dev.forcetower.melon.core.database.dao.UserDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// The MelonDatabase itself is provided per-platform (iosMain / jvmMain) because the builder setup
// differs. This common container only exposes the DAO as a graph binding.
@ContributesTo(AppScope::class)
interface DatabaseGraph {
    companion object {
        @Provides
        fun userDao(database: MelonDatabase): UserDao = database.userDao()
    }
}
