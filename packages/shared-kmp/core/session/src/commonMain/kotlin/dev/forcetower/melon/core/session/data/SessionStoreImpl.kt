package dev.forcetower.melon.core.session.data

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.SettingsDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.SyncStateDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.forcetower.melon.core.database.entity.CredentialsEntity
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal const val ACCESS_TOKEN_KEY = "melon.access_token"
internal const val REFRESH_TOKEN_KEY = "melon.refresh_token"

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AuthTokenSource>())
internal class SessionStoreImpl(
    private val storage: KeyValueStorage,
    private val userDao: UserDao,
    private val studentDao: StudentDao,
    private val semesterDao: SemesterDao,
    private val academicDao: AcademicDao,
    private val messageDao: MessageDao,
    private val settingsDao: SettingsDao,
    private val userSettingsDao: UserSettingsDao,
    private val credentialsDao: CredentialsDao,
    private val syncStateDao: SyncStateDao,
    private val scope: CoroutineScope,
    logger: Logger,
) : SessionStore {

    private val log = logger.withTag("SessionStoreImpl")
    private val tokenPresent = MutableStateFlow(false)

    override val authState: StateFlow<AuthState> =
        combine(tokenPresent, userDao.observeCurrent()) { hasToken, entity ->
            if (hasToken && entity != null) AuthState.Authenticated(entity.toDomain())
            else AuthState.Unauthenticated
        }.stateIn(scope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    init {
        scope.launch {
            val present = storage.get(ACCESS_TOKEN_KEY) != null
            tokenPresent.value = present
            log.i { "session bootstrapped tokenPresent=$present" }
        }
    }

    override suspend fun getAccessToken(): String? = storage.get(ACCESS_TOKEN_KEY)

    override suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String?,
        password: String?,
    ) {
        storage.put(ACCESS_TOKEN_KEY, accessToken)
        storage.put(REFRESH_TOKEN_KEY, refreshToken)
        userDao.upsert(UserEntity(id = user.id, name = user.name, imageUrl = user.imageUrl))
        if (username != null && password != null) {
            credentialsDao.upsert(
                CredentialsEntity(
                    userId = user.id,
                    username = username,
                    password = password,
                    updatedAt = Clock.System.now().toString(),
                )
            )
        }
        tokenPresent.value = true
        log.i { "session persisted userId=${user.id} hasUpstreamCreds=${username != null && password != null}" }
    }

    override suspend fun getCredentials(): UserCredentials? =
        credentialsDao.getCurrent()?.toDomain()

    override fun observeCredentials(): Flow<UserCredentials?> =
        credentialsDao.observeCurrent().map { it?.toDomain() }

    override suspend fun updateUpstreamCredentials(username: String, password: String) {
        val current = userDao.getCurrent() ?: run {
            log.w { "updateUpstreamCredentials skipped: no current user" }
            return
        }
        credentialsDao.upsert(
            CredentialsEntity(
                userId = current.id,
                username = username,
                password = password,
                updatedAt = Clock.System.now().toString(),
            )
        )
        log.i { "upstream credentials backfilled userId=${current.id}" }
    }

    override suspend fun logout() {
        log.i { "logout start" }
        storage.remove(ACCESS_TOKEN_KEY)
        storage.remove(REFRESH_TOKEN_KEY)
        // Wipe user-scoped local data on logout. We deliberately keep
        // `PendingMutation` so optimistic writes queued before logout can
        // still replay when the same user signs back in. Most deletes
        // cascade: `MessageDao.clear` takes scopes/attachments/states with
        // it, and clearing `Discipline` + `Semester` wipes the whole
        // academic subtree (DisciplineOffer → Class → StudentClass /
        // Lecture / Grade / ...). `credentialsDao.clear()` runs before the
        // user row is deleted so the teardown reads top-down even though
        // the FK cascade would also catch it.
        messageDao.clear()
        academicDao.clearDisciplines()
        academicDao.clearTeachers()
        academicDao.clearSpaces()
        semesterDao.clear()
        studentDao.clearStudents()
        studentDao.clearCourses()
        settingsDao.clear()
        userSettingsDao.clear()
        credentialsDao.clear()
        syncStateDao.clear()
        userDao.clear()
        tokenPresent.value = false
        log.i { "logout complete: local data wiped" }
    }
}

private fun UserEntity.toDomain() = User(id = id, name = name, imageUrl = imageUrl)

private fun CredentialsEntity.toDomain() =
    UserCredentials(username = username, password = password, updatedAt = updatedAt)
