package dev.forcetower.unes.reminders

import android.content.Context
import co.touchlab.kermit.Logger
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventCategory
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventReminder
import dev.forcetower.melon.feature.calendar.domain.usecase.ObservePersonalEventsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

// AlarmManager is out of reach without Robolectric, so the alarm is checked
// through what `PersonalEventReminderAlarms.rearm` derives it from: the
// snapshot file, read back after every publish.
@OptIn(ExperimentalCoroutinesApi::class)
internal class PersonalEventReminderSchedulerTest {
    private val filesDir: File = Files.createTempDirectory("personal-reminders").toFile()
    private val context = mockk<Context>().also { context ->
        every { context.filesDir } returns filesDir
        every { context.getSystemService(Context.ALARM_SERVICE) } returns null
    }
    private val events = MutableSharedFlow<List<PersonalEvent>>(replay = 1, extraBufferCapacity = 8)

    @AfterTest
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun theLogoutWipeDisarmsThePreviousAccountsReminders() =
        runTest {
            startScheduler()
            events.emit(listOf(event("lista", "Lista 3")))
            runCurrent()
            assertEquals(listOf("Lista 3"), published().reminders.map { it.title })

            events.emit(emptyList())
            runCurrent()

            assertEquals(emptyList(), published().reminders)
            assertNull(PersonalEventReminderAlarms.nextFireEpochMs(published(), System.currentTimeMillis()))
            verify(exactly = 2) { context.getSystemService(Context.ALARM_SERVICE) }
        }

    @Test
    fun backToBackWritesPublishInTheOrderTheyLanded() =
        runTest {
            startScheduler()
            events.emit(listOf(event("lista", "Lista 3"), event("prova", "Revisar P2")))
            events.emit(listOf(event("lista", "Lista 3")))
            events.emit(emptyList())
            runCurrent()

            assertEquals(emptyList(), published().reminders)
        }

    private fun TestScope.startScheduler() {
        val observeEvents = mockk<ObservePersonalEventsUseCase>()
        every { observeEvents() } returns events
        PersonalEventReminderScheduler(context, observeEvents, backgroundScope, Logger).start()
        runCurrent()
    }

    private fun published(): PersonalEventReminderSnapshot = assertNotNull(PersonalEventReminderSnapshot.load(context))

    private fun event(
        id: String,
        title: String,
    ) = PersonalEvent(
        id = id,
        title = title,
        start = LocalDate(2099, 10, 20),
        end = null,
        category = PersonalEventCategory.Task,
        discipline = null,
        reminder = PersonalEventReminder.DayBefore,
        notes = "",
        createdAt = 0L,
        completedAt = null,
    )
}
