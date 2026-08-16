package dev.forcetower.melon.feature.courseprogress.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumPeriod
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementKind
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumSummary
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumVersion
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// `GET api/curriculum` — the whole progress payload in one shot: the version
// the student is bound to, the headline hours, the per-nature breakdown, and
// the full grid with its prerequisite edges. Mirrors iOS
// `CourseProgressRepository+Live.swift`.
//
// Everything past `summary` is optional on the wire: the portal can answer
// with hours and no grid (an unmapped curriculum) or with a grid and no
// breakdown, and both are screens the design has copy for.
@Inject
internal class CurriculumService(
    private val client: HttpClient,
) {
    suspend fun curriculum(syncedAt: Instant): Outcome<CourseProgress, CourseProgressError> {
        val payload = try {
            val response = client.get("api/curriculum")
            if (!response.status.isSuccess()) return Outcome.Err(CourseProgressError.Connection)
            response.body<ApiEnvelope<CurriculumPayloadDTO>>().takeIf { it.ok }?.data
        } catch (_: Exception) {
            null
        } ?: return Outcome.Err(CourseProgressError.Connection)
        return Outcome.Ok(payload.toDomain(syncedAt))
    }
}

@Serializable
internal data class CurriculumPayloadDTO(
    val curriculum: VersionDTO? = null,
    val summary: SummaryDTO,
    val requirements: List<RequirementDTO>? = null,
    val periods: List<PeriodDTO>? = null,
    val currentPeriod: Int? = null,
    val prerequisitesKnown: Boolean? = null,
) {
    @Serializable
    internal data class VersionDTO(
        val id: String,
        val code: String,
        val label: String,
        val asOf: String,
        val minPeriods: Int? = null,
        val maxPeriods: Int? = null,
        val stale: Boolean? = null,
    )

    @Serializable
    internal data class SummaryDTO(
        val completedHours: Int,
        val requiredHours: Int? = null,
        val percent: Double? = null,
        val excludedHours: Int? = null,
        val unclassifiedHours: Int? = null,
        val disciplinesCompleted: Int? = null,
        val disciplinesTotal: Int? = null,
    )

    @Serializable
    internal data class RequirementDTO(
        val code: String,
        val kind: String,
        val label: String,
        val shortLabel: String? = null,
        val startsAtPeriod: Int? = null,
        val hoursRequired: Int,
        val hoursCompleted: Int,
        val derivable: Boolean? = null,
        val percent: Double? = null,
    )

    @Serializable
    internal data class EntryDTO(
        val code: String,
        val name: String,
        val hours: Int,
        val credits: Int? = null,
        val period: Int? = null,
        val coreqGroup: Int? = null,
        val requirementCode: String? = null,
        val status: String,
        val prerequisites: List<String>? = null,
        val corequisites: List<String>? = null,
    )

    @Serializable
    internal data class PeriodDTO(
        val period: Int? = null,
        val entries: List<EntryDTO>,
    )
}

internal fun CurriculumPayloadDTO.toDomain(syncedAt: Instant) = CourseProgress(
    curriculum = curriculum?.toDomain(),
    summary = summary.toDomain(),
    requirements = requirements.orEmpty().map { it.toDomain() },
    periods = periods.orEmpty().map { it.toDomain() }.sortedBy { it.sortKey },
    currentPeriod = currentPeriod,
    prerequisitesKnown = prerequisitesKnown ?: false,
    syncedAt = syncedAt,
)

private fun CurriculumPayloadDTO.VersionDTO.toDomain() = CurriculumVersion(
    id = id,
    code = code,
    label = label,
    asOf = asOf,
    minPeriods = minPeriods,
    maxPeriods = maxPeriods,
    stale = stale ?: false,
)

private fun CurriculumPayloadDTO.SummaryDTO.toDomain() = CurriculumSummary(
    completedHours = completedHours,
    requiredHours = requiredHours,
    percent = percent,
    excludedHours = excludedHours ?: 0,
    unclassifiedHours = unclassifiedHours ?: 0,
    disciplinesCompleted = disciplinesCompleted ?: 0,
    disciplinesTotal = disciplinesTotal ?: 0,
)

private fun CurriculumPayloadDTO.RequirementDTO.toDomain() = CurriculumRequirementProgress(
    code = code,
    kind = CurriculumRequirementKind.fromWire(kind),
    label = label,
    shortLabel = shortLabel ?: label,
    startsAtPeriod = startsAtPeriod,
    hoursRequired = hoursRequired,
    hoursCompleted = hoursCompleted,
    derivable = derivable ?: true,
    percent = percent,
)

private fun CurriculumPayloadDTO.PeriodDTO.toDomain() = CurriculumPeriod(
    period = period,
    entries = entries.map { it.toDomain() },
)

private fun CurriculumPayloadDTO.EntryDTO.toDomain() = CurriculumEntry(
    code = code,
    name = name,
    hours = hours,
    credits = credits,
    period = period,
    coreqGroup = coreqGroup,
    requirementCode = requirementCode,
    status = CurriculumEntryStatus.fromWire(status),
    prerequisites = prerequisites.orEmpty(),
    corequisites = corequisites.orEmpty(),
)
