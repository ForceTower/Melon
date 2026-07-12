package dev.forcetower.melon.feature.paradoxo.domain.model

// Paradoxo — the grade-statistics explorer. All data is server-aggregated
// (student_offers rollups) and fetched live per screen; the values change
// once a semester, so a session-scoped fetch beats a mirror table. Mirrors
// iOS `Paradoxo.swift`.

sealed interface ParadoxoRef {
    data class Discipline(val id: String) : ParadoxoRef
    data class Teacher(val id: String) : ParadoxoRef
}

enum class ParadoxoPulseKind {
    Brutal,
    Kind,
    Trend,
    Gap,
    Rising,
    Surprise,
    Signature,
}

// One curated headline for the rotating hero. Selection and ordering are
// server-side; the client only renders what the overview returns.
data class ParadoxoPulseFact(
    val id: String,
    val kind: ParadoxoPulseKind,
    val metric: Double,
    val title: String,
    val subtitle: String,
    val ref: ParadoxoRef,
)

enum class ParadoxoExploreKind {
    Brutal,
    Kind,
    Rising,
    Gap,
}

data class ParadoxoRankingEntry(
    val ref: ParadoxoRef,
    val name: String,
    val code: String?,
    val mean: Double,
    val studentCount: Int,
    val delta: Double?,
)

data class ParadoxoRanking(
    val kind: ParadoxoExploreKind,
    val entries: List<ParadoxoRankingEntry>,
)

// A discipline the student is (or was) enrolled in, with its historical
// all-students mean. `myPercentile` is server-computed from the student's
// own final grade against the distribution.
data class ParadoxoMyDiscipline(
    val id: String,
    val code: String,
    val name: String,
    val mean: Double,
    val sampleCount: Int,
    val spark: List<Double>,
    val myPercentile: Int?,
)

data class ParadoxoOverview(
    val pulse: List<ParadoxoPulseFact>,
    val myDisciplines: List<ParadoxoMyDiscipline>,
    val rankings: List<ParadoxoRanking>,
    val studentCount: Int,
    val meanCount: Int,
) {
    fun ranking(kind: ParadoxoExploreKind): ParadoxoRanking? =
        rankings.firstOrNull { it.kind == kind }
}

// Flat search catalogue — every discipline and teacher with published
// aggregates. Filtering happens client-side against this list.
data class ParadoxoIndexEntry(
    val ref: ParadoxoRef,
    val name: String,
    val code: String?,
    val mean: Double,
    val studentCount: Int,
)

data class ParadoxoSemesterMean(
    val semester: String,
    val mean: Double,
)

data class ParadoxoDisciplineTeacher(
    val id: String,
    val name: String,
    val mean: Double,
    val sampleCount: Int,
    val lastSemester: String?,
    val history: List<ParadoxoSemesterMean>,
)

data class ParadoxoDisciplineDetail(
    val id: String,
    val code: String,
    val name: String,
    val department: String?,
    val mean: Double,
    val studentCount: Int,
    val approved: Int,
    val failed: Int,
    val quit: Int,
    val history: List<ParadoxoSemesterMean>,
    val distribution: List<Double>,
    val myGrade: Double?,
    val teachers: List<ParadoxoDisciplineTeacher>,
)

data class ParadoxoTaughtDiscipline(
    val id: String,
    val code: String,
    val name: String,
    val mean: Double,
    val sampleCount: Int,
)

data class ParadoxoTeacherDetail(
    val id: String,
    val name: String,
    val mean: Double,
    val studentCount: Int,
    val approved: Int,
    val failed: Int,
    val quit: Int,
    val lastSemester: String?,
    val history: List<ParadoxoSemesterMean>,
    val distribution: List<Double>,
    val disciplines: List<ParadoxoTaughtDiscipline>,
)

enum class ParadoxoError {
    Connection,
}
