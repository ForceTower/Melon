package dev.forcetower.melon.feature.messages.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.feature.messages.data.network.MessagesApi
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException

// Bulk variant of `MarkMessageAsReadUseCase` — flips `readAt` for every
// currently unread message in a single transaction so the inbox flow only
// re-emits once, then best-effort acks the backend
// (`POST api/sync/messages/read-all`). Mirrors `markAllRead` in iOS
// `MessagesRepository+Live.swift`.
@Inject
class MarkAllMessagesAsReadUseCase internal constructor(
    private val messageDao: MessageDao,
    private val api: MessagesApi,
    logger: Logger,
) {
    private val log = logger.withTag("MarkAllMessagesAsRead")

    suspend operator fun invoke() {
        messageDao.markAllRead(Clock.System.now().toString())
        try {
            api.ackReadAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "markAllRead ack failed" }
        }
    }
}
