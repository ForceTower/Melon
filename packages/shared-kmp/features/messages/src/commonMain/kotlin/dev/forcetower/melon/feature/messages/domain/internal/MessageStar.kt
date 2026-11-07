package dev.forcetower.melon.feature.messages.domain.internal

import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity

// A local star change the server has not acknowledged wins; otherwise the
// server's merged flag decides, including changes made on other devices.
internal fun MessageEntity.isStarred(state: MessageStateEntity?): Boolean =
    if (state?.starPending == true) state.starred else starred == true
