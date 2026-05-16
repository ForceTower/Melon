package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.sync.data.dto.OnboardingInitialStatusDto
import dev.forcetower.melon.core.sync.data.dto.OnboardingMessagesStatusDto
import dev.forcetower.melon.core.sync.data.dto.OnboardingSemestersStatusDto
import dev.forcetower.melon.core.sync.data.dto.OnboardingStatusResponse
import dev.forcetower.melon.core.sync.domain.model.OnboardingStatus

internal fun OnboardingStatusResponse.toDomain(): OnboardingStatus = OnboardingStatus(
    courseLinked = courseLinked,
    initial = initial?.toDomain() ?: fallbackInitialStatus(activeSemesterReady),
    semesters = semesters.toDomain(),
    messages = messages.toDomain(),
    activeSemesterReady = activeSemesterReady,
)

private fun OnboardingInitialStatusDto.toDomain(): OnboardingStatus.InitialStatus =
    OnboardingStatus.InitialStatus(
        state = parseState(status),
        appliedSemesters = appliedSemesters,
    )

// Pre-split servers don't send `initial`. Fall back to the derived
// activeSemesterReady flag so gating logic still makes a correct call:
// ready → treat Phase 1 as done; not ready → still waiting.
// appliedSemesters defaults to 0 on old servers; the gate will treat
// them as "not yet applied" and keep polling.
private fun fallbackInitialStatus(activeSemesterReady: Boolean): OnboardingStatus.InitialStatus =
    OnboardingStatus.InitialStatus(
        state = if (activeSemesterReady) {
            OnboardingStatus.PhaseStatus.State.Done
        } else {
            OnboardingStatus.PhaseStatus.State.Pending
        },
        appliedSemesters = 0,
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
