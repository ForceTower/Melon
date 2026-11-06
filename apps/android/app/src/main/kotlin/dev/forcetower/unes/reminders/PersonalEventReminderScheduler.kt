package dev.forcetower.unes.reminders

import android.content.Context
import co.touchlab.kermit.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent
import dev.forcetower.melon.feature.calendar.domain.usecase.ObservePersonalEventsUseCase
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus

// Keeps the personal-entry reminder snapshot and its alarm honest: collects the
// student's entries from the database, so composer writes, check-offs and the
// logout wipe all republish through one collector, in order. Unlike the
// evaluation reminders there is no global switch to gate on: a reminder exists
// only because the student picked one on that entry. `start()` is called from
// `MelonApp` and is idempotent.
@Singleton
internal class PersonalEventReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val observeEvents: ObservePersonalEventsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope,
    logger: Logger,
) {
    private val log = logger.withTag("PersonalEventReminderScheduler")
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            observeEvents()
                .map { events -> PersonalEventReminderSnapshot(reminders = events.mapNotNull { it.toReminderEntry() }) }
                .distinctUntilChanged()
                .catch { log.w(it) { "personal reminder read failed" } }
                .collect { snapshot ->
                    runCatching { PersonalEventReminderSnapshot.save(context, snapshot) }
                        .onFailure { log.w(it) { "personal reminder snapshot write failed" } }
                    PersonalEventReminderAlarms.rearm(context)
                    log.d { "personal reminder snapshot published count=${snapshot.reminders.size}" }
                }
        }
    }
}

// The lead time is folded into the stored fire date, so the receiver only has
// to match "today". Entries without a reminder, and checked-off ones, drop out
// here.
internal fun PersonalEvent.toReminderEntry(): PersonalEventReminderSnapshot.Entry? {
    if (reminder.days <= 0 || isCompleted) return null
    val fireDate = start.minus(DatePeriod(days = reminder.days))
    return PersonalEventReminderSnapshot.Entry(
        id = id,
        title = title,
        fireDateIso = fireDate.toString(),
    )
}
