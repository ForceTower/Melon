package dev.forcetower.melon.feature.campusevent.domain.usecase

import dev.forcetower.melon.feature.campusevent.data.CampusEventRepository
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

// The featured event stream — hydrates from the offline snapshot, then emits
// every changing refresh. Null means nothing is featured for the student.
@Inject
class ObserveCampusEventUseCase internal constructor(
    private val repository: CampusEventRepository,
) {
    operator fun invoke(): Flow<CampusEvent?> = repository.observe()
}
