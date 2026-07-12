package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSelection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState

// Session model for the matrícula flow (dc `MatriculaScreen`, iOS
// `EnrollmentSession`). The catalogue is live/uncached; picks are in-memory
// only and become the wholesale submit payload — there is no draft endpoint.

internal enum class EnrollmentPhase { Idle, Loading, Loaded, Failed }

internal enum class EnrollmentFilter { All, Mandatory, Optional }

// One selection per discipline. `allowsOther` mirrors "aceitar outra turma";
// `waitlist` auto-sets when a full section is picked under a queue-enabled
// window.
internal data class EnrollmentPick(
    val disciplineId: Long,
    val sectionId: Long,
    val allowsOther: Boolean,
    val waitlist: Boolean,
)

internal data class ResolvedPick(
    val discipline: EnrollmentDiscipline,
    val section: EnrollmentSection,
    val allowsOther: Boolean,
    val waitlist: Boolean,
)

internal data class EnrollmentConflictUi(
    val first: ResolvedPick,
    val second: ResolvedPick,
    val day: Int,
)

// A candidate section clashing with an already-picked one (gates cards in
// the section picker).
internal data class EnrollmentClash(
    val discipline: EnrollmentDiscipline,
    val section: EnrollmentSection,
    val day: Int,
)

internal sealed interface EnrollmentBlocker {
    data object Empty : EnrollmentBlocker
    data class Conflicts(val count: Int) : EnrollmentBlocker
    data class UnderMinimum(val missing: Int) : EnrollmentBlocker
    data class OverMaximum(val excess: Int) : EnrollmentBlocker
}

internal data class EnrollmentUiState(
    val phase: EnrollmentPhase = EnrollmentPhase.Idle,
    val error: EnrollmentError? = null,
    // Window resolved and student is eligible. False after a Loaded phase
    // means "no enrollment right now" (the moon-zzz empty state).
    val available: Boolean = false,
    val window: EnrollmentWindow? = null,
    // Window resolved but the offers tree failed — status renders with an
    // inline retry instead of an empty catalogue.
    val offersFailed: Boolean = false,
    val disciplines: List<EnrollmentDiscipline> = emptyList(),
    val picks: List<EnrollmentPick> = emptyList(),
    val studentName: String? = null,
    val courseName: String? = null,
    val semesterOrdinal: Int? = null,
    val query: String = "",
    val filter: EnrollmentFilter = EnrollmentFilter.All,
    val submitting: Boolean = false,
    val submitError: EnrollmentError? = null,
    // "Reabrir matrícula": re-enables editing + resubmission after the
    // window closed for this student. Session-local — the backend reopens
    // the step automatically on the next submit, which replaces the saved
    // proposal wholesale.
    val reopened: Boolean = false,
    // Pinned at flow entry so the hero countdown doesn't tick live.
    val referenceNowMillis: Long = 0L,
) : UiState {

    // Picks joined to catalogue rows in selection order; picks whose rows
    // vanished from a refreshed catalogue silently drop out.
    val resolvedPicks: List<ResolvedPick>
        get() = picks.mapNotNull { pick ->
            val discipline = disciplines.find { it.id == pick.disciplineId } ?: return@mapNotNull null
            val section = discipline.sections.find { it.id == pick.sectionId } ?: return@mapNotNull null
            ResolvedPick(discipline, section, pick.allowsOther, pick.waitlist)
        }

    val totalHours: Int get() = resolvedPicks.sumOf { it.discipline.workload }

    val waitlistedCount: Int get() = resolvedPicks.count { it.waitlist }

    val allowsOtherCount: Int get() = resolvedPicks.count { it.allowsOther }

    // Every clashing pair among the current picks.
    val conflicts: List<EnrollmentConflictUi>
        get() {
            val list = resolvedPicks
            val out = mutableListOf<EnrollmentConflictUi>()
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val day = conflictDay(list[i].section, list[j].section) ?: continue
                    out += EnrollmentConflictUi(list[i], list[j], day)
                }
            }
            return out
        }

    // Ordered by severity; drives `canSubmit`. Prerequisites are surfaced as
    // a warning banner only, never a blocker (the backend pre-filters offers).
    val blockers: List<EnrollmentBlocker>
        get() {
            if (picks.isEmpty()) return listOf(EnrollmentBlocker.Empty)
            val window = window ?: return emptyList()
            val out = mutableListOf<EnrollmentBlocker>()
            val clashes = conflicts.size
            if (clashes > 0) out += EnrollmentBlocker.Conflicts(clashes)
            val total = totalHours
            if (total < window.minHours) out += EnrollmentBlocker.UnderMinimum(window.minHours - total)
            if (total > window.maxHours) out += EnrollmentBlocker.OverMaximum(total - window.maxHours)
            return out
        }

    // Closed = the student already finalized (or the window ended). Every
    // screen goes read-only until "Reabrir matrícula" flips `reopened`.
    val isReadonly: Boolean
        get() = window?.state == EnrollmentWindowState.Closed && !reopened

    // Picks may only mutate while the window is open or explicitly reopened.
    val canEdit: Boolean
        get() = window?.state == EnrollmentWindowState.Open ||
            (window?.state == EnrollmentWindowState.Closed && reopened)

    val canSubmit: Boolean
        get() = canEdit && blockers.isEmpty() && !submitting

    val selections: List<EnrollmentSelection>
        get() = resolvedPicks.map {
            EnrollmentSelection(sectionId = it.section.id, allowsOther = it.allowsOther, waitlist = it.waitlist)
        }

    fun disciplineById(id: Long): EnrollmentDiscipline? = disciplines.find { it.id == id }

    fun pickFor(disciplineId: Long): EnrollmentPick? = picks.find { it.disciplineId == disciplineId }

    fun selectedSectionOf(disciplineId: Long): EnrollmentSection? {
        val pick = pickFor(disciplineId) ?: return null
        return disciplineById(disciplineId)?.sections?.find { it.id == pick.sectionId }
    }

    // First pick of a *different* discipline clashing with a candidate section.
    fun clashFor(discipline: EnrollmentDiscipline, section: EnrollmentSection): EnrollmentClash? {
        for (pick in resolvedPicks) {
            if (pick.discipline.id == discipline.id) continue
            val day = conflictDay(section, pick.section) ?: continue
            return EnrollmentClash(pick.discipline, pick.section, day)
        }
        return null
    }
}

