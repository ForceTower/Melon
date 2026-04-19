package dev.forcetower.melon.feature.sync.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.MessagePageResult
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.core.sync.domain.repository.MirrorRepository
import dev.zacsweers.metro.Inject

// Pulls a single page of the inbox and applies it to the local mirror.
// First-page-only is the caller's choice (pass `cursor = null`); for full
// pagination, the caller chains invocations using each result's nextCursor.
@Inject
class SyncMessagesUseCase internal constructor(
    private val mirror: MirrorRepository,
) {
    suspend operator fun invoke(
        since: String? = null,
        cursor: String? = null,
    ): Outcome<MessagePageResult, SyncError> = mirror.syncMessages(since = since, cursor = cursor)
}
