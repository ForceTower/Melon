package dev.forcetower.melon.feature.courseprogress.domain.usecase

import dev.forcetower.melon.feature.courseprogress.data.CourseProgressRepository
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

// The mirrored curriculum payload. Emits on subscription so "Progresso" and
// the fluxograma paint offline, and again after every refresh that changes
// something. Null means nothing has been synced yet on this device.
@Inject
class ObserveCourseProgressUseCase internal constructor(
    private val repository: CourseProgressRepository,
) {
    operator fun invoke(): Flow<CourseProgress?> = repository.observe()
}
