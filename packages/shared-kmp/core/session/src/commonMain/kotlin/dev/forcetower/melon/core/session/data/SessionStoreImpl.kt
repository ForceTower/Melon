package dev.forcetower.melon.core.session.data

import androidx.room3.withWriteTransaction
import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.MelonDatabase
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.CalendarEventDao
import dev.forcetower.melon.core.database.dao.CredentialsDao
import dev.forcetower.melon.core.database.dao.CurriculumDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.PersonalEventDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.SettingsDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.SyncStateDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.dao.UserSettingsDao
import dev.forcetower.melon.core.database.entity.CredentialsEntity
import dev.forcetower.melon.core.database.entity.UserEntity
import dev.forcetower.melon.core.network.AuthTokenSource
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.session.domain.model.UserCredentials
import dev.forcetower.melon.core.storage.KeyValueStorage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val ACCESS_TOKEN_KEY = "melon.access_token"
internal const val REFRESH_TOKEN_KEY = "melon.refresh_token"
internal const val SESSION_INVALID_KEY = "melon.session_invalid"
internal const val CREDENTIALS_INVALID_KEY = "melon.credentials_invalid"
internal const val UPSTREAM_USERNAME_KEY = "melon.upstream_username"

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
    private val personalEventDao: PersonalEventDao,
    private val curriculumDao: CurriculumDao,
    private val calendarEventDao: CalendarEventDao,
    private val database: MelonDatabase,
    private val scope: CoroutineScope,
    logger: Logger,
) : SessionStore {

    private val log = logger.withTag("SessionStoreImpl")

    // Serializes every write of the token pair, so a rotation's compare-and-set
    // can't interleave with a login or a logout.
    private val tokens = Mutex()
    private val tokenPresent = MutableStateFlow(false)
    private val sessionInvalidState = MutableStateFlow(false)
    private val credentialsInvalidState = MutableStateFlow(false)
    private val upstreamUsernameState = MutableStateFlow<String?>(null)

    override val authState: StateFlow<AuthState> =
        combine(tokenPresent, userDao.observeCurrent()) { hasToken, entity ->
            if (hasToken && entity != null) {
                AuthState.Authenticated(entity.toDomain())
            } else {
                AuthState.Unauthenticated
            }
        }.stateIn(scope, SharingStarted.Eagerly, AuthState.Unauthenticated)

    override val sessionInvalid: StateFlow<Boolean> = sessionInvalidState

    override val credentialsInvalid: StateFlow<Boolean> = credentialsInvalidState

    override val upstreamUsername: StateFlow<String?> = upstreamUsernameState

    init {
        scope.launch {
            val present = storage.get(ACCESS_TOKEN_KEY) != null
            tokenPresent.value = present
            // Persisted so the banner renders on a cold start while offline.
            sessionInvalidState.value = storage.get(SESSION_INVALID_KEY) == "true"
            credentialsInvalidState.value = storage.get(CREDENTIALS_INVALID_KEY) == "true"
            upstreamUsernameState.value = storage.get(UPSTREAM_USERNAME_KEY)
            log.i {
                "session bootstrapped tokenPresent=$present invalid=${sessionInvalidState.value} " +
                    "credentialsInvalid=${credentialsInvalidState.value}"
            }
        }
    }

    override suspend fun getAccessToken(): String? = storage.get(ACCESS_TOKEN_KEY)

    override suspend fun getRefreshToken(): String? = storage.get(REFRESH_TOKEN_KEY)

    override suspend fun replaceTokens(
        staleAccessToken: String,
        accessToken: String,
        refreshToken: String,
    ): Boolean =
        tokens.withLock {
            if (storage.get(ACCESS_TOKEN_KEY) != staleAccessToken) {
                log.i { "token rotation dropped: the session ended or changed meanwhile" }
                return@withLock false
            }
            storage.put(ACCESS_TOKEN_KEY, accessToken)
            storage.put(REFRESH_TOKEN_KEY, refreshToken)
            log.i { "token pair rotated" }
            true
        }

    override suspend fun setSessionInvalid(invalid: Boolean) {
        if (sessionInvalidState.value == invalid) return
        storage.put(SESSION_INVALID_KEY, invalid.toString())
        sessionInvalidState.value = invalid
        log.i { "sessionInvalid=$invalid" }
    }

    override suspend fun setCredentialsInvalid(invalid: Boolean) {
        if (credentialsInvalidState.value == invalid) return
        storage.put(CREDENTIALS_INVALID_KEY, invalid.toString())
        credentialsInvalidState.value = invalid
        log.i { "credentialsInvalid=$invalid" }
    }

    override suspend fun setUpstreamUsername(username: String) {
        if (upstreamUsernameState.value == username) return
        storage.put(UPSTREAM_USERNAME_KEY, username)
        upstreamUsernameState.value = username
    }

    override suspend fun currentAuthState(): AuthState {
        val token = storage.get(ACCESS_TOKEN_KEY)
        val user = userDao.getCurrent()
        return if (token != null && user != null) {
            AuthState.Authenticated(user.toDomain())
        } else {
            AuthState.Unauthenticated
        }
    }

    override suspend fun persist(
        accessToken: String,
        refreshToken: String,
        user: User,
        username: String?,
        password: String?,
    ) {
        tokens.withLock {
            storage.put(ACCESS_TOKEN_KEY, accessToken)
            storage.put(REFRESH_TOKEN_KEY, refreshToken)
        }
        userDao.upsert(UserEntity(id = user.id, name = user.name, imageUrl = user.imageUrl))
        if (username != null && password != null) {
            credentialsDao.upsert(
                CredentialsEntity(
                    userId = user.id,
                    username = username,
                    password = password,
                    updatedAt = Clock.System.now().toString(),
                ),
            )
            // The server only signs a password login in after the portal took
            // that password; a passkey login says nothing about it.
            setCredentialsInvalid(false)
        }
        tokenPresent.value = true
        setSessionInvalid(false)
        log.i { "session persisted userId=${user.id} hasUpstreamCreds=${username != null && password != null}" }
    }

    override suspend fun getCredentials(): UserCredentials? = credentialsDao.getCurrent()?.toDomain()

    override fun observeCredentials(): Flow<UserCredentials?> = credentialsDao.observeCurrent().map { it?.toDomain() }

    override suspend fun updateUpstreamCredentials(
        username: String,
        password: String,
    ) {
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
            ),
        )
        log.i { "upstream credentials backfilled userId=${current.id}" }
    }

    // A confirmed logout finishes even when the caller's scope is cancelled
    // mid-teardown.
    override suspend fun logout() =
        withContext(NonCancellable) {
            log.i { "logout start" }
            // Wipe user-scoped local data on logout, in one transaction and
            // before the tokens go, so a failure part-way keeps the whole
            // session rather than handing the previous account's `User` and
            // `Credentials` rows to the next login. `PendingMutation` is not
            // cleared, but nothing enqueues into it yet; whatever starts writing
            // there must clear it here or scope its rows to a user, so one
            // account's writes never replay under another. Most deletes
            // cascade: `MessageDao.clear` takes scopes/attachments/states with
            // it, and clearing `Discipline` + `Semester` wipes the whole
            // academic subtree (DisciplineOffer → Class → StudentClass /
            // Lecture / Grade / ...). `credentialsDao.clear()` runs before the
            // user row is deleted so the teardown reads top-down even though
            // the FK cascade would also catch it.
            database.withWriteTransaction {
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
                // The student's own calendar entries go too: the next account on this
                // device must not inherit them.
                personalEventDao.clear()
                // The curriculum mirror is course-scoped and carries the student's own
                // situation per slot, so it can't survive into the next account.
                curriculumDao.clear()
                // The academic calendar holds course- and class-scoped rows and has
                // no FK to cascade from.
                calendarEventDao.clear()
                userDao.clear()
            }
            tokens.withLock {
                storage.remove(ACCESS_TOKEN_KEY)
                storage.remove(REFRESH_TOKEN_KEY)
            }
            storage.remove(UPSTREAM_USERNAME_KEY)
            upstreamUsernameState.value = null
            tokenPresent.value = false
            setSessionInvalid(false)
            setCredentialsInvalid(false)
            log.i { "logout complete: local data wiped" }
        }
}

private fun UserEntity.toDomain() = User(id = id, name = name, imageUrl = imageUrl)

private fun CredentialsEntity.toDomain() =
    UserCredentials(username = username, password = password, updatedAt = updatedAt)
