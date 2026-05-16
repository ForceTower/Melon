package dev.forcetower.unes.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.schedule.domain.model.NextClassDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveNextClassDayUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Subscribes to the KMP schedule flows (current week for today's classes,
// `ObserveNextClassDayUseCase` for the first future day with classes) and
// writes a JSON snapshot to internal storage, then asks AppWidgetManager
// to refresh the "Próxima aula" widget.
//
// Two flows on purpose, mirroring iOS: the week flow drives today's strip,
// while the next-class-day flow looks past the current week so Friday →
// Tuesday (with no Sat/Sun classes in between) still surfaces real data.
//
// The widget recomputes time-derived state (running / upcoming / dayDone,
// countdowns) on every render, so a stale snapshot still renders correctly.
// `start()` is called from `ConnectedViewModel` once the user lands on the
// authenticated shell.
@Singleton
class WidgetSnapshotPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val week: ObserveScheduleWeekUseCase,
    private val nextDay: ObserveNextClassDayUseCase,
    logger: Logger,
) {
    private val log = logger.withTag("WidgetSnapshotPublisher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var weekJob: Job? = null
    private var nextDayJob: Job? = null
    private var latestWeek: ScheduleWeek? = null
    private var latestNextDay: NextClassDay? = null

    fun start() {
        if (weekJob?.isActive == true) return
        log.i { "subscribing to widget data flows" }
        weekJob = scope.launch {
            week().collect { value ->
                mutex.withLock {
                    latestWeek = value
                    publish("week")
                }
            }
        }
        nextDayJob = scope.launch {
            nextDay().collect { value ->
                mutex.withLock {
                    latestNextDay = value
                    publish("nextDay")
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun publish(reason: String) {
        val w = latestWeek ?: return
        val snapshot = buildSnapshot(w, latestNextDay)
        runCatching { WidgetSnapshot.save(context, snapshot) }
            .onSuccess {
                log.d {
                    "widget snapshot published reason=$reason classes=${snapshot.today.size} " +
                        "nextDay=${snapshot.nextDay?.dateIso ?: "-"}"
                }
                broadcastUpdate()
            }
            .onFailure { e ->
                log.w(e) { "widget snapshot write failed reason=$reason" }
            }
    }

    // The widget host pulls our composition on the next AppWidgetProvider
    // update broadcast — send one explicitly here so the widget reflects the
    // new snapshot within seconds of a KMP flow tick rather than waiting on
    // the per-minute AlarmManager beat.
    private fun broadcastUpdate() {
        val mgr = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, NextClassWidgetReceiver::class.java)
        val ids = mgr.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    private fun buildSnapshot(
        week: ScheduleWeek,
        nextDay: NextClassDay?,
    ): WidgetSnapshot {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val todayIso = now.date.toString()
        val days = week.days.sortedBy { it.dayIndex }
        val todayDay = days.firstOrNull { it.dateIso == todayIso }
        val todayClasses = (todayDay?.classes ?: emptyList())
            .sortedBy { it.startTime }
            .map(::convert)

        return WidgetSnapshot(
            generatedAtEpochMs = nowInstant.toEpochMilliseconds(),
            todayDateIso = todayIso,
            today = todayClasses,
            nextDay = nextDay?.let {
                WidgetSnapshot.NextDay(
                    dateIso = it.dateIso,
                    daysAway = it.daysAway,
                    first = convert(it.first),
                )
            },
        )
    }

    private fun convert(c: ScheduleClass): WidgetSnapshot.Class = WidgetSnapshot.Class(
        classId = c.classId,
        code = c.code,
        title = c.title,
        prof = c.teacherName,
        room = c.room,
        topic = c.topic,
        startTime = trim(c.startTime),
        endTime = c.endTime?.let(::trim),
    )

    // KMP carries times as "HH:mm" or "HH:mm:ss" depending on upstream — same
    // fixup the iOS publisher does (`String(value.prefix(5))`).
    private fun trim(value: String): String = if (value.length > 5) value.substring(0, 5) else value
}
