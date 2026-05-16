package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.database.entity.AcademicCalendarEventEntity
import dev.forcetower.melon.core.sync.data.dto.CalendarEventDto

internal fun CalendarEventDto.toEntity(): AcademicCalendarEventEntity = AcademicCalendarEventEntity(
    id = id,
    platformId = platformId,
    description = description,
    start = start,
    end = end,
    fixed = fixed,
    closed = closed,
    scope = scope,
    origin = origin,
)
