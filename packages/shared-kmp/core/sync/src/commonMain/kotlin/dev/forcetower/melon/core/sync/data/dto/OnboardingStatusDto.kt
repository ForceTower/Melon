package dev.forcetower.melon.core.sync.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingStatusResponse(
    val courseLinked: Boolean,
    // Phase 1 of the two-phase onboarding split. Nullable so a fresh
    // client talking to a pre-split server still deserializes — the
    // mapper falls back to activeSemesterReady to synthesize a state.
    val initial: OnboardingInitialStatusDto? = null,
    val semesters: OnboardingSemestersStatusDto,
    val messages: OnboardingMessagesStatusDto,
    // Defaulted for resilience against older server builds that predate the
    // field; pre-field servers behave as if nothing is ready, which matches
    // the gate's wait-and-retry behavior.
    val activeSemesterReady: Boolean = false,
)

@Serializable
internal data class OnboardingInitialStatusDto(
    // pending | running | done | failed
    val status: String,
    // Count of distinct semesters the student has synced class enrollments
    // in. The iOS `grades` step gates advancement on this being > 0.
    // Defaulted for resilience against older server builds (pre-field).
    val appliedSemesters: Int = 0,
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
