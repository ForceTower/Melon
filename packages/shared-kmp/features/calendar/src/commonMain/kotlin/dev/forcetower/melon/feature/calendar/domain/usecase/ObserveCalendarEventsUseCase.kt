package dev.forcetower.melon.feature.calendar.domain.usecase

import dev.forcetower.melon.core.database.dao.CalendarEventDao
import dev.forcetower.melon.core.database.entity.AcademicCalendarEventEntity
import dev.forcetower.melon.feature.calendar.domain.model.CalendarEventFeed
import dev.forcetower.melon.feature.calendar.domain.model.CalendarFeedOrigin
import dev.forcetower.melon.feature.calendar.domain.model.CalendarFeedScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@Inject
class ObserveCalendarEventsUseCase internal constructor(
    private val dao: CalendarEventDao,
) {
    operator fun invoke(): Flow<List<CalendarEventFeed>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.project() } }

    // Defensive: rows whose scope/origin can't be projected are dropped at the
    // boundary. Server already filters unknowns, but if a future enum lands
    // in the DB before the client knows about it we'd rather skip it than
    // surface a half-typed row.
    private fun AcademicCalendarEventEntity.project(): CalendarEventFeed? {
        val scope = CalendarFeedScope.fromWire(scope) ?: return null
        val origin = CalendarFeedOrigin.fromWire(origin) ?: return null
        val startDate = runCatching { LocalDate.parse(start) }.getOrNull() ?: return null
        val endDate = end?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return CalendarEventFeed(
            id = id,
            platformId = platformId,
            description = description,
            start = startDate,
            end = endDate,
            fixed = fixed,
            closed = closed,
            scope = scope,
            origin = origin,
        )
    }
}
