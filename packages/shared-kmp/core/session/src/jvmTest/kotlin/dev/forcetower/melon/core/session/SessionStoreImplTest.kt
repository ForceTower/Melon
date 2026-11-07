package dev.forcetower.melon.core.session

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.MelonDatabase
import dev.forcetower.melon.core.database.entity.AcademicCalendarEventEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.session.data.ACCESS_TOKEN_KEY
import dev.forcetower.melon.core.session.data.SessionStoreImpl
import dev.forcetower.melon.core.session.data.UPSTREAM_USERNAME_KEY
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.core.storage.KeyValueStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class SessionStoreImplTest {
    @Test
    fun logoutForgetsTheUpstreamUsernameAndTheAcademicCalendar() =
        withStore { store, database, storage ->
            store.persist("a1", "r1", User("a", "Alice", null), "alice", "secret")
            store.setUpstreamUsername("alice")
            database.calendarEventDao().upsertAll(listOf(calendarEvent("course-event")))

            store.logout()

            assertEquals(emptyList(), database.calendarEventDao().observeAll().first())
            assertNull(store.upstreamUsername.value)
            assertNull(storage.get(UPSTREAM_USERNAME_KEY))
        }

    @Test
    fun aFailedTeardownKeepsTheWholeSession() =
        withStore { store, database, storage ->
            store.persist("a1", "r1", User("a", "Alice", null), "alice", "secret")
            database.messageDao().upsertMessages(listOf(message("m1")))
            database.useWriterConnection {
                it.executeSQL("CREATE TRIGGER fail BEFORE DELETE ON User BEGIN SELECT RAISE(ABORT, 'boom'); END")
            }

            assertTrue(runCatching { store.logout() }.isFailure)

            assertEquals("a1", storage.get(ACCESS_TOKEN_KEY))
            assertEquals("secret", store.getCredentials()?.password)
            assertNotNull(database.messageDao().getMessage("m1"))
            assertNotNull(database.userDao().getCurrent())
        }

    @Test
    fun aCancelledCallerStillFinishesTheLogout() =
        withStore { store, database, storage ->
            store.persist("a1", "r1", User("a", "Alice", null), "alice", "secret")

            coroutineScope {
                launch(start = CoroutineStart.UNDISPATCHED) { store.logout() }.cancelAndJoin()
            }

            assertNull(storage.get(ACCESS_TOKEN_KEY))
            assertNull(store.getCredentials())
            assertNull(database.userDao().getCurrent())
        }

    @Test
    fun aRotationLandingAfterLogoutDoesNotRestoreTheTokens() =
        withStore { store, _, _ ->
            store.persist("a1", "r1", User("a", "Alice", null))
            store.logout()

            assertFalse(store.replaceTokens(staleAccessToken = "a1", accessToken = "a2", refreshToken = "r2"))

            assertNull(store.getAccessToken())
            assertNull(store.getRefreshToken())
        }

    @Test
    fun aRotationOnlyReplacesThePairItStartedFrom() =
        withStore { store, _, _ ->
            store.persist("a1", "r1", User("a", "Alice", null))

            assertTrue(store.replaceTokens(staleAccessToken = "a1", accessToken = "a2", refreshToken = "r2"))
            assertFalse(store.replaceTokens(staleAccessToken = "a1", accessToken = "a3", refreshToken = "r3"))

            assertEquals("a2", store.getAccessToken())
            assertEquals("r2", store.getRefreshToken())
        }

    @Test
    fun aPasswordLoginClearsTheCredentialsInvalidFlag() =
        withStore { store, _, _ ->
            store.persist("a1", "r1", User("a", "Alice", null))
            store.setCredentialsInvalid(true)

            store.persist("a2", "r2", User("a", "Alice", null))
            assertTrue(store.credentialsInvalid.value)

            store.persist("a3", "r3", User("a", "Alice", null), "alice", "fresh")
            assertFalse(store.credentialsInvalid.value)
        }

    private fun withStore(block: suspend (SessionStoreImpl, MelonDatabase, KeyValueStorage) -> Unit) =
        runBlocking {
            val database = Room.inMemoryDatabaseBuilder<MelonDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val storage = MemoryStorage()
            try {
                val store = SessionStoreImpl(
                    storage = storage,
                    userDao = database.userDao(),
                    studentDao = database.studentDao(),
                    semesterDao = database.semesterDao(),
                    academicDao = database.academicDao(),
                    messageDao = database.messageDao(),
                    settingsDao = database.settingsDao(),
                    userSettingsDao = database.userSettingsDao(),
                    credentialsDao = database.credentialsDao(),
                    syncStateDao = database.syncStateDao(),
                    personalEventDao = database.personalEventDao(),
                    curriculumDao = database.curriculumDao(),
                    calendarEventDao = database.calendarEventDao(),
                    database = database,
                    scope = scope,
                    logger = Logger,
                )
                block(store, database, storage)
            } finally {
                scope.cancel()
                database.close()
            }
        }

    private fun calendarEvent(id: String) =
        AcademicCalendarEventEntity(
            id = id,
            platformId = id,
            description = "Course event",
            start = "2026-09-14",
            end = null,
            fixed = false,
            closed = false,
            scope = "COURSE",
            origin = "MANUAL",
        )

    private fun message(id: String) =
        MessageEntity(
            id = id,
            platformId = id,
            source = "upstream",
            subject = "Subject",
            content = "Body",
            senderName = "Teacher",
            senderType = null,
            timestamp = "2026-09-14T10:00:00Z",
            createdAt = "2026-09-14T10:00:00Z",
            read = false,
            starred = false,
        )
}

private class MemoryStorage : KeyValueStorage {
    private val values = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = values[key]

    override suspend fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
