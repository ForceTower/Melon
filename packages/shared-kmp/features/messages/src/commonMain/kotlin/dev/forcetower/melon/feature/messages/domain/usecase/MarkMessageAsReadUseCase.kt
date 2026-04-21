package dev.forcetower.melon.feature.messages.domain.usecase

import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Clock

// Local-first: flips `readAt` in the device DB. The backend has no ack
// endpoint yet (sync is read-only for messages today) — a follow-up will
// enqueue a pending mutation so the state replays server-side.
@Inject
class MarkMessageAsReadUseCase internal constructor(
    private val messageDao: MessageDao,
) {
    suspend operator fun invoke(messageId: String) {
        val existing = messageDao.getState(messageId)
        if (existing?.readAt != null) return
        val now = Clock.System.now().toString()
        messageDao.upsertState(
            MessageStateEntity(
                messageId = messageId,
                readAt = now,
                starred = existing?.starred == true,
                updatedAt = now,
            ),
        )
    }
}
