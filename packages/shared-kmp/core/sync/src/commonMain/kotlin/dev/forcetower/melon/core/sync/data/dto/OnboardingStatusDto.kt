package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingStatusResponse(
    val courseLinked: Boolean,
    val semesters: OnboardingSemestersStatusDto,
    val messages: OnboardingMessagesStatusDto,
)

@Serializable
internal data class OnboardingSemestersStatusDto(
    // pending | running | done | partial | failed
    val status: String,
    val total: Int,
    val done: Int,
    val failed: Int,
)

@Serializable
internal data class OnboardingMessagesStatusDto(
    // pending | running | done | failed (server omits "partial" for messages)
    val status: String,
)
