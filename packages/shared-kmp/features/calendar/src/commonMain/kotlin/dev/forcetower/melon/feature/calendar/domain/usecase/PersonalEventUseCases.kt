package dev.forcetower.melon.feature.calendar.domain.usecase

import dev.forcetower.melon.core.database.dao.PersonalEventDao
import dev.forcetower.melon.core.database.entity.PersonalEventEntity
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEvent
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventCategory
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventDiscipline
import dev.forcetower.melon.feature.calendar.domain.model.PersonalEventReminder
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@Inject
class ObservePersonalEventsUseCase internal constructor(
    private val dao: PersonalEventDao,
) {
    operator fun invoke(): Flow<List<PersonalEvent>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.project() } }
}

// One-shot read for the reminder snapshot, which runs outside composition.
@Inject
class ReadPersonalEventsUseCase internal constructor(
    private val dao: PersonalEventDao,
) {
    suspend operator fun invoke(): List<PersonalEvent> = dao.all().mapNotNull { it.project() }
}

@Inject
class SavePersonalEventUseCase internal constructor(
    private val dao: PersonalEventDao,
) {
    suspend operator fun invoke(event: PersonalEvent) = dao.upsert(event.toEntity())
}

@Inject
class DeletePersonalEventUseCase internal constructor(
    private val dao: PersonalEventDao,
) {
    suspend operator fun invoke(id: String) = dao.deleteById(id)
}

// A row whose start date can't be parsed is unrenderable, so it's dropped at
// the boundary rather than surfaced half-typed.
private fun PersonalEventEntity.project(): PersonalEvent? {
    val startDate = runCatching { LocalDate.parse(start) }.getOrNull() ?: return null
    val endDate = end?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return PersonalEvent(
        id = id,
        title = title,
        start = startDate,
        end = endDate,
        category = PersonalEventCategory.fromWire(category),
        discipline = discipline(),
        reminder = PersonalEventReminder.fromDays(reminderDays),
        notes = notes,
        createdAt = createdAt,
    )
}

private fun PersonalEventEntity.discipline(): PersonalEventDiscipline? {
    val id = disciplineId ?: return null
    val code = disciplineCode ?: return null
    val name = disciplineName ?: return null
    return PersonalEventDiscipline(id = id, code = code, name = name)
}

private fun PersonalEvent.toEntity(): PersonalEventEntity = PersonalEventEntity(
    id = id,
    title = title,
    start = start.toString(),
    end = end?.toString(),
    category = category.wire,
    disciplineId = discipline?.id,
    disciplineCode = discipline?.code,
    disciplineName = discipline?.name,
    reminderDays = reminder.days,
    notes = notes,
    createdAt = createdAt,
)
