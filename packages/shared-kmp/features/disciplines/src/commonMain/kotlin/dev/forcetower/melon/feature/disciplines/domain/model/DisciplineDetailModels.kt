package dev.forcetower.melon.feature.disciplines.domain.model

// Full detail feed for a single DisciplineOffer — everything the detail view
// renders when the student taps a discipline from the list. Each list is
// pre-ordered the way the UI expects: groups by class type then name, sections
// one-per-class, grades by (evaluation.position, ordinal), lectures ascending
// by date, attachments newest-first.
data class DisciplineDetail(
    val offerId: String,
    val semesterId: String,
    val disciplineId: String,
    val code: String,
    val name: String,
    val department: String?,
    // Upstream `ementa`, stored under `Discipline.program`. Null when the
    // syllabus hasn't been synced / isn't set on the curriculum.
    val ementa: String?,
    val hours: Int,
    val missedHours: Int,
    // Derived `ceil(hours * 0.25)` — matches the list-view rule.
    val allowedMissedHours: Int,
    val finalGrade: Double?,
    val approved: Boolean?,
    val groups: List<DisciplineDetailGroup>,
    val sections: List<DisciplineDetailSection>,
    val lectures: List<DisciplineDetailLecture>,
    val attachments: List<DisciplineDetailAttachment>,
)

// One per Class (group) under the offer. `kind` is the upstream class.type
// code ("TEO"/"PRA"/"LAB" etc.) — the human label is a presentation concern
// and happens on the native side.
data class DisciplineDetailGroup(
    val classId: String,
    val code: String,
    val kind: String,
    val teacherName: String?,
)

// One grade section per class. `groupName` lets the view show a group badge
// on each section when the discipline has multiple groups.
data class DisciplineDetailSection(
    val classId: String,
    val kind: String,
    val groupName: String,
    val grades: List<DisciplineDetailGrade>,
)

// One evaluation slot for a student. `value` / `weight` are parsed — upstream
// stores them as strings with comma decimals which the use case normalizes.
data class DisciplineDetailGrade(
    val evaluationId: String,
    val evaluationName: String?,
    val gradeNameShort: String?,
    val position: Int,
    val ordinal: Int,
    val weight: Double?,
    val value: Double?,
    val dateIso: String?,
)

// One ClassLecture row. `situation` is the upstream attendance code (0 = present).
// `attachmentCount` matches the number of LectureMaterial rows for this lecture
// and drives the paperclip indicator on the classes timeline.
data class DisciplineDetailLecture(
    val lectureId: String,
    val classId: String,
    val ordinal: Int,
    val situation: Int,
    val dateIso: String?,
    val subject: String?,
    val attachmentCount: Int,
)

// One LectureMaterial row with its owning class/group resolved. The native
// side infers AttachmentKind from the URL extension. `caption` is the
// upstream `description` — renamed here to avoid clashing with
// `NSObject.description` on the SKIE-generated Swift API.
data class DisciplineDetailAttachment(
    val materialId: String,
    val classId: String,
    // Class's groupName, null when the discipline runs a single group.
    val groupName: String?,
    val caption: String?,
    val url: String,
    val lectureDateIso: String?,
)
