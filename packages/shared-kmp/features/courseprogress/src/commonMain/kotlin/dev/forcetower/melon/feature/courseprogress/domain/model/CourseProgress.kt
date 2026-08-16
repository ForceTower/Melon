package dev.forcetower.melon.feature.courseprogress.domain.model

import kotlin.time.Instant

// "Progresso do curso" — how much is left to graduate, and the curriculum grid
// the fluxograma lays out. Mirrors iOS `CourseProgress.swift`; the graph walks
// (trail / upstream / downstream) live here so both platforms answer the same
// question the same way.

// Situation of one curriculum slot for the student. `wire` is the vocabulary
// of `api/curriculum`.
//
// `Available` / `Blocked` only ever arrive when the payload says prerequisites
// are known; otherwise untouched slots come back as `NotTaken`.
enum class CurriculumEntryStatus(val wire: String) {
    Completed("completed"),
    InProgress("in_progress"),
    Available("available"),
    Withdrawn("withdrawn"),
    Failed("failed"),
    Blocked("blocked"),
    NotTaken("not_taken"),
    ;

    companion object {
        // Anything this build can't name is, at most, not completed.
        fun fromWire(raw: String?): CurriculumEntryStatus =
            entries.firstOrNull { it.wire == raw } ?: NotTaken
    }
}

// The hour-type bucket a requirement belongs to. The university's own label
// travels alongside it and is what the UI shows.
enum class CurriculumRequirementKind(val wire: String) {
    Required("required"),
    Elective("elective"),
    Complementary("complementary"),
    Internship("internship"),
    Capstone("capstone"),
    Extension("extension"),
    Other("other"),
    ;

    companion object {
        // A kind this build doesn't know is a newer server bucket — generic,
        // not dropped.
        fun fromWire(raw: String?): CurriculumRequirementKind =
            entries.firstOrNull { it.wire == raw } ?: Other
    }
}

// The curriculum version ("matriz curricular") the student is bound to.
data class CurriculumVersion(
    val id: String,
    // SAGRES's version identifier, a semester code: "20232".
    val code: String,
    // Verbatim upstream label, e.g. "BACHAREL E FORMAÇÃO DE PSICÓLOGO".
    val label: String,
    // yyyy-MM-dd — how current the transcribed source document is.
    val asOf: String,
    val minPeriods: Int?,
    val maxPeriods: Int?,
    // The source document is old enough that required hours may have moved.
    val stale: Boolean,
) {
    // The version as students read it — "2024.1" for the semester code
    // "20241". Anything not shaped like a semester code shows verbatim.
    val codeLabel: String
        get() = if (code.length == 5 && code.all { it.isDigit() }) {
            "${code.take(4)}.${code.last()}"
        } else {
            code
        }
}

// The headline numbers. `requiredHours` is null only when no curriculum is
// held for the course — the screen then shows hours with no denominator.
data class CurriculumSummary(
    val completedHours: Int,
    val requiredHours: Int?,
    // Capped per requirement so surplus electives can't exceed 100.
    val percent: Double?,
    // Of `requiredHours`, how much sits in buckets whose completion is never
    // observable (paper certificates).
    val excludedHours: Int,
    // Completed hours whose requirement is unknown — in the total, absent
    // from the bars.
    val unclassifiedHours: Int,
    val disciplinesCompleted: Int,
    val disciplinesTotal: Int,
) {
    val remainingHours: Int?
        get() = requiredHours?.let { maxOf(0, it - completedHours) }

    companion object {
        val EMPTY = CurriculumSummary(
            completedHours = 0,
            requiredHours = null,
            percent = null,
            excludedHours = 0,
            unclassifiedHours = 0,
            disciplinesCompleted = 0,
            disciplinesTotal = 0,
        )
    }
}

// One hour-type bucket ("natureza") with the student's progress against it.
data class CurriculumRequirementProgress(
    // Stable slug, e.g. "nucleo-comum".
    val code: String,
    val kind: CurriculumRequirementKind,
    // The university's own pt-BR wording.
    val label: String,
    // Abbreviated for narrow rows; equals `label` when none was authored.
    val shortLabel: String,
    // First período this bucket appears in — explains a legitimate 0%.
    val startsAtPeriod: Int?,
    val hoursRequired: Int,
    val hoursCompleted: Int,
    // False when completion lives outside anything observable (atividades
    // complementares): render as "not counted yet", not as zero progress.
    val derivable: Boolean,
    val percent: Double?,
) {
    val hoursRemaining: Int
        get() = maxOf(0, hoursRequired - hoursCompleted)
}

// One slot in the curriculum grid.
data class CurriculumEntry(
    val code: String,
    // Arrives abbreviated and upper-case from the university system.
    val name: String,
    val hours: Int,
    val credits: Int?,
    // Null for the elective pool — not scheduled in any período.
    val period: Int?,
    // Entries sharing a group must be taken in the same período.
    val coreqGroup: Int?,
    val requirementCode: String?,
    val status: CurriculumEntryStatus,
    // Must be completed first; these gate `Available` / `Blocked`.
    val prerequisites: List<String>,
    // Taken alongside; never gates availability.
    val corequisites: List<String>,
)

