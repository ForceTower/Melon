package dev.forcetower.melon.core.session

import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@ContributesTo(AppScope::class)
interface SessionGraph {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun applicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @SingleIn(AppScope::class)
        fun sessionStore(
            storage: KeyValueStorage,
            userDao: UserDao,
            scope: CoroutineScope,
        ): SessionStore = SessionStoreImpl(storage, userDao, scope)

        @Provides
        fun authTokenSource(sessionStore: SessionStore): AuthTokenSource = sessionStore
    }
}
