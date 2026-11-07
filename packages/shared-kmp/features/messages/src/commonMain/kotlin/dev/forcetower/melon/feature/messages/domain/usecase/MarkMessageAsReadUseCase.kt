package dev.forcetower.melon.feature.messages.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.feature.messages.data.network.MessagesApi
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException

// Local-first: flips `readAt` in the device DB, then best-effort acks the
// backend (`POST api/sync/messages/read`) so other devices see the state.
// The overlay already flipped the row and the mirror never resurrects an
// unread dot, so a lost ack only leaves the server behind until the next
// read. Mirrors `markRead` in iOS `MessagesRepository+Live.swift`.
@Inject
class MarkMessageAsReadUseCase internal constructor(
    private val messageDao: MessageDao,
    private val api: MessagesApi,
    logger: Logger,
) {
    private val log = logger.withTag("MarkMessageAsRead")

    suspend operator fun invoke(messageId: String) {
        if (messageDao.getMessage(messageId)?.read == true) return
        if (messageDao.getState(messageId)?.readAt != null) return
        messageDao.markRead(messageId, Clock.System.now().toString())
        try {
            api.ackRead(listOf(messageId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "markRead ack failed id=$messageId" }
        }
    }
}
