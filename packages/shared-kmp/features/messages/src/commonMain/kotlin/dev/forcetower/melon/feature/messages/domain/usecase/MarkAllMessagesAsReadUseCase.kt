package dev.forcetower.melon.feature.messages.domain.usecase

import dev.forcetower.melon.core.database.dao.MessageDao
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

// Bulk variant of `MarkMessageAsReadUseCase` — flips `readAt` for every
// currently unread message in a single transaction so the inbox flow only
// re-emits once. Like the single-message version, this is local-first: the
// pending-mutation queue (TODO) will replay the state server-side.
@Inject
class MarkAllMessagesAsReadUseCase internal constructor(
    private val messageDao: MessageDao,
) {
    suspend operator fun invoke() {
        messageDao.markAllRead(Clock.System.now().toString())
    }
}
