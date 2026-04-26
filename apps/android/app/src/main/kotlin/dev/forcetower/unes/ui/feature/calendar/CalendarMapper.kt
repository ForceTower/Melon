package dev.forcetower.unes.ui.feature.calendar

import java.time.LocalDate
import dev.forcetower.melon.feature.calendar.domain.model.CalendarEventFeed as KmpEvent
import dev.forcetower.melon.feature.calendar.domain.model.CalendarFeedOrigin as KmpOrigin
import dev.forcetower.melon.feature.calendar.domain.model.CalendarFeedScope as KmpScope
import kotlinx.datetime.LocalDate as KmpLocalDate

// Projects a KMP `CalendarEventFeed` into the Android `CalendarEvent` so the
// agenda / hero / filter code never has to deal with the shared-layer shapes.
// Mirrors iOS `CalendarMapping`.
internal fun mapCalendarEvent(feed: KmpEvent): CalendarEvent = CalendarEvent(
    id = feed.id,
    description = feed.description,
    start = feed.start.toJavaLocalDate(),
    end = feed.end?.toJavaLocalDate(),
    fixed = feed.fixed,
    closed = feed.closed,
    scope = mapScope(feed.scope),
    origin = mapOrigin(feed.origin),
)

private fun mapScope(raw: KmpScope): CalendarScope = when (raw) {
    KmpScope.GENERAL -> CalendarScope.General
    KmpScope.FACULTY -> CalendarScope.Faculty
    KmpScope.COURSE -> CalendarScope.Course
    KmpScope.CLASS -> CalendarScope.ClassScope
    KmpScope.CAMPUS -> CalendarScope.Campus
}

private fun mapOrigin(raw: KmpOrigin): CalendarOrigin = when (raw) {
    KmpOrigin.MANUAL -> CalendarOrigin.Manual
    KmpOrigin.EVALUATION -> CalendarOrigin.Evaluation
    KmpOrigin.FINAL_EXAM -> CalendarOrigin.FinalExam
    KmpOrigin.SECOND_CALL -> CalendarOrigin.SecondCall
    KmpOrigin.SECOND_EPOCH -> CalendarOrigin.SecondEpoch
}

private fun KmpLocalDate.toJavaLocalDate(): LocalDate =
    LocalDate.of(year, monthNumber, dayOfMonth)
