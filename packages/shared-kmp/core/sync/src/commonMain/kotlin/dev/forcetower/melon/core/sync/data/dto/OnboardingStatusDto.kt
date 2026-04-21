package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingStatusResponse(
    val courseLinked: Boolean,
    val semesters: OnboardingSemestersStatusDto,
    val messages: OnboardingMessagesStatusDto,
    // Defaulted for resilience against older server builds that predate the
    // field; pre-field servers behave as if nothing is ready, which matches
    // the gate's wait-and-retry behavior.
    val activeSemesterReady: Boolean = false,
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
