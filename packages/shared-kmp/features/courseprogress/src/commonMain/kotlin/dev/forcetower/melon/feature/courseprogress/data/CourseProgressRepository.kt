package dev.forcetower.melon.feature.courseprogress.data

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.database.dao.CurriculumDao
import dev.forcetower.melon.core.database.entity.CurriculumEntity
import dev.forcetower.melon.core.database.entity.CurriculumEntryEntity
import dev.forcetower.melon.core.database.entity.CurriculumPrerequisiteEntity
import dev.forcetower.melon.core.database.entity.CurriculumProgressEntity
import dev.forcetower.melon.core.database.entity.CurriculumRequirementEntity
import dev.forcetower.melon.feature.courseprogress.data.network.CurriculumService
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgressError
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumBindingSource
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumPeriod
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementKind
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumSummary
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumSupersession
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumVersion
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// The curriculum mirror, read side and write side. `observe` projects the Room
// tables into one `CourseProgress`, so the screens render offline and every
// refresh lands through the same stream; `refresh` pulls `api/curriculum` and
// replaces the mirror in a single transaction. `selectVersion` / `resetVersion`
// re-bind the student server-side and mirror the rebuilt payload the same
// way. The Android analogue of iOS `MirrorStore+CourseProgress` +
// `CourseProgressRepository+Live`.
@SingleIn(AppScope::class)
@Inject
class CourseProgressRepository internal constructor(
    private val service: CurriculumService,
    private val dao: CurriculumDao,
) {
    // Null until the first successful refresh lands (and again after logout).
    fun observe(): Flow<CourseProgress?> = combine(
        dao.observeProgress(),
        dao.observeVersions(),
        dao.observeRequirements(),
        dao.observeEntries(),
        dao.observePrerequisites(),
    ) { progress, versions, requirements, entries, prerequisites ->
        progress?.project(versions, requirements, entries, prerequisites)
    }.distinctUntilChanged()

    // Failures leave the mirrored payload alone — a stale screen beats an
    // error screen, and the caller decides whether to narrate the failure.
    suspend fun refresh(): Outcome<Unit, CourseProgressError> =
        mirror { service.curriculum(Clock.System.now()) }

    // Binds the student to one of `availableVersions` by hand; the screen
    // re-renders from the rebuilt payload.
    suspend fun selectVersion(curriculumId: String): Outcome<Unit, CourseProgressError> =
        mirror { service.selectVersion(curriculumId, Clock.System.now()) }

    // Hands the binding back to the server's own resolution.
    suspend fun resetVersion(): Outcome<Unit, CourseProgressError> =
        mirror { service.resetVersion(Clock.System.now()) }

    // Every write path answers with the whole payload, so fetching and
    // mirroring are the same move for a refresh, a pick and a reset.
    private suspend inline fun mirror(
        fetch: () -> Outcome<CourseProgress, CourseProgressError>,
    ): Outcome<Unit, CourseProgressError> = when (val outcome = fetch()) {
        is Outcome.Ok -> {
            apply(outcome.value)
            Outcome.Ok(Unit)
        }
        is Outcome.Err -> Outcome.Err(outcome.error)
    }

    private suspend fun apply(progress: CourseProgress) {
        val curriculum = progress.curriculum
        val curriculumId = curriculum?.id
        val requirements = mutableListOf<CurriculumRequirementEntity>()
        val entries = mutableListOf<CurriculumEntryEntity>()
        val prerequisites = mutableListOf<CurriculumPrerequisiteEntity>()

        if (curriculumId != null) {
            progress.requirements.forEachIndexed { position, requirement ->
                requirements += requirement.toEntity(curriculumId, position)
            }
            // One running counter across every período: the payload's order is
            // the render order, and it already groups each período together.
            var position = 0
            progress.periods.forEach { period ->
                period.entries.forEach { entry ->
                    entries += entry.toEntity(curriculumId, position)
                    position += 1
                    prerequisites += entry.edgeEntities(curriculumId)
                }
            }
        }

        // The server lists the bound version among the alternatives; should
        // it ever not, it still has to be readable back.
        val versions = buildList {
            if (curriculum != null && progress.availableVersions.none { it.id == curriculum.id }) {
                add(curriculum)
            }
            addAll(progress.availableVersions)
        }

        dao.replace(
            progress = progress.toProgressEntity(curriculumId),
            versions = versions.mapIndexed { position, version -> version.toEntity(position) },
            requirements = requirements,
            entries = entries,
            prerequisites = prerequisites,
        )
    }
}

// ───────── domain → mirror ─────────

private fun CourseProgress.toProgressEntity(curriculumId: String?) = CurriculumProgressEntity(
    curriculumId = curriculumId,
    completedHours = summary.completedHours,
    requiredHours = summary.requiredHours,
    percent = summary.percent,
    excludedHours = summary.excludedHours,
    unclassifiedHours = summary.unclassifiedHours,
    disciplinesCompleted = summary.disciplinesCompleted,
    disciplinesTotal = summary.disciplinesTotal,
    currentPeriod = currentPeriod,
    prerequisitesKnown = prerequisitesKnown,
    syncedAt = syncedAt.toEpochMilliseconds(),
    approvedHours = approvedHours,
)

private fun CurriculumVersion.toEntity(position: Int) = CurriculumEntity(
    id = id,
    code = code,
    label = label,
    asOf = asOf,
    minPeriods = minPeriods,
    maxPeriods = maxPeriods,
    stale = stale,
    current = current,
    supersededByCode = supersededBy?.code,
    supersededByEffectiveFrom = supersededBy?.effectiveFrom,
    source = source?.wire,
    completedHours = completedHours,
    requiredHours = requiredHours,
    percent = percent,
    fit = fit,
    position = position,
)

