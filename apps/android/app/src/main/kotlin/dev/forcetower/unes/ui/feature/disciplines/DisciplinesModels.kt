package dev.forcetower.unes.ui.feature.disciplines

import androidx.compose.ui.graphics.Color

// UI projection types for the Disciplinas screen. Mirrors iOS
// `DisciplineModels.swift` — same field names, same derived helpers — so the
// list/detail components can read either platform's payload without renaming.
// Built from the KMP `DisciplinesListState` / `DisciplineDetail` flow output
// in `DisciplinesListViewModel.kt`.

internal data class GradeEntry(
    val label: String,
    val title: String,
    // dd/MM/yyyy. Null when upstream hasn't scheduled the evaluation yet.
    val date: String?,
    val score: Double?,
)

internal data class GradeSection(
    val name: String,
    val group: String? = null,
    val grades: List<GradeEntry>,
)

internal data class DisciplineGroup(
    val code: String,
    val kind: String,
    val prof: String,
)

// One row on the detail screen's classes timeline. `past`/`isNext` are
// classified upstream by `ObserveDisciplineDetailUseCase` against real
// today, so the UI doesn't need its own clock.
internal data class ClassEntry(
    val date: String?,
    val title: String,
    // Null when upstream doesn't expose the count; 0 hides the paperclip,
    // any positive value shows the badge.
    val attachments: Int?,
    val past: Boolean,
    val isNext: Boolean = false,
)

internal enum class AttachmentKind {
    Pdf, Slides, Link, Notes, Other;

    val label: String
        get() = when (this) {
            Pdf -> "PDF"
            Slides -> "SLIDES"
            Link -> "LINK"
            Notes -> "NOTES"
            Other -> "FILE"
        }
}

internal data class Attachment(
    val name: String,
    val kind: AttachmentKind,
    val added: String,
    // Class group name (e.g. "T01"). Null on single-group disciplines.
    val group: String?,
    val url: String? = null,
)

// One discipline card. `color` is resolved via `ColorFor.discipline(code)` so
// the same code lands on the same tint across Overview, Schedule, and the
// Disciplinas screens.
internal data class Discipline(
    val code: String,
    val fullCode: String,
    val title: String,
    val dept: String,
    val prof: String,
    val color: Color,
    val hours: Int,
    val absences: Int,
    val allowedAbsences: Int,
    val sections: List<GradeSection>,
    val classes: List<ClassEntry> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val ementa: String? = null,
    val groups: List<DisciplineGroup> = emptyList(),
    val finalGrade: Double? = null,
    // Authoritative pass/fail flag from upstream — set once the semester
    // closes (after finals). When null the discipline is still in progress
    // (or upstream simply hasn't recorded a result), and `status` falls back
    // to grade thresholds.
    val approved: Boolean? = null,
    // Weighted partial average computed upstream. When null, `partialAverage`
    // falls back to the unweighted mean of released scores.
    val storedPartialAverage: Double? = null,
    // Keys carried for the detail-view handoff. Populated when the data
    // originates from the KMP feed.
    val disciplineId: String? = null,
    val offerId: String? = null,
    val semesterId: String? = null,
)

internal data class Semester(
    // Display code, e.g. "2026.1".
    val id: String,
    val disciplines: List<Discipline> = emptyList(),
    val isDownloaded: Boolean = true,
    val estimatedCount: Int? = null,
    // Opaque DB primary key used by SyncSemesterUseCase.
    val dbSemesterId: String? = null,
)

internal enum class AbsenceRisk { Ok, Warn, Risk }

internal data class DisciplineStatus(val key: Key, val label: String) {
    enum class Key { Approved, Ongoing, Low, Failed, Final, Pending }
}

internal data class NeededProjection(
    val required: Double,
    val pending: Int,
    val target: Double,
)

// ───────── Derived helpers — mirror iOS Discipline extensions ─────────

internal val Discipline.allGrades: List<GradeEntry>
    get() = sections.flatMap { it.grades }

internal val Discipline.partialAverage: Double?
    get() {
        storedPartialAverage?.let { return it }
        val scores = allGrades.mapNotNull { it.score }
        if (scores.isEmpty()) return null
        return scores.sum() / scores.size
    }

internal val Discipline.completedCount: Int
    get() = allGrades.count { it.score != null }

internal val Discipline.totalEvaluations: Int
    get() = allGrades.size

// Required average on the remaining evaluations to close at `target`. Assumes
// equal weighting — matches `neededNext()` in the prototype.
internal fun Discipline.needed(target: Double = 7.0): NeededProjection? {
    val grades = allGrades
    val done = grades.mapNotNull { it.score }
    val pending = grades.filter { it.score == null }
    if (done.isEmpty() || pending.isEmpty()) return null
    val sumDone = done.sum()
    val n = grades.size.toDouble()
    val required = (target * n - sumDone) / pending.size.toDouble()
    return NeededProjection(required = required, pending = pending.size, target = target)
}

