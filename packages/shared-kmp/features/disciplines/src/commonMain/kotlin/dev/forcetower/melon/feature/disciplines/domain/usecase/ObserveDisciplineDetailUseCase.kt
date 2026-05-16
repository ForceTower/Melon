package dev.forcetower.melon.feature.disciplines.domain.usecase

import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.query.DisciplineDetailEnrollmentRow
import dev.forcetower.melon.core.database.query.DisciplineDetailGradeRow
import dev.forcetower.melon.core.database.query.DisciplineDetailLectureRow
import dev.forcetower.melon.core.database.query.DisciplineDetailMaterialRow
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetail
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailAttachment
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailGrade
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailGroup
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailLecture
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailSection
import dev.zacsweers.metro.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Reactive detail feed for the `DisciplineDetailView`. Composes four scoped
// DAO flows — enrollments (class/group rows), grades, lectures, attachments —
// and groups them in Kotlin so the SQL stays flat and the detail payload lands
// in one emission. Mirrors `ObserveDisciplinesListUseCase`'s shape: fan-in via
// `combine`, distinct emissions, aggregation in memory.
//
// Emits `null` when the offer has no enrollment rows — which happens in the
// gap between wiping and re-inserting a semester during sync. The native
// layer keeps the seed discipline on the screen while it waits for the next
// non-null emission.
@Inject
class ObserveDisciplineDetailUseCase internal constructor(
    private val academicDao: AcademicDao,
) {
    operator fun invoke(offerId: String): Flow<DisciplineDetail?> = combine(
        academicDao.observeDisciplineOfferEnrollments(offerId),
        academicDao.observeDisciplineOfferGrades(offerId),
        academicDao.observeDisciplineOfferLectures(offerId),
        academicDao.observeDisciplineOfferMaterials(offerId),
    ) { enrollments, grades, lectures, materials ->
        if (enrollments.isEmpty()) null
        else build(enrollments, grades, lectures, materials)
    }.distinctUntilChanged()
}

