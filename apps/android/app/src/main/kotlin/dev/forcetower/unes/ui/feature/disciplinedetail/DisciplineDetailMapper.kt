package dev.forcetower.unes.ui.feature.disciplinedetail

import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetail as KmpDetail
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailAttachment as KmpAttachment
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailGrade as KmpGrade
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailGroup as KmpGroup
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailLecture as KmpLecture
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplineDetailSection as KmpSection
import dev.forcetower.unes.ui.feature.disciplines.Attachment
import dev.forcetower.unes.ui.feature.disciplines.AttachmentKind
import dev.forcetower.unes.ui.feature.disciplines.ClassEntry
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineDateFormatting
import dev.forcetower.unes.ui.feature.disciplines.DisciplineGroup
import dev.forcetower.unes.ui.feature.disciplines.GradeEntry
import dev.forcetower.unes.ui.feature.disciplines.GradeSection

// KMP `DisciplineDetail` → presentation `Discipline`. Mirrors iOS
// `DisciplineDetailViewModel.map(detail:seed:)` so both clients land on the
// same projection. Color is left as `Color.Unspecified` here; the screen
// resolves it against the live palette via `ColorFor.discipline`.
internal fun mapDetail(raw: KmpDetail, seed: Discipline?): Discipline {
    val hasMultipleGroups = raw.groups.size > 1
    val classIdToGroupName = raw.groups.associate { it.classId to it.code }
    val groups = raw.groups.map(::mapGroup)
    val sections = raw.sections.map(::mapSection)
    val classes = raw.lectures.map(::mapLecture)
    val attachments = raw.attachments.map { att ->
        mapAttachment(
            att,
            showGroup = hasMultipleGroups,
            resolvedGroupName = classIdToGroupName[att.classId],
        )
    }

    return Discipline(
        code = raw.code,
        fullCode = raw.code,
        title = raw.name,
        dept = raw.department ?: seed?.dept.orEmpty(),
        prof = primaryProf(raw.groups) ?: seed?.prof.orEmpty(),
        // Resolved to a palette color in the composable that consumes this.
        color = androidx.compose.ui.graphics.Color.Unspecified,
        hours = raw.hours,
        absences = raw.missedHours,
        allowedAbsences = raw.allowedMissedHours,
        sections = sections,
        classes = classes,
        attachments = attachments,
        ementa = raw.ementa,
        groups = groups,
        finalGrade = raw.finalGrade,
        disciplineId = raw.disciplineId,
        offerId = raw.offerId,
        semesterId = raw.semesterId,
    )
}

private fun mapGroup(raw: KmpGroup) = DisciplineGroup(
    code = raw.code,
    kind = kindLabel(raw.kind),
    prof = raw.teacherName.orEmpty(),
)

// KMP emits a single merged section per discipline (groupName null, kind ""
// on multi-group). The UI's section header reads `name`, so a blank kind
// falls back to "Notas".
private fun mapSection(raw: KmpSection) = GradeSection(
    name = if (raw.kind.isEmpty()) "Notas" else kindLabel(raw.kind),
    group = raw.groupName,
    grades = raw.grades.map(::mapGrade),
)

private fun mapGrade(raw: KmpGrade) = GradeEntry(
    label = raw.gradeNameShort.orEmpty(),
    title = raw.evaluationName.orEmpty(),
    date = DisciplineDateFormatting.ddMmYyyy(raw.dateIso),
    score = raw.value,
)

private fun mapLecture(raw: KmpLecture) = ClassEntry(
    date = DisciplineDateFormatting.ddMmYyyy(raw.dateIso),
    title = raw.subject.orEmpty(),
    attachments = raw.attachmentCount.takeIf { it > 0 },
    past = raw.isPast,
    isNext = raw.isCurrent,
)

private fun mapAttachment(
    raw: KmpAttachment,
    showGroup: Boolean,
    resolvedGroupName: String?,
): Attachment {
    val caption = raw.caption?.takeIf { it.isNotEmpty() }
    return Attachment(
        name = caption ?: friendlyName(raw.url),
        kind = inferKind(raw.url),
        added = DisciplineDateFormatting.ddMm(raw.lectureDateIso).orEmpty(),
        group = if (showGroup) (resolvedGroupName ?: raw.groupName) else null,
        url = raw.url,
    )
}

private fun primaryProf(groups: List<KmpGroup>): String? =
    groups.asSequence().mapNotNull { it.teacherName }.firstOrNull { it.isNotEmpty() }

private fun kindLabel(raw: String): String = when (raw.uppercase()) {
    "TEO", "TEORICA", "TEÓRICA" -> "Teórica"
    "PRA", "PRATICA", "PRÁTICA" -> "Prática"
    "LAB", "LABORATORIO", "LABORATÓRIO" -> "Laboratório"
    else -> raw
}

private fun inferKind(url: String): AttachmentKind {
    val lower = url.lowercase()
    return when {
        lower.endsWith(".pdf") -> AttachmentKind.Pdf
        lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".key") -> AttachmentKind.Slides
        lower.endsWith(".md") || lower.endsWith(".txt") -> AttachmentKind.Notes
        lower.startsWith("http://") || lower.startsWith("https://") -> AttachmentKind.Link
        else -> AttachmentKind.Other
    }
}

private fun friendlyName(url: String): String {
    val trimmed = url.substringBefore('?').substringBefore('#')
    val last = trimmed.substringAfterLast('/', missingDelimiterValue = "")
    return last.ifEmpty { url }
}
