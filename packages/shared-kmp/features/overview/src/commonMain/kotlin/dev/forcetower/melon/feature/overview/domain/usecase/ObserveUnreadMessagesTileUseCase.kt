package dev.forcetower.melon.feature.overview.domain.usecase

import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.feature.overview.domain.model.OverviewMessagesTile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class ObserveUnreadMessagesTileUseCase internal constructor(
    private val messageDao: MessageDao,
) {
    operator fun invoke(): Flow<OverviewMessagesTile> =
        combine(
            messageDao.observeUnreadCount(),
            messageDao.observeLatestUnread(),
        ) { count, head ->
            val preview = head?.subject
                ?.takeIf { it.isNotBlank() }
                ?: head?.content?.take(80)
            OverviewMessagesTile(
                unreadCount = count,
                lastSender = head?.senderName,
                lastPreview = preview,
                lastTimestamp = head?.timestamp,
            )
        }
}