private fun build(
    enrollments: List<DisciplineDetailEnrollmentRow>,
    grades: List<DisciplineDetailGradeRow>,
    lectures: List<DisciplineDetailLectureRow>,
    materials: List<DisciplineDetailMaterialRow>,
): DisciplineDetail {
    val head = enrollments.first()

    // Deduplicate by classId while preserving the SQL ordering (type, groupName).
    val classRows = enrollments.distinctBy { it.classId }

    // Upstream repeats the discipline's nominal `cargaHoraria` on every class
    // group (verified against the legacy DB: multi-group disciplines like
    // FIS110 carry the same 90h on each class row). Summing would double-count
    // multi-group disciplines, so read straight from the offer/catalog hours.
    val hours = head.offerHours ?: head.disciplineHours
    // `missedClasses` is also a discipline-level value replicated onto every
    // StudentClass row by `applyResult`; take the first non-null instead of
    // summing so multi-group disciplines don't double-count.
    val missedHours = enrollments.firstNotNullOfOrNull { it.missedClasses } ?: 0
    val allowedMissedHours = ceil(hours * 0.25).toInt()
    val finalGradeString = enrollments.firstOrNull { !it.finalGrade.isNullOrBlank() }?.finalGrade
    val finalGrade = finalGradeString?.replace(",", ".")?.toDoubleOrNull()
    val approved = enrollments.firstOrNull { it.approved != null }?.approved

    val groups = classRows.map { row ->
        DisciplineDetailGroup(
            classId = row.classId,
            code = row.groupName,
            kind = row.classType,
            teacherName = row.teacherName,
        )
    }

    // Dedup grades by the upstream id (`gradePlatformId`). Backend writes the
    // same grade set once per class row — see `applyDiscipline` — but upstream
    // exposes a single shared list. Collapse back to that single list so the
    // UI doesn't show N copies of every grade on multi-group disciplines.
    // `distinctBy` keeps the first occurrence, and SQL already orders by
    // (classId, evaluation.position, ordinal), so "first" lands on the
    // alphabetically-first class's copy — arbitrary but consistent.
    val dedupedGrades = grades
        .distinctBy { it.gradePlatformId }
        .sortedWith(compareBy({ it.evaluationPosition }, { it.ordinal }))

    // One merged section; null classId / groupName make it render across
    // every group pill (iOS filters `section.group == selected || == nil`).
    // Multi-group: `kind` stays blank so the section header reads as plain
    // "NOTAS" rather than picking one group's label arbitrarily.
    val sectionKind = if (classRows.size == 1) classRows.first().classType else ""
    val sections = if (dedupedGrades.isEmpty()) {
        emptyList()
    } else {
        listOf(
            DisciplineDetailSection(
                classId = null,
                kind = sectionKind,
                groupName = null,
                grades = dedupedGrades.map(::toDetailGrade),
            ),
        )
    }

    val classIdToGroupName = classRows.associate { it.classId to it.groupName }

    // Native side needs a narrow window of "interesting" lectures, not the
    // full semester dump. Filter out placeholder rows (no subject and no
    // materials — upstream emits these for cancelled / TBD slots), then
    // window to 1 past + 1 current + 3 upcoming around today so both native
    // targets render the same slice without duplicating the logic.
    // `isPast` / `isCurrent` are classified here against real today so the
    // native layer doesn't need its own clock to agree with ours.
    val today = Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
    val filteredRows = lectures
        .filter { !it.subject.isNullOrBlank() || it.attachmentCount > 0 }
    val anchorIndex = filteredRows.indexOfFirst {
        val d = it.date
        d != null && d >= today
    }
    val mappedLectures = filteredRows
        .mapIndexed { idx, row ->
            val date = row.date
            DisciplineDetailLecture(
                lectureId = row.lectureId,
                classId = row.classId,
                ordinal = row.ordinal,
                situation = row.situation,
                dateIso = date,
                subject = row.subject,
                attachmentCount = row.attachmentCount,
                isPast = date != null && date < today,
                isCurrent = idx == anchorIndex,
            )
        }
        .let { windowAroundToday(it, anchorIndex) }

    val mappedAttachments = materials.map { row ->
        DisciplineDetailAttachment(
            materialId = row.materialId,
            classId = row.classId,
            groupName = classIdToGroupName[row.classId],
            caption = row.caption,
            url = row.url,
            lectureDateIso = row.lectureDate,
        )
    }

    return DisciplineDetail(
        offerId = head.offerId,
        semesterId = head.semesterId,
        disciplineId = head.disciplineId,
        code = head.disciplineCode,
        name = head.disciplineName,
        department = head.department,
        ementa = head.disciplineProgram,
        hours = hours,
        missedHours = missedHours,
        allowedMissedHours = allowedMissedHours,
        finalGrade = finalGrade,
        approved = approved,
        groups = groups,
        sections = sections,
        lectures = mappedLectures,
        attachments = mappedAttachments,
    )
}

// Returns at most 5 lectures: 1 past + the "current" (anchor) + 3 upcoming.
// If every dated lecture is already in the past (anchor < 0) we fall back to
// the 5 most recent entries. `lectures` must already be ordered by
// (date ASC, ordinal ASC) — the DAO query guarantees this.
internal fun windowAroundToday(
    lectures: List<DisciplineDetailLecture>,
    anchor: Int,
): List<DisciplineDetailLecture> {
    if (lectures.size <= 5) return lectures
    if (anchor < 0) return lectures.takeLast(5)
    val start = (anchor - 1).coerceAtLeast(0)
    val end = (anchor + 4).coerceAtMost(lectures.size)
    return lectures.subList(start, end)
}

private fun toDetailGrade(row: DisciplineDetailGradeRow): DisciplineDetailGrade =
    DisciplineDetailGrade(
        evaluationId = row.evaluationId,
        evaluationName = row.evaluationName ?: row.gradeName,
        gradeNameShort = row.gradeNameShort,
        position = row.evaluationPosition,
        ordinal = row.ordinal,
        weight = row.weight.replace(",", ".").toDoubleOrNull(),
        value = row.value?.takeIf { it.isNotBlank() }?.replace(",", ".")?.toDoubleOrNull(),
        dateIso = row.date,
    )
