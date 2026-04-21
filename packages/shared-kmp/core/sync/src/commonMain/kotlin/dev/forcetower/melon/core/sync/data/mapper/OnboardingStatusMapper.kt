package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.sync.data.dto.OnboardingMessagesStatusDto
import dev.forcetower.melon.core.sync.data.dto.OnboardingSemestersStatusDto
import dev.forcetower.melon.core.sync.data.dto.OnboardingStatusResponse
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus

internal fun OnboardingStatusResponse.toDomain(): OnboardingStatus = OnboardingStatus(
    courseLinked = courseLinked,
    semesters = semesters.toDomain(),
    messages = messages.toDomain(),
    activeSemesterReady = activeSemesterReady,
)

private fun OnboardingSemestersStatusDto.toDomain(): OnboardingStatus.PhaseStatus =
    OnboardingStatus.PhaseStatus(
        state = parseState(status),
        total = total,
        done = done,
        failed = failed,
    )

private fun OnboardingMessagesStatusDto.toDomain(): OnboardingStatus.PhaseStatus =
    OnboardingStatus.PhaseStatus(state = parseState(status))

private fun parseState(raw: String): OnboardingStatus.PhaseStatus.State = when (raw) {
    "pending" -> OnboardingStatus.PhaseStatus.State.Pending
    "running" -> OnboardingStatus.PhaseStatus.State.Running
    "done" -> OnboardingStatus.PhaseStatus.State.Done
    "partial" -> OnboardingStatus.PhaseStatus.State.Partial
    "failed" -> OnboardingStatus.PhaseStatus.State.Failed
    else -> OnboardingStatus.PhaseStatus.State.Unknown
}