// The entries scheduled for one período; `period == null` is the elective pool.
data class CurriculumPeriod(
    val period: Int?,
    val entries: List<CurriculumEntry>,
) {
    // The pool sorts after every numbered período.
    val sortKey: Int get() = period ?: Int.MAX_VALUE

    val hours: Int get() = entries.sumOf { it.hours }

    fun count(status: CurriculumEntryStatus): Int = entries.count { it.status == status }

    val completedCount: Int get() = count(CurriculumEntryStatus.Completed)
}

// The one payload behind the progress screen and the fluxograma, as mirrored
// on disk.
data class CourseProgress(
    // Null when no curriculum is held for the student's course.
    val curriculum: CurriculumVersion?,
    val summary: CurriculumSummary,
    val requirements: List<CurriculumRequirementProgress>,
    // Sorted by período, elective pool last.
    val periods: List<CurriculumPeriod>,
    // Where the student is now — the highest período they have work in.
    val currentPeriod: Int?,
    // False when too few entries carry prerequisites to claim availability.
    val prerequisitesKnown: Boolean,
    val syncedAt: Instant,
) {
    val hasCurriculum: Boolean get() = curriculum != null

    // The total is known but the per-requirement split didn't come back.
    val hasBreakdown: Boolean get() = requirements.isNotEmpty()

    // Numbered períodos only — what the rail, map and grid lay out.
    val scheduledPeriods: List<CurriculumPeriod> get() = periods.filter { it.period != null }

    val electivePool: CurriculumPeriod?
        get() = periods.firstOrNull { it.period == null && it.entries.isNotEmpty() }

    val entries: List<CurriculumEntry> get() = periods.flatMap { it.entries }

    val entriesByCode: Map<String, CurriculumEntry> by lazy {
        entries.associateBy { it.code }
    }

    // Every entry that lists a given code as a prerequisite, keyed by that code.
    private val unlocksByCode: Map<String, List<String>> by lazy {
        val result = mutableMapOf<String, MutableList<String>>()
        entries.forEach { entry ->
            entry.prerequisites.forEach { required ->
                result.getOrPut(required) { mutableListOf() }.add(entry.code)
            }
        }
        result
    }

    fun entry(code: String): CurriculumEntry? = entriesByCode[code]

    fun period(number: Int): CurriculumPeriod? = periods.firstOrNull { it.period == number }

    // The período the fluxograma opens on: the student's own, else the first.
    val landingPeriod: Int
        get() = currentPeriod ?: scheduledPeriods.firstOrNull()?.period ?: 1

    // Entries that list `code` as a prerequisite — what completing it unlocks.
    fun unlocks(code: String): List<CurriculumEntry> =
        unlocksByCode[code].orEmpty().mapNotNull(::entry)

    // Disciplines to be taken alongside `code` — its own list plus every entry
    // that names it, since the relation is symmetric in practice but upstream
    // only ever writes one side.
    fun corequisites(code: String): List<CurriculumEntry> {
        val seen = mutableSetOf(code)
        val result = mutableListOf<CurriculumEntry>()
        entry(code)?.corequisites?.mapNotNull(::entry)?.forEach {
            if (seen.add(it.code)) result.add(it)
        }
        entries.forEach {
            if (it.corequisites.contains(code) && seen.add(it.code)) result.add(it)
        }
        return result
    }

    // The prerequisite chain through `code`, both ways: everything it depends
    // on (transitively) and everything it eventually unlocks. Always contains
    // `code` itself.
    fun trail(code: String): Set<String> = upstream(code) + downstream(code) + code

    // Everything `code` transitively depends on — what must be done first.
    fun upstream(code: String): Set<String> =
        reach(code) { entriesByCode[it]?.prerequisites.orEmpty() }

    // Everything `code` transitively unlocks — what it eventually opens.
    fun downstream(code: String): Set<String> = reach(code) { unlocksByCode[it].orEmpty() }

    // The chain entries in reading order: by período, then code.
    fun chainEntries(codes: Set<String>): List<CurriculumEntry> =
        codes.mapNotNull(::entry).sortedWith(
            compareBy({ it.period ?: Int.MAX_VALUE }, { it.code }),
        )

    // The requirement label for an entry, when the bucket is known.
    fun requirementLabel(entry: CurriculumEntry): String? {
        val code = entry.requirementCode ?: return null
        return requirements.firstOrNull { it.code == code }?.shortLabel
    }

    // Transitive closure from `code` along `next`, excluding `code` itself.
    private fun reach(code: String, next: (String) -> List<String>): Set<String> {
        val visited = mutableSetOf(code)
        val frontier = ArrayDeque(listOf(code))
        while (frontier.isNotEmpty()) {
            next(frontier.removeLast()).forEach { candidate ->
                if (visited.add(candidate)) frontier.addLast(candidate)
            }
        }
        return visited - code
    }
}
