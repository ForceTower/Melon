package dev.forcetower.melon.feature.disciplines.domain.model

// Full detail feed for a single DisciplineOffer — everything the detail view
// renders when the student taps a discipline from the list. Each list is
// pre-ordered the way the UI expects: groups by class type then name, sections
// one-per-class, grades by (evaluation.position, ordinal), lectures ascending
// by date, attachments newest-first.
// `lectures` carries the full lesson plan (placeholder rows filtered out);
// windowing/collapsing to the "interesting" slice around today is a
// presentation concern handled by the native side via `isPast`/`isCurrent`.
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
    // LIVE while the Prova Final is pending — never infer a verdict from it.
    val finalGrade: Double?,
    val approved: Boolean?,
    // Upstream "Em prova final" marker on any of the offer's StudentClass rows.
    val wentToFinals: Boolean,
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

// One grade section per discipline. Upstream exposes evaluations at the
// discipline level — the backend replicates them per class for schema
// reasons (see `applyDiscipline`), so the use case dedupes by the upstream
// grade id and emits a single section. `classId` / `groupName` stay null
// for the merged section so the UI renders it regardless of which group
// pill is selected.
data class DisciplineDetailSection(
    val classId: String?,
    val kind: String,
    val groupName: String?,
    val grades: List<DisciplineDetailGrade>,
)

// One evaluation slot for a student. `value` / `weight` are parsed — upstream
// stores them as strings with comma decimals which the use case normalizes.
// `gradeName` is the raw upstream grade name ("Prova Final" etc.) — the native
// side needs it verbatim to pull the final-exam row out of the regular list
// (name "Prova Final" + short "Adicional", never inferred from the value).
data class DisciplineDetailGrade(
    val evaluationId: String,
    val evaluationName: String?,
    val gradeName: String,
    val gradeNameShort: String?,
    val position: Int,
    val ordinal: Int,
    val weight: Double?,
    val value: Double?,
    val dateIso: String?,
)

// One ClassLecture row. `situation` is the upstream attendance code (0 = present).
// `attachmentCount` matches the number of LectureMaterial rows for this lecture
// and drives the paperclip indicator on the classes timeline. `isPast` and
// `isCurrent` are classified by the use case against `Clock.System.now()` —
// exposing them as flags keeps the native clients from having to agree on a
// clock (iOS's `DisciplineDate.today` is pinned for fixtures, for instance).
data class DisciplineDetailLecture(
    val lectureId: String,
    val classId: String,
    val ordinal: Int,
    val situation: Int,
    val dateIso: String?,
    val subject: String?,
    val attachmentCount: Int,
    val isPast: Boolean,
    val isCurrent: Boolean,
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
