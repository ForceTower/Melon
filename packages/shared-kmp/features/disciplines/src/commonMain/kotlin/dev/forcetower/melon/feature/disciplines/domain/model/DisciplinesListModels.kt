package dev.forcetower.melon.feature.disciplines.domain.model

// Snapshot of the Disciplinas screen: the one card-stack for the semester
// containing today, the collapsible history of fully-downloaded past
// semesters, and placeholder rows for semesters whose payload we haven't
// pulled yet. Every list is ordered the way the UI renders it.
data class DisciplinesListState(
    val current: SemesterDisciplines?,
    val past: List<SemesterDisciplines>,
    val pending: List<PendingSemester>,
)

data class SemesterDisciplines(
    val semesterId: String,
    val semesterCode: String,
    val disciplines: List<DisciplineListItem>,
)

// One entry per (semester, discipline). Multiple StudentClass rows — when a
// discipline runs as e.g. theory + practice — are merged into a single item:
// hours come from the catalog (upstream replicates them per group), absences
// are taken from any one row (also replicated by `applyResult`), grades dedup
// by upstream id, teacher is the first non-null.
data class DisciplineListItem(
    val disciplineId: String,
    val offerId: String,
    val semesterId: String,
    val studentClassIds: List<String>,
    val code: String,
    val name: String,
    val department: String?,
    val teacherName: String?,
    val hours: Int,
    val missedHours: Int,
    val allowedMissedHours: Int,
    // Weighted mean of released grades, null when nothing is released.
    val partialAverage: Double?,
    // Parsed from the String upstream stores; null when not set. LIVE while
    // the Prova Final is pending — never infer a verdict from it alone.
    val finalGrade: Double?,
    val approved: Boolean?,
    // Upstream "Em prova final" marker on any of the offer's StudentClass rows.
    val wentToFinals: Boolean,
    val status: DisciplineStatusKind,
    // "Te · Pr" when the discipline has more than one group (class type);
    // null when the student is enrolled in a single group.
    val groupsLabel: String?,
    // Per-evaluation detail in natural semester order (by ordinal). Released
    // entries carry a numeric `value`; pending ones leave it null.
    val grades: List<ListGradeEntry>,
)

// Card-ready evaluation row. String fields are already parsed into Doubles so
// clients don't re-duplicate the comma/number fix-ups.
data class ListGradeEntry(
    val name: String,
    val nameShort: String?,
    // ISO yyyy-MM-dd; null when upstream hasn't scheduled it yet.
    val date: String?,
    // Parsed from the String upstream stores; null when unreleased or blank.
    val value: Double?,
    // Parsed weight; null when upstream sent a malformed value.
    val weight: Double?,
)

enum class DisciplineStatusKind {
    // No grade released yet. Still ongoing.
    PENDING,

    // Some grades released, partial average above the warning threshold.
    ONGOING,

    // Some grades released, partial average below the pass threshold.
    LOW,

    // Final grade released, student passed.
    APROVADO,

    // Final grade released, student failed outright.
    REPROVADO,

    // Final grade released but not yet at pass/fail cutoff — student is in
    // the final-exam window.
    FINAL,
}

// Semester known to exist for the student but whose payload has not been
// pulled. UI renders a "BAIXAR" card; tapping triggers SyncSemesterUseCase.
data class PendingSemester(
    val semesterId: String,
    val semesterCode: String,
)