private fun CurriculumRequirementProgress.toEntity(curriculumId: String, position: Int) =
    CurriculumRequirementEntity(
        curriculumId = curriculumId,
        code = code,
        kind = kind.wire,
        label = label,
        shortLabel = shortLabel,
        startsAtPeriod = startsAtPeriod,
        hoursRequired = hoursRequired,
        hoursCompleted = hoursCompleted,
        derivable = derivable,
        percent = percent,
        position = position,
    )

private fun CurriculumEntry.toEntity(curriculumId: String, position: Int) = CurriculumEntryEntity(
    curriculumId = curriculumId,
    code = code,
    name = name,
    hours = hours,
    credits = credits,
    period = period,
    coreqGroup = coreqGroup,
    requirementCode = requirementCode,
    status = status.wire,
    position = position,
)

private fun CurriculumEntry.edgeEntities(curriculumId: String): List<CurriculumPrerequisiteEntity> =
    prerequisites.mapIndexed { index, required ->
        CurriculumPrerequisiteEntity(
            curriculumId = curriculumId,
            entryCode = code,
            requiresCode = required,
            kind = CurriculumPrerequisiteEntity.KIND_PREREQUISITE,
            position = index,
        )
    } + corequisites.mapIndexed { index, alongside ->
        CurriculumPrerequisiteEntity(
            curriculumId = curriculumId,
            entryCode = code,
            requiresCode = alongside,
            kind = CurriculumPrerequisiteEntity.KIND_COREQUISITE,
            position = index,
        )
    }

// ───────── mirror → domain ─────────

private fun CurriculumProgressEntity.project(
    versionRows: List<CurriculumEntity>,
    requirementRows: List<CurriculumRequirementEntity>,
    entryRows: List<CurriculumEntryEntity>,
    edgeRows: List<CurriculumPrerequisiteEntity>,
): CourseProgress {
    val summary = CurriculumSummary(
        completedHours = completedHours,
        requiredHours = requiredHours,
        percent = percent,
        excludedHours = excludedHours,
        unclassifiedHours = unclassifiedHours,
        disciplinesCompleted = disciplinesCompleted,
        disciplinesTotal = disciplinesTotal,
    )
    val versions = versionRows.map { it.toDomain() }
    val version = versions.firstOrNull { it.id == curriculumId }
    if (version == null) {
        return CourseProgress(
            curriculum = null,
            summary = summary,
            requirements = emptyList(),
            periods = emptyList(),
            currentPeriod = currentPeriod,
            prerequisitesKnown = prerequisitesKnown,
            syncedAt = Instant.fromEpochMilliseconds(syncedAt),
            availableVersions = versions,
            approvedHours = approvedHours,
        )
    }

    val prerequisitesByEntry = mutableMapOf<String, MutableList<String>>()
    val corequisitesByEntry = mutableMapOf<String, MutableList<String>>()
    edgeRows.forEach { edge ->
        val target = if (edge.kind == CurriculumPrerequisiteEntity.KIND_COREQUISITE) {
            corequisitesByEntry
        } else {
            prerequisitesByEntry
        }
        target.getOrPut(edge.entryCode) { mutableListOf() }.add(edge.requiresCode)
    }

    // Row order already groups a período's entries together; the pool (null
    // período) sorts last.
    val periods = entryRows
        .map { row ->
            CurriculumEntry(
                code = row.code,
                name = row.name,
                hours = row.hours,
                credits = row.credits,
                period = row.period,
                coreqGroup = row.coreqGroup,
                requirementCode = row.requirementCode,
                status = CurriculumEntryStatus.fromWire(row.status),
                prerequisites = prerequisitesByEntry[row.code].orEmpty(),
                corequisites = corequisitesByEntry[row.code].orEmpty(),
            )
        }
        .groupBy { it.period }
        .map { (period, entries) -> CurriculumPeriod(period = period, entries = entries) }
        .sortedBy { it.sortKey }

    return CourseProgress(
        curriculum = version,
        summary = summary,
        requirements = requirementRows.map { row ->
            CurriculumRequirementProgress(
                code = row.code,
                kind = CurriculumRequirementKind.fromWire(row.kind),
                label = row.label,
                shortLabel = row.shortLabel,
                startsAtPeriod = row.startsAtPeriod,
                hoursRequired = row.hoursRequired,
                hoursCompleted = row.hoursCompleted,
                derivable = row.derivable,
                percent = row.percent,
            )
        },
        periods = periods,
        currentPeriod = currentPeriod,
        prerequisitesKnown = prerequisitesKnown,
        syncedAt = Instant.fromEpochMilliseconds(syncedAt),
        availableVersions = versions,
        approvedHours = approvedHours,
    )
}

private fun CurriculumEntity.toDomain() = CurriculumVersion(
    id = id,
    code = code,
    label = label,
    asOf = asOf,
    minPeriods = minPeriods,
    maxPeriods = maxPeriods,
    stale = stale,
    current = current,
    supersededBy = supersededByCode?.let {
        CurriculumSupersession(code = it, effectiveFrom = supersededByEffectiveFrom)
    },
    source = CurriculumBindingSource.fromWire(source),
    completedHours = completedHours,
    requiredHours = requiredHours,
    percent = percent,
    fit = fit,
)
