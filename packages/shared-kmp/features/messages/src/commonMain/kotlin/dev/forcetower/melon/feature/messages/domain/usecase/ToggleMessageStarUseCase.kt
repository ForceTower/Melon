package dev.forcetower.melon.feature.messages.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.feature.messages.data.network.MessagesApi
import dev.forcetower.melon.feature.messages.domain.internal.isStarred
import dev.zacsweers.metro.Inject
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException

// Local-first: records the flipped star as pending in the device DB
// (preserving `readAt`), then acks the backend (`POST api/sync/messages/star`)
// so other devices see it. A pending change wins over the mirrored server
// value until the server acknowledges it; one the server never took is
// replayed by the next inbox sync. Mirrors `setStarred` in iOS
// `MessagesRepository+Live.swift`.
@Inject
class ToggleMessageStarUseCase internal constructor(
    private val messageDao: MessageDao,
    private val api: MessagesApi,
    logger: Logger,
) {
    private val log = logger.withTag("ToggleMessageStar")

    suspend operator fun invoke(messageId: String) {
        val message = messageDao.getMessage(messageId) ?: return
        val starred = !message.isStarred(messageDao.getState(messageId))
        messageDao.markStarPending(messageId, starred, Clock.System.now().toString())
        if (ack(messageId, starred)) messageDao.acknowledgeStar(messageId, starred)
    }

    private suspend fun ack(
        messageId: String,
        starred: Boolean,
    ): Boolean =
        try {
            val status = api.ackStar(id = messageId, starred = starred).status
            if (!status.isSuccess()) log.w { "setStarred ack rejected id=$messageId status=${status.value}" }
            status.isSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "setStarred ack failed id=$messageId starred=$starred" }
            false
        }
}
