package dev.forcetower.unes.reminders

import android.content.Context
import co.touchlab.kermit.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent
import dev.forcetower.melon.feature.calendar.domain.usecase.ReadPersonalEventsUseCase
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

// Keeps the personal-entry reminder snapshot and its alarm honest. Unlike the
// evaluation reminders there is no global switch to gate on: a reminder exists
// only because the student picked one on that entry, so `refresh()` is called
// after every composer write and once on app start.
@Singleton
internal class PersonalEventReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val readEvents: ReadPersonalEventsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope,
    logger: Logger,
) {
    private val log = logger.withTag("PersonalEventReminderScheduler")

    fun start() = refresh()

    fun refresh() {
        scope.launch {
            val events = runCatching { readEvents() }.getOrElse {
                log.w(it) { "personal reminder read failed" }
                return@launch
            }
            val snapshot = PersonalEventReminderSnapshot(
                reminders = events.mapNotNull { it.toEntry() },
            )
            runCatching { PersonalEventReminderSnapshot.save(context, snapshot) }
                .onFailure { log.w(it) { "personal reminder snapshot write failed" } }
            PersonalEventReminderAlarms.rearm(context)
            log.d { "personal reminder snapshot published count=${snapshot.reminders.size}" }
        }
    }

    // The lead time is folded into the stored fire date, so the receiver only
    // has to match "today". Entries without a reminder drop out here.
    private fun PersonalEvent.toEntry(): PersonalEventReminderSnapshot.Entry? {
        if (reminder.days <= 0) return null
        val fireDate = start.minus(DatePeriod(days = reminder.days))
        return PersonalEventReminderSnapshot.Entry(
            id = id,
            title = title,
            fireDateIso = fireDate.toIsoString(),
        )
    }

    private fun LocalDate.toIsoString(): String = toString()
}