internal val Discipline.absenceRisk: AbsenceRisk
    get() {
        if (allowedAbsences <= 0) return AbsenceRisk.Ok
        val ratio = absences.toDouble() / allowedAbsences.toDouble()
        return when {
            ratio >= 0.75 -> AbsenceRisk.Risk
            ratio >= 0.50 -> AbsenceRisk.Warn
            else -> AbsenceRisk.Ok
        }
    }

internal val Discipline.status: DisciplineStatus
    get() {
        // Trust upstream's `approved` flag whenever it's set — the partial
        // mean alone can't tell us whether a student passed via the final
        // exam (typical 5–7 mean range), so falling back to a `>= 7` cutoff
        // misclassifies anyone who took finals.
        approved?.let {
            return if (it) {
                DisciplineStatus(DisciplineStatus.Key.Approved, "aprovado")
            } else {
                DisciplineStatus(DisciplineStatus.Key.Failed, "reprovado")
            }
        }
        val final = finalGrade
        if (final != null) {
            return when {
                final >= 7 -> DisciplineStatus(DisciplineStatus.Key.Approved, "aprovado")
                final >= 5 -> DisciplineStatus(DisciplineStatus.Key.Final, "prova final")
                else -> DisciplineStatus(DisciplineStatus.Key.Failed, "reprovado")
            }
        }
        val avg = partialAverage ?: return DisciplineStatus(DisciplineStatus.Key.Pending, "sem notas")
        return if (avg < 5.5) {
            DisciplineStatus(DisciplineStatus.Key.Low, "nota baixa")
        } else {
            DisciplineStatus(DisciplineStatus.Key.Ongoing, "em andamento")
        }
    }

internal val Discipline.hasMultipleGroups: Boolean
    get() = groups.size > 1

// "Te · Pr" style label used on the list card.
internal val Discipline.groupsShortLabel: String?
    get() {
        if (!hasMultipleGroups) return null
        return groups.joinToString(separator = " · ") { it.kind.take(2) }
    }

// Next unreleased evaluation with a scheduled date.
internal val Discipline.nextEvaluation: GradeEntry?
    get() = allGrades.firstOrNull { it.score == null && it.date != null }

// Filter sections to those visible for the currently-selected group pill.
// `null` (= "Tudo") returns all sections; a specific group returns only its
// sections plus any group-agnostic ones (KMP emits one merged section with
// `group = null` so it stays visible across every pill).
internal fun Discipline.sectionsForGroup(groupCode: String?): List<GradeSection> {
    if (groupCode == null) return sections
    return sections.filter { it.group == groupCode || it.group == null }
}

// Same idea for attachments — filtered by the segmented control selection.
internal fun Discipline.attachmentsForGroup(groupCode: String?): List<Attachment> {
    if (groupCode == null) return attachments
    return attachments.filter { it.group == groupCode || it.group == null }
}

// Trend across the last two completed grades (null when fewer than two).
internal val Discipline.trend: Double?
    get() {
        val scores = allGrades.mapNotNull { it.score }
        if (scores.size < 2) return null
        return scores[scores.size - 1] - scores[scores.size - 2]
    }

// ───────── Date helpers ─────────

internal object DisciplineDateFormatting {
    // KMP emits ISO "yyyy-MM-dd"; the UI reads "dd/MM/yyyy" — same conversion
    // iOS uses in `DisciplineDateFormatting.swift`.
    fun ddMmYyyy(iso: String?): String? {
        if (iso == null || iso.length < 10) return null
        return "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}"
    }

    // "20/03" — short form used for the attachment "added" caption (matches
    // iOS `DisciplineDateFormatting.ddMm`).
    fun ddMm(iso: String?): String? {
        if (iso == null || iso.length < 10) return null
        return "${iso.substring(8, 10)}/${iso.substring(5, 7)}"
    }
}

internal object DisciplineDate {
    // Parses dd/MM/yyyy and returns days from real-today (positive = future).
    // Mirrors iOS `DisciplineDate.daysUntil`. The iOS variant pins today to
    // 2026-04-18 to align with the prototype's mock dates; here we use the
    // real clock, since the data comes from the KMP flow.
    fun daysUntil(date: String?): Int? {
        if (date == null) return null
        val parts = date.split('/')
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        val target = java.time.LocalDate.of(year, month, day)
        val today = java.time.LocalDate.now()
        return java.time.temporal.ChronoUnit.DAYS.between(today, target).toInt()
    }
}
