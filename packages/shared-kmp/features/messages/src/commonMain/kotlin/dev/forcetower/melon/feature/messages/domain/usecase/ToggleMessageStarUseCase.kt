package dev.forcetower.melon.feature.messages.domain.usecase

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.forcetower.melon.feature.messages.data.network.MessagesApi
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException

// Local-first: flips `starred` in the device DB (preserving `readAt`), then
// best-effort acks the backend (`POST api/sync/messages/star`) so other
// devices see the state. Mirrors `setStarred` in iOS
// `MessagesRepository+Live.swift`.
@Inject
class ToggleMessageStarUseCase internal constructor(
    private val messageDao: MessageDao,
    private val api: MessagesApi,
    logger: Logger,
) {
    private val log = logger.withTag("ToggleMessageStar")

    suspend operator fun invoke(messageId: String) {
        val existing = messageDao.getState(messageId)
        val message = messageDao.getMessage(messageId)
        // Toggle off the merged value the UI shows (server OR overlay), and
        // also flip the mirrored server flag — otherwise an unstar would stay
        // shadowed by the OR-merge until the ack round-trips through a sync.
        val starred = !(message?.starred == true || existing?.starred == true)
        messageDao.upsertState(
            MessageStateEntity(
                messageId = messageId,
                readAt = existing?.readAt,
                starred = starred,
                updatedAt = Clock.System.now().toString(),
            ),
        )
        messageDao.updateMessageStarred(messageId, starred)
        try {
            api.ackStar(id = messageId, starred = starred)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "setStarred ack failed id=$messageId starred=$starred" }
        }
    }
}
