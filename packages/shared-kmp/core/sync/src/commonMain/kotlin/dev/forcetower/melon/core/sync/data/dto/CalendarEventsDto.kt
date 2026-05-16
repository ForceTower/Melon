package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CalendarEventsResponse(
    val events: List<CalendarEventDto>,
)

@Serializable
internal data class CalendarEventDto(
    val id: String,
    val platformId: String,
    val description: String,
    // YYYY-MM-DD
    val start: String,
    val end: String?,
    val fixed: Boolean,
    val closed: Boolean,
    // GENERAL | FACULTY | COURSE | CLASS | CAMPUS
    val scope: String,
    // MANUAL | EVALUATION | FINAL_EXAM | SECOND_CALL | SECOND_EPOCH
    val origin: String,
)
