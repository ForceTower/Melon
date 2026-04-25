package dev.forcetower.melon.core.database

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.PendingMutationDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.SettingsDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.SyncStateDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// The MelonDatabase itself is provided per-platform (iosMain / jvmMain) because the builder setup
// differs. This common container only exposes the DAOs as graph bindings.
@ContributesTo(AppScope::class)
interface DatabaseGraph {
    companion object {
        @Provides fun userDao(database: MelonDatabase): UserDao = database.userDao()
        @Provides fun studentDao(database: MelonDatabase): StudentDao = database.studentDao()
        @Provides fun semesterDao(database: MelonDatabase): SemesterDao = database.semesterDao()
        @Provides fun academicDao(database: MelonDatabase): AcademicDao = database.academicDao()
        @Provides fun messageDao(database: MelonDatabase): MessageDao = database.messageDao()
        @Provides fun settingsDao(database: MelonDatabase): SettingsDao = database.settingsDao()
        @Provides fun userSettingsDao(database: MelonDatabase): UserSettingsDao = database.userSettingsDao()
        @Provides fun credentialsDao(database: MelonDatabase): CredentialsDao = database.credentialsDao()
        @Provides fun syncStateDao(database: MelonDatabase): SyncStateDao = database.syncStateDao()
        @Provides fun pendingMutationDao(database: MelonDatabase): PendingMutationDao = database.pendingMutationDao()
    }
}
