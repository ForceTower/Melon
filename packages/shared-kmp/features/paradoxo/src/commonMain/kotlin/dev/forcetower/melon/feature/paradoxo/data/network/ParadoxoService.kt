package dev.forcetower.melon.feature.paradoxo.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineDetail
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineTeacher
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoError
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoExploreKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoIndexEntry
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoMyDiscipline
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoOverview
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseFact
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRanking
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRankingEntry
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRef
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoSemesterMean
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTaughtDiscipline
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTeacherDetail
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

// Read-only aggregates from `api/paradoxo/*`. Unknown pulse/ranking/ref
// kinds are dropped rather than failing the payload, so the backend can
// ship new card kinds ahead of the clients. Mirrors iOS
// `ParadoxoRepository+Live.swift`.
@Inject
internal class ParadoxoService(
    private val client: HttpClient,
) {
    suspend fun overview(): Outcome<ParadoxoOverview, ParadoxoError> =
        fetch<ParadoxoOverviewDTO, ParadoxoOverview>("api/paradoxo/overview") { it.toDomain() }

    suspend fun index(): Outcome<List<ParadoxoIndexEntry>, ParadoxoError> =
        fetch<ParadoxoIndexDTO, List<ParadoxoIndexEntry>>("api/paradoxo/index") { dto ->
            dto.entries.mapNotNull { it.toDomain() }
        }

    suspend fun discipline(id: String): Outcome<ParadoxoDisciplineDetail, ParadoxoError> =
        fetch<ParadoxoDisciplineDTO, ParadoxoDisciplineDetail>("api/paradoxo/disciplines/$id") {
            it.toDomain()
        }

    suspend fun teacher(id: String): Outcome<ParadoxoTeacherDetail, ParadoxoError> =
        fetch<ParadoxoTeacherDTO, ParadoxoTeacherDetail>("api/paradoxo/teachers/$id") {
            it.toDomain()
        }

    private suspend inline fun <reified D, T> fetch(
        path: String,
        map: (D) -> T,
    ): Outcome<T, ParadoxoError> {
        val payload = try {
            val response = client.get(path)
            if (!response.status.isSuccess()) return Outcome.Err(ParadoxoError.Connection)
            response.body<ApiEnvelope<D>>().takeIf { it.ok }?.data
        } catch (_: Exception) {
            null
        } ?: return Outcome.Err(ParadoxoError.Connection)
        return Outcome.Ok(map(payload))
    }
}

private fun ParadoxoRefDTO.toDomain(): ParadoxoRef? = when (kind) {
    "discipline" -> ParadoxoRef.Discipline(id)
    "teacher" -> ParadoxoRef.Teacher(id)
    else -> null
}

private fun pulseKindOf(raw: String): ParadoxoPulseKind? = when (raw) {
    "brutal" -> ParadoxoPulseKind.Brutal
    "kind" -> ParadoxoPulseKind.Kind
    "trend" -> ParadoxoPulseKind.Trend
    "gap" -> ParadoxoPulseKind.Gap
    "rising" -> ParadoxoPulseKind.Rising
    "surprise" -> ParadoxoPulseKind.Surprise
    "signature" -> ParadoxoPulseKind.Signature
    else -> null
}

private fun exploreKindOf(raw: String): ParadoxoExploreKind? = when (raw) {
    "brutal" -> ParadoxoExploreKind.Brutal
    "kind" -> ParadoxoExploreKind.Kind
    "rising" -> ParadoxoExploreKind.Rising
    "gap" -> ParadoxoExploreKind.Gap
    else -> null
}

@Serializable
internal data class ParadoxoRefDTO(
    val kind: String,
    val id: String,
)

@Serializable
internal data class ParadoxoSemesterMeanDTO(
    val semester: String,
    val mean: Double,
) {
    fun toDomain() = ParadoxoSemesterMean(semester = semester, mean = mean)
}

