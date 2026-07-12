package dev.forcetower.melon.feature.messages.data.dto

import kotlinx.serialization.Serializable

// Bodies of the read/star device acks — POST /api/sync/messages/read and
// /api/sync/messages/star. Mirror `MarkReadBody`/`StarBody` in the iOS
// `MessagesRepository+Live.swift`.

@Serializable
internal data class MarkMessagesReadRequest(val ids: List<String>)

@Serializable
internal data class StarMessageRequest(val id: String, val starred: Boolean)
