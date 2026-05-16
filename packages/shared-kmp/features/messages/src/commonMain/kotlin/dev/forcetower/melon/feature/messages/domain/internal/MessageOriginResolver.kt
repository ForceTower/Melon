package dev.forcetower.melon.feature.messages.domain.internal

import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedOrigin
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedSource

// Picks a single origin for a message from its list of scope rows. Messages
// can carry multiple scopes (e.g. university + coordination); we keep the
// most specific one so the UI surfaces the clearest signal. `source='app'`
// always wins — those are UNES-authored and shouldn't masquerade as a
// class/campus message even when their scopes fan them out broadly.
internal object MessageOriginResolver {
    fun resolve(source: MessageFeedSource, scopes: List<MessageScopeEntity>): MessageFeedOrigin {
        if (source == MessageFeedSource.APP) return MessageFeedOrigin.APP
        if (scopes.isEmpty()) return MessageFeedOrigin.CAMPUS
        return scopes.map { mapScope(it.scope) }.minByOrNull { it.priority }?.origin
            ?: MessageFeedOrigin.CAMPUS
    }

    fun primaryDisciplineScope(scopes: List<MessageScopeEntity>): MessageScopeEntity? =
        scopes.firstOrNull { it.scope == "class" }

    private data class Ranked(val origin: MessageFeedOrigin, val priority: Int)

    private fun mapScope(raw: String): Ranked = when (raw) {
        "class" -> Ranked(MessageFeedOrigin.DISCIPLINE, 0)
        "personal" -> Ranked(MessageFeedOrigin.DIRECT, 1)
        "coordination" -> Ranked(MessageFeedOrigin.SECRETARIAT, 2)
        "course" -> Ranked(MessageFeedOrigin.CAMPUS, 3)
        "university" -> Ranked(MessageFeedOrigin.CAMPUS, 4)
        "list" -> Ranked(MessageFeedOrigin.APP, 5)
        else -> Ranked(MessageFeedOrigin.CAMPUS, 6)
    }
}

internal fun String.toMessageFeedSource(): MessageFeedSource = when (this) {
    "app" -> MessageFeedSource.APP
    else -> MessageFeedSource.UPSTREAM
}