@Serializable
internal data class ParadoxoOverviewDTO(
    val pulse: List<PulseFact>,
    val myDisciplines: List<Mine>? = null,
    val rankings: List<Ranking>? = null,
    val studentCount: Int? = null,
    val meanCount: Int? = null,
) {
    @Serializable
    internal data class PulseFact(
        val id: String,
        val kind: String,
        val metric: Double,
        val title: String,
        val subtitle: String,
        val ref: ParadoxoRefDTO,
    )

    @Serializable
    internal data class Mine(
        val id: String,
        val code: String,
        val name: String,
        val mean: Double,
        val sampleCount: Int,
        val spark: List<Double>? = null,
        val myPercentile: Int? = null,
    )

    @Serializable
    internal data class Ranking(
        val kind: String,
        val entries: List<Entry>,
    ) {
        @Serializable
        internal data class Entry(
            val ref: ParadoxoRefDTO,
            val name: String,
            val code: String? = null,
            val mean: Double,
            val studentCount: Int,
            val delta: Double? = null,
        )
    }

    fun toDomain() = ParadoxoOverview(
        pulse = pulse.mapNotNull { fact ->
            val kind = pulseKindOf(fact.kind) ?: return@mapNotNull null
            val ref = fact.ref.toDomain() ?: return@mapNotNull null
            ParadoxoPulseFact(
                id = fact.id,
                kind = kind,
                metric = fact.metric,
                title = fact.title,
                subtitle = fact.subtitle,
                ref = ref,
            )
        },
        myDisciplines = myDisciplines.orEmpty().map {
            ParadoxoMyDiscipline(
                id = it.id,
                code = it.code,
                name = it.name,
                mean = it.mean,
                sampleCount = it.sampleCount,
                spark = it.spark.orEmpty(),
                myPercentile = it.myPercentile,
            )
        },
        rankings = rankings.orEmpty().mapNotNull { ranking ->
            val kind = exploreKindOf(ranking.kind) ?: return@mapNotNull null
            ParadoxoRanking(
                kind = kind,
                entries = ranking.entries.mapNotNull { entry ->
                    val ref = entry.ref.toDomain() ?: return@mapNotNull null
                    ParadoxoRankingEntry(
                        ref = ref,
                        name = entry.name,
                        code = entry.code,
                        mean = entry.mean,
                        studentCount = entry.studentCount,
                        delta = entry.delta,
                    )
                },
            )
        },
        studentCount = studentCount ?: 0,
        meanCount = meanCount ?: 0,
    )
}

@Serializable
internal data class ParadoxoIndexDTO(
    val entries: List<Entry>,
) {
    @Serializable
    internal data class Entry(
        val ref: ParadoxoRefDTO,
        val name: String,
        val code: String? = null,
        val mean: Double,
        val studentCount: Int,
    ) {
        fun toDomain(): ParadoxoIndexEntry? {
            val ref = ref.toDomain() ?: return null
            return ParadoxoIndexEntry(
                ref = ref,
                name = name,
                code = code,
                mean = mean,
                studentCount = studentCount,
            )
        }
    }
}

@Serializable
internal data class ParadoxoDisciplineDTO(
    val id: String,
    val code: String,
    val name: String,
    val department: String? = null,
    val mean: Double,
    val studentCount: Int,
    val approved: Int,
    val failed: Int,
    val quit: Int,
    val history: List<ParadoxoSemesterMeanDTO>,
    val distribution: List<Double>? = null,
    val myGrade: Double? = null,
    val teachers: List<Teacher>? = null,
) {
    @Serializable
    internal data class Teacher(
        val id: String,
        val name: String,
        val mean: Double,
        val sampleCount: Int,
        val lastSemester: String? = null,
        val history: List<ParadoxoSemesterMeanDTO>? = null,
    )

    fun toDomain() = ParadoxoDisciplineDetail(
        id = id,
        code = code,
        name = name,
        department = department,
        mean = mean,
        studentCount = studentCount,
        approved = approved,
        failed = failed,
        quit = quit,
        history = history.map { it.toDomain() },
        distribution = distribution.orEmpty(),
        myGrade = myGrade,
        teachers = teachers.orEmpty().map { teacher ->
            ParadoxoDisciplineTeacher(
                id = teacher.id,
                name = teacher.name,
                mean = teacher.mean,
                sampleCount = teacher.sampleCount,
                lastSemester = teacher.lastSemester,
                history = teacher.history.orEmpty().map { it.toDomain() },
            )
        },
    )
}

@Serializable
internal data class ParadoxoTeacherDTO(
    val id: String,
    val name: String,
    val mean: Double,
    val studentCount: Int,
    val approved: Int,
    val failed: Int,
    val quit: Int,
    val lastSemester: String? = null,
    val history: List<ParadoxoSemesterMeanDTO>? = null,
    val distribution: List<Double>? = null,
    val disciplines: List<Discipline>? = null,
) {
    @Serializable
    internal data class Discipline(
        val id: String,
        val code: String,
        val name: String,
        val mean: Double,
        val sampleCount: Int,
    )

    fun toDomain() = ParadoxoTeacherDetail(
        id = id,
        name = name,
        mean = mean,
        studentCount = studentCount,
        approved = approved,
        failed = failed,
        quit = quit,
        lastSemester = lastSemester,
        history = history.orEmpty().map { it.toDomain() },
        distribution = distribution.orEmpty(),
        disciplines = disciplines.orEmpty().map {
            ParadoxoTaughtDiscipline(
                id = it.id,
                code = it.code,
                name = it.name,
                mean = it.mean,
                sampleCount = it.sampleCount,
            )
        },
    )
}
