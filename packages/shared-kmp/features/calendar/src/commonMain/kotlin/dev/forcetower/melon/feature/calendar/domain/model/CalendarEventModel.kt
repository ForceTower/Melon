package dev.forcetower.melon.feature.calendar.domain.model

import kotlinx.datetime.LocalDate

// Strongly-typed projection of an academic calendar event for UI consumers.
// The use case parses the Room row's strings into LocalDate / enums here so
// no view code has to deal with the wire format.
data class CalendarEventFeed(
    val id: String,
    val platformId: String,
    val description: String,
    val start: LocalDate,
    val end: LocalDate?,
    val fixed: Boolean,
    val closed: Boolean,
    val scope: CalendarFeedScope,
    val origin: CalendarFeedOrigin,
)

enum class CalendarFeedScope {
    GENERAL, FACULTY, COURSE, CLASS, CAMPUS;

    companion object {
        fun fromWire(value: String): CalendarFeedScope? = when (value) {
            "GENERAL" -> GENERAL
            "FACULTY" -> FACULTY
            "COURSE" -> COURSE
            "CLASS" -> CLASS
            "CAMPUS" -> CAMPUS
            else -> null
        }
    }
}

enum class CalendarFeedOrigin {
    MANUAL, EVALUATION, FINAL_EXAM, SECOND_CALL, SECOND_EPOCH;

    companion object {
        fun fromWire(value: String): CalendarFeedOrigin? = when (value) {
            "MANUAL" -> MANUAL
            "EVALUATION" -> EVALUATION
            "FINAL_EXAM" -> FINAL_EXAM
            "SECOND_CALL" -> SECOND_CALL
            "SECOND_EPOCH" -> SECOND_EPOCH
            else -> null
        }
    }
}