internal sealed interface EnrollmentIntent : UiIntent {
    // Status screen mount: re-anchors the countdown clock, full-loads on
    // first entry, silently refreshes the catalogue afterwards.
    data object Enter : EnrollmentIntent
    data object Retry : EnrollmentIntent
    data class QueryChanged(val query: String) : EnrollmentIntent
    data class FilterChanged(val filter: EnrollmentFilter) : EnrollmentIntent
    data class SectionTapped(val disciplineId: Long, val sectionId: Long) : EnrollmentIntent
    data class RemovePick(val disciplineId: Long) : EnrollmentIntent
    data class AllowsOtherChanged(val disciplineId: Long, val value: Boolean) : EnrollmentIntent
    // "Reabrir matrícula" on the closed status hub.
    data object Reopen : EnrollmentIntent
    data object Submit : EnrollmentIntent
    data object DismissSubmitError : EnrollmentIntent
}

internal sealed interface EnrollmentEffect : UiEffect {
    data object Submitted : EnrollmentEffect
}

// ───────── pure section/slot helpers (mirror iOS `Enrollment.swift`) ─────────

// "13:30" or "13:30:00" → minutes since midnight; malformed slots collapse
// to 0 and are dropped by `hasSchedule` upstream when empty.
internal fun slotMinutes(time: String): Int {
    val parts = time.split(':')
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

internal val EnrollmentSection.allSlots: List<EnrollmentSlot>
    get() = meetings.flatMap { it.slots }

// "A definir" sections have no slots — they never conflict.
internal val EnrollmentSection.hasSchedule: Boolean get() = allSlots.isNotEmpty()

internal fun slotsOverlap(a: EnrollmentSlot, b: EnrollmentSlot): Boolean =
    a.day == b.day &&
        slotMinutes(a.start) < slotMinutes(b.end) &&
        slotMinutes(b.start) < slotMinutes(a.end)

// First weekday where any pair of slots overlaps, else null.
internal fun conflictDay(a: EnrollmentSection, b: EnrollmentSection): Int? {
    for (slotA in a.allSlots) {
        for (slotB in b.allSlots) {
            if (slotsOverlap(slotA, slotB)) return slotA.day
        }
    }
    return null
}

internal data class EnrollmentSeats(val filled: Int, val total: Int) {
    val fraction: Float get() = if (total > 0) filled.toFloat() / total else 0f
    val isFull: Boolean get() = filled >= total
    val isTight: Boolean get() = !isFull && fraction >= 0.85f
}

internal val EnrollmentSection.seats: EnrollmentSeats
    get() = EnrollmentSeats(filled = proposalsCount, total = vacancies)

internal val EnrollmentDiscipline.hasUnmetPrerequisite: Boolean
    get() = prerequisites.any { !it.met }

internal data class EnrollmentPeriodGroup(
    val period: Int,
    val disciplines: List<EnrollmentDiscipline>,
)

// Ascending curriculum periods with 0 (optativas/eletivas) last.
internal fun groupedByPeriod(list: List<EnrollmentDiscipline>): List<EnrollmentPeriodGroup> =
    list.groupBy { it.gradePeriod }
        .map { (period, items) -> EnrollmentPeriodGroup(period, items) }
        .sortedWith(compareBy({ it.period == 0 }, { it.period }))

// Single source of pick defaults: `allowsOther` seeds from the section flag,
// `waitlist` auto-sets when picking a full section under a queue window.
internal fun makePick(
    window: EnrollmentWindow?,
    discipline: EnrollmentDiscipline,
    section: EnrollmentSection,
): EnrollmentPick = EnrollmentPick(
    disciplineId = discipline.id,
    sectionId = section.id,
    allowsOther = section.allowsOtherDefault,
    waitlist = section.seats.isFull && window?.useQueue == true,
)

// Resume mechanism: rebuild picks from sections flagged `selected` by the
// backend. The wire doesn't carry saved toggle values, so they reset to
// defaults.
internal fun preseedPicks(
    window: EnrollmentWindow?,
    disciplines: List<EnrollmentDiscipline>,
): List<EnrollmentPick> = disciplines.mapNotNull { discipline ->
    discipline.sections.firstOrNull { it.selected }?.let { makePick(window, discipline, it) }
}
